#!/usr/bin/env node
/**
 * pagekit —— 让 AI agent 生成/校验/落库「符合本项目规则的页面」的零依赖 CLI。
 *
 * 只覆盖呈现层：ui_page（页面）+ ui_node（组件树）。不含逻辑图（ui_flow）。
 * 依赖：Node 18+ 内置能力（fetch / fs / path），不装任何 npm 包。
 *
 * 职责边界：
 *   - 值类型校验（颜色/length/枚举/url 等）由后端在保存时完成，本脚本刻意不复制这些规则；
 *   - 本地 validate 只查「由组件库目录数据决定的结构规则」+ 树的引用完整性，因此不会与后端漂移。
 *
 * 用法：node pagekit.mjs <命令> [参数] [--base url] [--json]
 * 命令：login / catalog / pages / get / validate / save / create-page / delete-page
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const KIT_ROOT = dirname(fileURLToPath(import.meta.url));
const DEFAULT_CATALOG = join(KIT_ROOT, "references", "component-catalog.json");
const DEFAULT_TOKEN_FILE = join(process.cwd(), ".qwen", "tmp", "pagekit-token.json");
const DEFAULT_BASE = "http://localhost:9331";

/**
 * 用户端已经接了渲染内核的业务组件（两端 blocks/builtinWidgets.ts 里的注册表）。
 * 组件库目录里查不出「前端注册了没有」，所以这份名单要跟着注册表手工维护：
 * 组件库新增内置业务组件后，在这里登记之前，validate 会提醒它只会显示占位。
 */
const RENDERED_BUILTINS = new Set([
    "builtin:banner",
    "builtin:notice",
    "builtin:hot_post",
    "builtin:board_nav",
]);

/* ------------------------------- 参数解析 ------------------------------- */

function parseArgs(argv) {
    const flags = {};
    const positional = [];
    const takesValue = new Set(["base", "token", "token-file", "out", "user", "password", "key", "name", "route", "id", "catalog"]);
    for (let i = 0; i < argv.length; i += 1) {
        const arg = argv[i];
        if (!arg.startsWith("--")) {
            positional.push(arg);
            continue;
        }
        const name = arg.slice(2);
        if (takesValue.has(name)) {
            flags[name] = argv[i + 1];
            i += 1;
        } else {
            flags[name] = true;
        }
    }
    return { command: positional.shift(), positional, flags };
}

const { command, positional, flags } = parseArgs(process.argv.slice(2));
const base = (flags.base || process.env.PAGEKIT_BASE || DEFAULT_BASE).replace(/\/+$/, "");
const asJson = Boolean(flags.json);

/* ------------------------------- 输出助手 ------------------------------- */

function out(text) {
    process.stdout.write(text + "\n");
}
function err(text) {
    process.stderr.write(text + "\n");
}
function print(data, human) {
    if (asJson) {
        out(JSON.stringify(data, null, 2));
    } else {
        out(human);
    }
}
class CliError extends Error {}
function fail(message) {
    throw new CliError(message);
}
function pad(text, width) {
    const value = text === null || text === undefined ? "" : String(text);
    // 中文按两个宽度算，表格才对齐
    const visual = [...value].reduce((sum, ch) => sum + (ch.charCodeAt(0) > 0x2e80 ? 2 : 1), 0);
    return value + " ".repeat(Math.max(0, width - visual));
}

/* ------------------------------- 令牌与请求 ------------------------------- */

function tokenFilePath() {
    return flags["token-file"] ? resolve(flags["token-file"]) : DEFAULT_TOKEN_FILE;
}

function readToken() {
    if (flags.token) {
        return flags.token;
    }
    if (process.env.PAGEKIT_TOKEN) {
        return process.env.PAGEKIT_TOKEN;
    }
    const file = tokenFilePath();
    if (existsSync(file)) {
        try {
            const parsed = JSON.parse(readFileSync(file, "utf8"));
            if (parsed.token) {
                return parsed.token;
            }
        } catch {
            fail(`令牌文件不是合法 JSON：${file}`);
        }
    }
    return null;
}

