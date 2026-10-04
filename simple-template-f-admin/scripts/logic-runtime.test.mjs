/**
 * 图执行器的回归测试（headless，不需要浏览器）。
 *
 * 原理：src/logic 下的五个文件不依赖 Vue，用项目自带的 tsc 编译成 CommonJS 到临时目录，
 * 再 require 进来用真图跑断言。改执行器后跑 `npm run test:logic` 就知道语义有没有被改坏。
 *
 * 运行：在 simple-template-f-admin 下执行 npm run test:logic
 */
import { execFileSync } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createRequire } from "node:module";

const projectRoot = new URL("..", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1");
const sources = ["types.ts", "valueRef.ts", "presetExpand.ts", "atomExecutors.ts", "graphRuntime.ts"].map(
    (file) => join("src", "logic", file),
);
const outDir = mkdtempSync(join(tmpdir(), "logic-runtime-"));

try {
    // 直接用 node 跑 tsc 的入口（避免 shell:true 带来的转义问题与弃用告警）
    execFileSync(
        process.execPath,
        [
            join(projectRoot, "node_modules", "typescript", "bin", "tsc"),
            ...sources,
            "--outDir", outDir,
            "--module", "commonjs",
            "--target", "es2022",
            "--skipLibCheck",
        ],
        { cwd: projectRoot, stdio: ["ignore", "ignore", "inherit"] },
    );

    const require = createRequire(import.meta.url);
    const { createGraphRuntime } = require(join(outDir, "graphRuntime.js"));

    let failures = 0;
    const check = (label, actual, expected) => {
        const ok = JSON.stringify(actual) === JSON.stringify(expected);
        if (!ok) {
            failures++;
        }
        console.log(`${ok ? "OK  " : "!!! "} ${label}`);
        if (!ok) {
            console.log(`      期望 ${JSON.stringify(expected)}`);
            console.log(`      实际 ${JSON.stringify(actual)}`);
        }
    };

    const makeHost = (overrides = {}) => {
        const calls = { api: [], toasts: [], errors: [] };
        const host = {
            callApi: async (method, path, params) => {
                calls.api.push({ method, path, params });
                return { records: [{ id: 1 }, { id: 2 }, { id: 3 }], total: 3, page: 1, size: 10 };
            },
            navigate: () => {},
            toast: (message, type) => calls.toasts.push({ message, type }),
            scrollTo: () => {},
            setVisible: () => {},
            openModal: () => {},
            closeModal: () => {},
            componentValue: () => undefined,
            runtimeValues: () => ({ page: 3, routeParam: { id: 42 }, query: { k: "x" } }),
            isLoggedIn: () => false,
            currentMemberId: () => null,
            logout: () => {},
            onError: (message) => calls.errors.push(message),
        };
        return { host: { ...host, ...overrides }, calls };
    };
    const makeRuntime = (flows, presets, host) => {
        const state = { outputs: {}, visible: {}, session: {}, loading: {}, trace: [] };
        return { runtime: createGraphRuntime({ state, host, flows, presets }), state };
    };

    // 1 控制流推进 + 分支短路
    {
        const { host, calls } = makeHost();
        const { runtime, state } = makeRuntime(
            {
                init: {
                    nodes: [
                        { id: "n1", typeCode: "on.pageEnter" },
                        { id: "n2", typeCode: "cond.isLogin" },
                        { id: "n3", typeCode: "branch", config: { cond: "$ref.n2.boolean" } },
                        { id: "n4", typeCode: "act.toast", config: { message: "已登录" } },
                        { id: "n5", typeCode: "act.toast", config: { message: "未登录" } },
                    ],
                    edges: [
                        { from: { node: "n1", port: "out" }, to: { node: "n2", port: "signal" } },
                        { from: { node: "n2", port: "out" }, to: { node: "n3", port: "signal" } },
                        { from: { node: "n3", port: "true" }, to: { node: "n4", port: "signal" } },
                        { from: { node: "n3", port: "false" }, to: { node: "n5", port: "signal" } },
                    ],
                },
            },
            {},
            host,
        );
        await runtime.runFlow("init");
        console.log("\n— 1 控制流推进 + 分支短路 —");
        check("未登录时只走 false 支", calls.toasts.map((t) => t.message), ["未登录"]);
        check("分支结果落在 outputs", state.outputs.n3.boolean, false);
    }

    // 2 数据流惰性拉取 + 一次运行内只算一次
    {
        const { host, calls } = makeHost();
        const { runtime, state } = makeRuntime(
            {
                init: {
                    nodes: [
                        { id: "n1", typeCode: "on.pageEnter" },
                        { id: "n2", typeCode: "data.postList", config: { boardId: 5 } },
                        { id: "n3", typeCode: "op.take", config: { list: "$ref.n2.listPost", n: 1 } },
                        { id: "n4", typeCode: "op.take", config: { list: "$ref.n2.listPost", n: 2 } },
                    ],
                    edges: [
                        { from: { node: "n1", port: "out" }, to: { node: "n3", port: "signal" } },
                        { from: { node: "n3", port: "out" }, to: { node: "n4", port: "signal" } },
                    ],
                },
            },
            {},
            host,
        );
        await runtime.runFlow("init");
        console.log("\n— 2 数据流惰性拉取 + 缓存 —");
        check("上游数据节点被拉起来了", state.outputs.n2.listPost.length, 3);
        check("分页字段映射出来了", state.outputs.n2.pageInfo.total, 3);
        check("只请求一次（缓存生效）", calls.api.length, 1);
        check("两次 take 各自生效", [state.outputs.n3.any.length, state.outputs.n4.any.length], [1, 2]);
    }

    // 3 预设运行期递归展开（$param 与 exports）
    {
        const { host, calls } = makeHost();
        const presets = {
            "preset.test@1": {
                nodes: [
                    { id: "p1", typeCode: "data.runtime", config: { key: "routeParam.id" } },
                    { id: "p2", typeCode: "op.take", config: { list: ["a", "b", "c"], n: 1 } },
                    { id: "p3", typeCode: "act.toast", config: { message: "$param.msg" } },
                ],
                edges: [
                    { from: { node: "p1", port: "out" }, to: { node: "p2", port: "signal" } },
                    { from: { node: "p2", port: "out" }, to: { node: "p3", port: "signal" } },
                ],
                exports: { any: "p2.any", rid: "p1.any" },
            },
        };
        const { runtime, state } = makeRuntime(
            {
                init: {
                    nodes: [
                        { id: "n1", typeCode: "on.pageEnter" },
                        { id: "n2", typeCode: "preset", presetCode: "preset.test", presetVersion: 1, config: { msg: "来自父图" } },
                        { id: "n3", typeCode: "act.toast", config: { message: "预设后" } },
                    ],
                    edges: [
                        { from: { node: "n1", port: "out" }, to: { node: "n2", port: "signal" } },
                        { from: { node: "n2", port: "out" }, to: { node: "n3", port: "signal" } },
                    ],
                },
            },
            presets,
            host,
        );
        await runtime.runFlow("init");
        console.log("\n— 3 预设递归展开 —");
        check("预设出参挂到预设节点上", [state.outputs.n2.rid, state.outputs.n2.any], [42, ["a"]]);
        check("预设内部节点 outputs 带前缀", state.outputs["n2/p1"].any, 42);
        check("$param 从父图传进来", calls.toasts.map((t) => t.message), ["来自父图", "预设后"]);
    }

    // 4 出错：记轨迹 + 通知宿主 + 不往下走
    {
        const { host, calls } = makeHost();
        const { runtime, state } = makeRuntime(
            {
                init: {
                    nodes: [
                        { id: "n1", typeCode: "on.pageEnter" },
                        { id: "n2", typeCode: "act.navigate", config: {} },
                        { id: "n3", typeCode: "act.toast", config: { message: "不该跑到" } },
                    ],
                    edges: [
                        { from: { node: "n1", port: "out" }, to: { node: "n2", port: "signal" } },
                        { from: { node: "n2", port: "out" }, to: { node: "n3", port: "signal" } },
                    ],
                },
            },
            {},
            host,
        );
        await runtime.runFlow("init");
        console.log("\n— 4 出错处理 —");
        check("轨迹里有 error", state.trace.some((t) => t.nodeId === "n2" && t.status === "error"), true);
        check("宿主被通知", calls.errors.length, 1);
        check("报错后不再往下推进", calls.toasts.length, 0);
    }

    // 5 未知原子 / 未知预设：跳过并告警，不白屏
    {
        const { host, calls } = makeHost();
        const { runtime, state } = makeRuntime(
            {
                init: {
                    nodes: [
                        { id: "n1", typeCode: "on.pageEnter" },
                        { id: "n2", typeCode: "on.notExist" },
                        { id: "n3", typeCode: "preset", presetCode: "preset.nope", presetVersion: 9 },
                        { id: "n4", typeCode: "act.toast", config: { message: "still alive" } },
                    ],
                    edges: [
                        { from: { node: "n1", port: "out" }, to: { node: "n2", port: "signal" } },
                        { from: { node: "n2", port: "out" }, to: { node: "n3", port: "signal" } },
                        { from: { node: "n3", port: "out" }, to: { node: "n4", port: "signal" } },
                    ],
                },
            },
            {},
            host,
        );
        await runtime.runFlow("init");
        console.log("\n— 5 未知原子/预设 —");
        check("两次跳过都记了轨迹", state.trace.filter((t) => t.status === "skipped").length, 2);
        check("告警原因分别是原子/预设", [calls.errors[0]?.includes("未知原子"), calls.errors[1]?.includes("未知预设")], [true, true]);
        check("后续节点照常跑（不白屏）", calls.toasts.map((t) => t.message), ["still alive"]);
    }

    // 6 组件交互按 nodeId 找到对应的根
    {
        const { host, calls } = makeHost();
        const { runtime } = makeRuntime(
            {
                click: {
                    nodes: [
                        { id: "c1", typeCode: "on.click", config: { nodeId: 30 } },
                        { id: "c2", typeCode: "act.toast", config: { message: "点了 30 号组件" } },
                    ],
                    edges: [{ from: { node: "c1", port: "out" }, to: { node: "c2", port: "signal" } }],
                },
            },
            {},
            host,
        );
        await runtime.triggerEvent("click", 30);
        await runtime.triggerEvent("click", 99);
        console.log("\n— 6 交互触发 —");
        check("只响应匹配的组件", calls.toasts.map((t) => t.message), ["点了 30 号组件"]);
    }

    // 7 渲染绑定求值与显隐
    {
        const { host } = makeHost();
        const { runtime, state } = makeRuntime({}, {}, host);
        state.outputs = { n5: { listPost: [{ id: 1 }] } };
        state.visible = { 30: false };
        console.log("\n— 7 绑定求值与显隐 —");
        check("$ref 能解析", runtime.evaluate("$ref.n5.listPost"), [{ id: 1 }]);
        check("$runtime 能解析", runtime.evaluate("$runtime.page"), 3);
        check("解析不出来给 undefined", runtime.evaluate("$ref.nope.x"), undefined);
        check("字面量原样返回", runtime.evaluate("普通文案"), "普通文案");
        check("act.setVisible 改的是渲染器读的显隐", runtime.isVisible(30), false);
        check("没写过显隐默认可见", runtime.isVisible(31), true);
    }

    console.log(failures === 0 ? "\n全部通过" : `\n有 ${failures} 项不符合预期`);
    process.exitCode = failures === 0 ? 0 : 1;
} finally {
    rmSync(outDir, { recursive: true, force: true });
}