async function request(path, { method = "GET", body, needToken = true } = {}) {
    const headers = {};
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }
    if (needToken) {
        const token = readToken();
        if (!token) {
            fail(`缺少令牌：先跑 'pagekit login'，或设置环境变量 PAGEKIT_TOKEN / 传 --token`);
        }
        headers.Authorization = `Bearer ${token}`;
    }
    let response;
    try {
        response = await fetch(base + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch (e) {
        fail(`连不上后端 ${base}：${e.message}（后端起了吗？--base 对吗？）`);
    }
    const text = await response.text();
    let parsed;
    try {
        parsed = text ? JSON.parse(text) : {};
    } catch {
        fail(`后端返回的不是 JSON（HTTP ${response.status}）：${text.slice(0, 200)}`);
    }
    if (typeof parsed.code === "number" && parsed.code !== 200) {
        fail(`${parsed.code} ${parsed.message}`);
    }
    return parsed.data;
}

/* ------------------------------- 目录快照 ------------------------------- */

function loadCatalog(explicitPath) {
    const file = explicitPath ? resolve(explicitPath) : DEFAULT_CATALOG;
    if (!existsSync(file)) {
        fail(`组件库快照不存在：${file}\n先跑一次 'pagekit catalog' 拉取（需要后端在跑）。`);
    }
    try {
        return { file, catalog: JSON.parse(readFileSync(file, "utf8")) };
    } catch (e) {
        fail(`组件库快照不是合法 JSON：${file}（${e.message}）`);
    }
}

function typeIndex(catalog) {
    const map = new Map();
    for (const type of catalog.types || []) {
        map.set(type.typeCode, type);
    }
    return map;
}

/* --------------------------- 页面引用（id 或 key） --------------------------- */

async function resolvePageId(ref) {
    if (ref === undefined || ref === null || ref === "") {
        fail("缺少页面参数：传页面 id 或 pageKey");
    }
    if (/^\d+$/.test(String(ref))) {
        return Number(ref);
    }
    const pages = await request("/admin/site/pages");
    const hit = (pages || []).find((page) => page.pageKey === ref);
    if (!hit) {
        const list = (pages || []).map((page) => page.pageKey).join(", ");
        fail(`找不到 pageKey=${ref} 的页面；现有页面：${list || "（无）"}`);
    }
    return hit.id;
}

/* ------------------------------- 节点变换 ------------------------------- */

const NODE_FIELDS = ["id", "parentId", "typeCode", "nodeName", "sort", "visible", "styleJson", "contentJson", "behaviorJson", "remark"];

/** 读取文件里的节点数组：接受裸数组，或 { pageKey, nodes:[...] } */
function loadNodeFile(file) {
    const path = resolve(file);
    if (!existsSync(path)) {
        fail(`文件不存在：${path}`);
    }
    let parsed;
    try {
        parsed = JSON.parse(readFileSync(path, "utf8"));
    } catch (e) {
        fail(`不是合法 JSON：${path}（${e.message}）`);
    }
    const nodes = Array.isArray(parsed) ? parsed : parsed && parsed.nodes;
    if (!Array.isArray(nodes)) {
        fail(`文件里没有节点数组：期望裸数组，或 { "nodes": [...] }`);
    }
    if (!nodes.length) {
        fail("节点数组是空的（后端也会拒绝空树）");
    }
    return { path, nodes, wrapper: Array.isArray(parsed) ? null : parsed };
}

/** 把接口返回的树（带 children）压平成可回灌保存的扁平数组 */
function flatten(tree, parentId = null, acc = []) {
    for (const node of tree || []) {
        const flat = {
            id: node.id,
            parentId,
            typeCode: node.typeCode,
            nodeName: node.nodeName,
            sort: node.sort,
            visible: node.visible,
            styleJson: node.style || {},
            contentJson: node.content || {},
            behaviorJson: node.behavior || {},
            remark: node.remark,
        };
        acc.push(flat);
        flatten(node.children, node.id, acc);
    }
    return acc;
}

/* ------------------------------- validate ------------------------------- */

function validateNodes(nodes, catalog) {
    const errors = [];
    const warnings = [];
    const types = typeIndex(catalog);

    const positiveIds = new Set();
    const tempIds = new Set();
    for (const [index, node] of nodes.entries()) {
        const at = `第 ${index + 1} 个节点（${node.typeCode || "缺少 typeCode"}${node.nodeName ? " / " + node.nodeName : ""}）`;
        const id = node.id;
        if (id === undefined || id === null) {
            continue;
        }
        if (!Number.isInteger(id)) {
            errors.push(`${at}：id 必须是整数（新增节点用负数临时 id，如 -1）`);
            continue;
        }
        if (id > 0) {
            if (positiveIds.has(id)) {
                errors.push(`${at}：id ${id} 重复`);
            }
            positiveIds.add(id);
        } else {
            if (tempIds.has(id)) {
                errors.push(`${at}：临时 id ${id} 重复`);
            }
            tempIds.add(id);
        }
    }

    const byId = new Map();
    for (const node of nodes) {
        if (node.id !== undefined && node.id !== null) {
            byId.set(node.id, node);
        }
    }

    const schemaOf = { styleJson: "styleSchema", contentJson: "contentSchema", behaviorJson: "behaviorSchema" };

    for (const [index, node] of nodes.entries()) {
        const at = `第 ${index + 1} 个节点（${node.typeCode || "缺少 typeCode"}${node.nodeName ? " / " + node.nodeName : ""}）`;

        for (const key of Object.keys(node)) {
            if (!NODE_FIELDS.includes(key)) {
                warnings.push(`${at}：字段 ${key} 不是保存接口认识的字段，会被忽略（拼写错了？）`);
            }
        }

        if (!node.typeCode) {
            errors.push(`${at}：缺少 typeCode`);
            continue;
        }
        const type = types.get(node.typeCode);
        if (!type) {
            const codes = [...types.keys()].join(", ");
            errors.push(`${at}：组件库里没有 typeCode=${node.typeCode}；可用：${codes}`);
            continue;
        }
        if (type.status !== undefined && type.status !== 1) {
            errors.push(`${at}：组件 ${node.typeCode} 已停用（status=${type.status}）`);
        }
        if (type.isBuiltin === 1 && !RENDERED_BUILTINS.has(type.renderKind)) {
            warnings.push(`${at}：${node.typeCode} 是内置业务组件，用户端渲染内核还没接入，页面里只会看到占位`);
        }

        for (const [field, schemaField] of Object.entries(schemaOf)) {
            const values = node[field];
            if (values === undefined || values === null || Object.keys(values).length === 0) {
                continue;
            }
            if (typeof values !== "object" || Array.isArray(values)) {
                errors.push(`${at}：${field} 必须是对象`);
                continue;
            }
            const allowed = type[schemaField] || [];
            const unknown = Object.keys(values).filter((key) => !allowed.includes(key));
            if (unknown.length) {
                errors.push(`${at}：${field} 里有该组件未开放的属性 ${unknown.join(", ")}；可用：${allowed.join(", ")}`);
            }
            // 值类型规则（颜色/length/枚举…）由后端保证，这里只挑一种必然踩坑的写法提醒：
            // length 只接受单值，枚举值不会带 px/% 等单位，所以这个启发式不会误报枚举。
            for (const [key, value] of Object.entries(values)) {
                if (typeof value === "string" && /\s/.test(value.trim()) && /\d(px|%|rem|em|vh|vw)\b/.test(value)) {
                    warnings.push(`${at}：${field}.${key} = "${value}" 看起来是尺寸的多值简写，后端 length 类型只接受单值（如 "8px"），这一项会在 save 时被拒`);
                }
            }
        }

        if (node.sort !== undefined && node.sort !== null && (!Number.isInteger(node.sort) || node.sort < 0)) {
            errors.push(`${at}：sort 必须是不小于 0 的整数`);
        }
        if (node.visible !== undefined && node.visible !== null && ![0, 1].includes(node.visible)) {
            errors.push(`${at}：visible 只能是 1（可见）或 0（隐藏）`);
        }

        const parentId = node.parentId;
        if (parentId === undefined || parentId === null) {
            const style = node.styleJson || {};
            // 「行」组件本身就是靠 flexDirection=row 横排的，只有别的组件在根上这么写才是踩约定
            if (style.flexDirection === "row" && node.typeCode !== "row") {
                warnings.push(`${at}：根节点直接用 flexDirection=row 横排。项目约定页面根固定竖排，横排请改用「行」组件（typeCode=row，见 references/layout-rules.md）`);
            }
        } else if (parentId < 0) {
            const parent = byId.get(parentId);
            if (!parent) {
                errors.push(`${at}：parentId=${parentId} 指向的临时 id 不在本次提交里`);
                continue;
            }
            if (parent.typeCode && types.get(parent.typeCode)?.isContainer !== 1) {
                errors.push(`${at}：父节点 ${parent.typeCode} 不是容器（isContainer≠1），不能放子节点`);
            }
        } else if (parentId > 0) {
            const parent = byId.get(parentId);
            if (!parent) {
                warnings.push(`${at}：父节点 id=${parentId} 不在本次提交里（视为既有节点，离线无法确认它是不是容器，后端会校验）`);
            } else if (types.get(parent.typeCode)?.isContainer !== 1) {
                errors.push(`${at}：父节点 ${parent.typeCode} 不是容器（isContainer≠1），不能放子节点`);
            }
        }

        if (node.renderKind === undefined && (type.renderKind === "image" || type.renderKind === "icon")) {
            const key = type.renderKind === "icon" ? "icon" : "imageUrl";
            if (!(node.contentJson || {})[key]) {
                warnings.push(`${at}：${type.renderKind} 组件没有填 ${key}，页面上是空的`);
            }
        }
    }

    // 环检测：沿 parentId 往上走，最多走节点数步
    for (const [index, node] of nodes.entries()) {
        let cursor = node.parentId;
        let hops = 0;
        while (cursor !== undefined && cursor !== null) {
            if (hops > nodes.length) {
                errors.push(`第 ${index + 1} 个节点：父子关系存在环`);
                break;
            }
            const parent = byId.get(cursor);
            cursor = parent ? parent.parentId : null;
            hops += 1;
        }
    }

    // 同级 sort 重复
    const byParent = new Map();
    for (const node of nodes) {
        const key = String(node.parentId ?? "root");
        if (!byParent.has(key)) {
            byParent.set(key, new Map());
        }
        const sorts = byParent.get(key);
        const sort = node.sort ?? 0;
        sorts.set(sort, (sorts.get(sort) || 0) + 1);
    }
    for (const [key, sorts] of byParent) {
        for (const [sort, count] of sorts) {
            if (count > 1) {
                warnings.push(`同一父节点（${key === "root" ? "页面根" : "id=" + key}）下有 ${count} 个节点 sort=${sort}，顺序不确定`);
            }
        }
    }

    const stats = {
        total: nodes.length,
        newNodes: nodes.filter((node) => node.id === undefined || node.id === null || node.id < 0).length,
        roots: nodes.filter((node) => node.parentId === undefined || node.parentId === null).length,
    };
    return { errors, warnings, stats };
}

/* ------------------------------- 各命令 ------------------------------- */

async function cmdLogin() {
    const username = flags.user || positional[0];
    const password = flags.password || positional[1];
    if (!username || !password) {
        fail("用法：pagekit login --user <账号> --password <密码>");
    }
    const data = await request("/admin/login", {
        method: "POST",
        needToken: false,
        body: { username, password },
    });
    if (!data || !data.token) {
        fail("登录成功但没拿到令牌");
    }
    const file = tokenFilePath();
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, JSON.stringify({ base, token: data.token, savedAt: new Date().toISOString() }, null, 2));
    print({ tokenFile: file, base }, `已登录 ${username}，令牌写入 ${file}\n（该目录已在 .gitignore 里，不会提交；生产/别处请优先用环境变量 PAGEKIT_TOKEN）`);
}

async function cmdCatalog() {
    const types = await request("/admin/site/component-types");
    const catalog = {
        exportedAt: new Date().toISOString(),
        base,
        count: (types || []).length,
        types: (types || []).map((type) => ({
            typeCode: type.typeCode,
            typeName: type.typeName,
            category: type.category,
            renderKind: type.renderKind,
            isContainer: type.isContainer,
            isBuiltin: type.isBuiltin,
            status: type.status,
            sort: type.sort,
            styleSchema: type.styleSchema || [],
            contentSchema: type.contentSchema || [],
            behaviorSchema: type.behaviorSchema || [],
            defaultStyle: type.defaultStyle || {},
            defaultContent: type.defaultContent || {},
        })),
    };
    const file = flags.out ? resolve(flags.out) : DEFAULT_CATALOG;
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, JSON.stringify(catalog, null, 2) + "\n");
    const containers = catalog.types.filter((type) => type.isContainer === 1).map((type) => type.typeCode);
    const builtins = catalog.types.filter((type) => type.isBuiltin === 1).map((type) => type.typeCode);
    const renderedBuiltins = catalog.types
        .filter((type) => type.isBuiltin === 1 && RENDERED_BUILTINS.has(type.renderKind))
        .map((type) => type.typeCode);
    print(
        { file, ...catalog },
        [
            `已导出 ${catalog.count} 个组件到 ${file}`,
            `容器组件：${containers.join(", ") || "（无）"}`,
            `内置业务组件：${builtins.join(", ") || "（无）"}`,
            `其中用户端已接渲染内核：${renderedBuiltins.join(", ") || "（无）"}`,
        ].join("\n"),
    );
}

async function cmdPages() {
    const pages = await request("/admin/site/pages");
    const rows = (pages || []).map((page) => [
        page.id,
        page.pageKey,
        page.pageName,
        page.routePath || "-",
        page.isHome === 1 ? "首页" : "",
        page.status === 1 ? "启用" : "停用",
        page.nodeCount,
    ]);
    const header = ["id", "pageKey", "pageName", "routePath", "home", "status", "nodes"];
    const lines = [header.map((h) => pad(h, 14)).join("")];
    for (const row of rows) {
        lines.push(row.map((cell) => pad(cell, 14)).join(""));
    }
    print({ pages }, lines.join("\n") || "（还没有页面）");
}

async function cmdGet() {
    const pageId = await resolvePageId(positional[0]);
    const tree = await request(`/admin/site/pages/${pageId}/nodes`);
    const nodes = flatten(tree);
    if (flags.out) {
        const file = resolve(flags.out);
        writeFileSync(file, JSON.stringify(nodes, null, 2) + "\n");
        print({ file, nodeCount: nodes.length }, `已导出 ${nodes.length} 个节点到 ${file}\n（整树保存＝未提交的旧节点会被删除；改完再 save 前先确认没漏节点）`);
        return;
    }
    print(nodes, `页面 ${pageId} 共 ${nodes.length} 个节点：\n` + nodes.map((node) => `  ${node.id}  ${"  ".repeat(0)}${node.typeCode}  ${node.nodeName || ""}`).join("\n"));
}

async function cmdValidate() {
    const { path, nodes } = loadNodeFile(positional[0]);
    const { file, catalog } = loadCatalog(flags.catalog);
    const result = validateNodes(nodes, catalog);
    const ok = result.errors.length === 0;
    const human = [
        ok ? `✔ 校验通过：${result.stats.total} 个节点（新增 ${result.stats.newNodes}、根节点 ${result.stats.roots}）` : `✘ 校验未通过：${result.errors.length} 个错误`,
        ...result.errors.map((e) => `  [错误] ${e}`),
        ...result.warnings.map((w) => `  [提醒] ${w}`),
        `（依据快照：${file}；值类型如颜色/length/枚举由后端在 save 时校验）`,
    ].join("\n");
    print({ ok, file: path, catalog: file, ...result }, human);
    if (!ok) {
        process.exitCode = 1;
    }
}

async function cmdSave() {
    const pageId = await resolvePageId(positional[0]);
    const { path, nodes } = loadNodeFile(positional[1]);
    const { catalog } = loadCatalog(flags.catalog);
    const local = validateNodes(nodes, catalog);
    if (local.errors.length) {
        err(`本地预检未通过（未提交）：\n` + local.errors.map((e) => `  [错误] ${e}`).join("\n"));
        process.exitCode = 1;
        return;
    }
    const tree = await request(`/admin/site/pages/${pageId}/nodes`, { method: "PUT", body: nodes });
    const saved = flatten(tree);
    print(
        { pageId, file: path, saved: saved.length },
        `已保存到页面 ${pageId}：${saved.length} 个节点（来自 ${path}）\n` +
            local.warnings.map((w) => `  [提醒] ${w}`).join("\n"),
    );
}

async function cmdCreatePage() {
    const pageKey = flags.key || positional[0];
    const pageName = flags.name || positional[1];
    if (!pageKey || !pageName) {
        fail("用法：pagekit create-page --key <pageKey> --name <页面名> [--route /path]");
    }
    await request("/admin/site/pages", {
        method: "POST",
        body: { pageKey, pageName, routePath: flags.route || null },
    });
    const pages = await request("/admin/site/pages");
    const created = (pages || []).find((page) => page.pageKey === pageKey);
    print({ page: created }, `已创建页面 ${pageKey}（id=${created ? created.id : "?"}），接着用 'pagekit save <id> <file>' 落节点`);
}

async function cmdDeletePage() {
    const pageId = await resolvePageId(positional[0]);
    await request(`/admin/site/pages/${pageId}`, { method: "DELETE" });
    print({ pageId }, `已删除页面 ${pageId}`);
}

/* ------------------------------- 入口 ------------------------------- */

const COMMANDS = {
    login: cmdLogin,
    catalog: cmdCatalog,
    pages: cmdPages,
    get: cmdGet,
    validate: cmdValidate,
    save: cmdSave,
    "create-page": cmdCreatePage,
    "delete-page": cmdDeletePage,
};

async function main() {
    if (!command || flags.help) {
        out(
            [
                "pagekit —— 生成/校验/落库符合本项目规则的页面（呈现层）",
                "",
                "用法：node pagekit.mjs <命令> [参数] [--base url] [--json]",
                "",
                "命令：",
                "  login --user <账号> --password <密码>     登录并缓存令牌",
                "  catalog [--out <file>]                    导出组件库快照（validate 的依据）",
                "  pages                                     列出页面",
                "  get <pageId|pageKey> [--out <file>]       导出页面节点（可回灌保存的扁平数组）",
                "  validate <file>                           离线结构预检",
                "  save <pageId|pageKey> <file>              整树保存（未提交的旧节点会被删除）",
                "  create-page --key <k> --name <n>          新建页面",
                "  delete-page <pageId|pageKey>              删除页面",
                "",
                `默认后端：${DEFAULT_BASE}（--base 或 PAGEKIT_BASE 覆盖）`,
            ].join("\n"),
        );
        return;
    }
    const handler = COMMANDS[command];
    if (!handler) {
        fail(`未知命令：${command}（可用：${Object.keys(COMMANDS).join(" / ")}）`);
    }
    await handler();
}

main().catch((e) => {
    if (e instanceof CliError) {
        err(`✘ ${e.message}`);
    } else {
        err(`✘ 未预期错误：${e.stack || e.message}`);
    }
    process.exitCode = 1;
});
