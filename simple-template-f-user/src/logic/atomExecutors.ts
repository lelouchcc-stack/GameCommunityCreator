/**
 * 原子执行器（管理端与用户端同源副本）：每个原子"到底干什么"。
 *
 * 与原子库种子一一对应：这里返回的端口名必须与 ui_logic_node_type.outputs_schema 一致。
 * runner=api 的原子调 /app/** 接口（玩家侧接口大多还没实现，调用会失败并走错误轨迹——
 * 这是设计里说好的"先占位"）；runner=ui 的原子是前端本地动作或纯函数。
 *
 * 引擎只负责控制流与数据流，具体怎么发请求/跳转/提示由宿主（页面）提供。
 */
import type { EngineState, GraphHost, LogicNode } from "./types";

export interface ExecContext {
    node: LogicNode
    inputs: Record<string, any>
    state: EngineState
    host: GraphHost
}

export type Executor = (ctx: ExecContext) => Promise<Record<string, any>> | Record<string, any>;

/* ---------------- 接口调用 ---------------- */

async function request(host: GraphHost, spec: string, inputs: Record<string, any>): Promise<any> {
    const spaceAt = spec.indexOf(" ");
    const method = spec.slice(0, spaceAt).toUpperCase();
    const template = spec.slice(spaceAt + 1);
    const params: Record<string, any> = {};
    for (const [key, value] of Object.entries(inputs)) {
        if (key !== "signal" && value !== undefined && value !== null && value !== "") {
            params[key] = value;
        }
    }
    // 路径占位符用同名入参填（种子里已改成 {postId}/{boardId} 这种真实入参名）
    const path = template.replace(/\{([A-Za-z0-9_]+)\}/g, (_match, key: string) => {
        const value = params[key];
        if (value === undefined) {
            throw new Error(`接口 ${method} ${template} 缺少路径参数：${key}`);
        }
        delete params[key];
        return encodeURIComponent(String(value));
    });
    return host.callApi(method, path, params);
}

/**
 * 接口返回的 data -> 声明端口。
 * 约定：单端口整体给；多端口先按同名字段取，取不到的——list* 取 records/list/content，
 * pageInfo 由 page/size/total/pages 组成。
 * 玩家侧接口落地后，每个原子的响应映射应逐个定死（当前是通用兜底）。
 */
function mapApiOutputs(ports: string[], data: any): Record<string, any> {
    const out: Record<string, any> = {};
    const [only] = ports;
    if (ports.length === 0 || only === undefined) {
        return out;
    }
    if (ports.length === 1) {
        out[only] = data;
        return out;
    }
    const isPlainObject = data !== null && typeof data === "object" && !Array.isArray(data);
    for (const port of ports) {
        if (isPlainObject && port in data) {
            out[port] = data[port];
        } else if (port.startsWith("list")) {
            out[port] = isPlainObject ? data.records ?? data.list ?? data.content ?? data : data;
        } else if (port === "pageInfo") {
            out[port] = isPlainObject
                ? { page: data.page ?? data.current, size: data.size, total: data.total, pages: data.pages }
                : null;
        } else {
            out[port] = data;
        }
    }
    return out;
}

function apiAtom(spec: string, ports: string[], options: { savesToken?: boolean } = {}): Executor {
    return async ({ inputs, host, state }) => {
        const data = await request(host, spec, inputs);
        if (options.savesToken && data && typeof data === "object" && typeof data.token === "string") {
            state.session.token = data.token;
        }
        return mapApiOutputs(ports, data);
    };
}

/* ---------------- 工具 ---------------- */

const asList = (value: any): any[] => (Array.isArray(value) ? value : []);
const num = (value: any): number => (value === undefined || value === null || value === "" ? NaN : Number(value));

function pick(item: any, keys: string[]): any {
    for (const key of keys) {
        if (item && typeof item === "object" && item[key] !== undefined) {
            return item[key];
        }
    }
    return undefined;
}

function timeValue(item: any): number {
    const raw = pick(item, ["createTime", "createdAt", "releaseTime", "updateTime"]);
    const parsed = raw ? Date.parse(String(raw).replace(" ", "T")) : NaN;
    return Number.isNaN(parsed) ? 0 : parsed;
}

/* ---------------- 执行器表 ---------------- */

export function createExecutors(host: GraphHost): Record<string, Executor> {
    void host;
    return {
        /* 触发原子：由引擎当根驱动，本身不干活 */
        "on.pageEnter": () => ({}),
        "on.click": () => ({}),
        "on.submit": () => ({}),

        /* 数据源（接口） */
        "data.boardList": apiAtom("GET /app/board/list", ["listBoard"]),
        "data.boardDetail": apiAtom("GET /app/board/{boardId}", ["board", "listBoardVersion", "listProgressStage"]),
        "data.postList": apiAtom("GET /app/post/page", ["listPost", "pageInfo"]),
        "data.postDetail": apiAtom("GET /app/post/{postId}", ["post"]),
        "data.commentList": apiAtom("GET /app/comment/page", ["listComment", "pageInfo"]),
        "data.noticeList": apiAtom("GET /app/notice/list", ["listNotice"]),
        "data.currentMember": apiAtom("GET /app/member/me", ["member"]),
        "data.myPosts": apiAtom("GET /app/member/my-posts", ["listPost", "pageInfo"]),
        "data.myFavorites": apiAtom("GET /app/member/my-favorites", ["listPost", "pageInfo"]),
        "data.myReports": apiAtom("GET /app/member/my-reports", ["listReport", "pageInfo"]),
        "data.myProgress": apiAtom("GET /app/member/progress", ["any"]),

        /* 数据源（前端） */
        "data.runtime": ({ inputs, host }) => {
            const key = String(inputs.key ?? "");
            if (!key) {
                // 没设 key 就整包给出去，方便 $ref.x.any.routeParam.id 这种取法
                return { any: host.runtimeValues() };
            }
            const values = host.runtimeValues();
            const value = key.split(".").reduce<any>((acc, part) => (acc == null ? acc : acc[part]), values);
            return { any: value };
        },
        "data.componentValue": ({ inputs, node, host }) => {
            const nodeId = inputs.nodeId ?? node.config?.nodeId;
            if (nodeId === undefined || nodeId === null || nodeId === "") {
                throw new Error("读组件当前值：没有指定组件");
            }
            return { any: host.componentValue(nodeId, inputs.field ?? node.config?.field) };
        },

        /* 算子（纯函数） */
        "op.filterByVersion": ({ inputs }) => ({
            listPost: asList(inputs.list).filter((item) => String(pick(item, ["versionId"])) === String(inputs.versionId)),
        }),
        "op.filterByStage": ({ inputs }) => ({
            listPost: asList(inputs.list).filter((item) => String(pick(item, ["stageId"])) === String(inputs.stageId)),
        }),
        "op.filterByKeyword": ({ inputs }) => {
            const keyword = String(inputs.keyword ?? "").trim().toLowerCase();
            if (!keyword) {
                return { listPost: asList(inputs.list) };
            }
            return {
                listPost: asList(inputs.list).filter((item) =>
                    [pick(item, ["title"]), pick(item, ["content"])]
                        .some((field) => String(field ?? "").toLowerCase().includes(keyword)),
                ),
            };
        },
        "op.sort": ({ inputs }) => {
            const by = String(inputs.by ?? "latest");
            const list = [...asList(inputs.list)];
            list.sort((a, b) =>
                by === "hot" ? Number(pick(b, ["likeCount"]) ?? 0) - Number(pick(a, ["likeCount"]) ?? 0) : timeValue(b) - timeValue(a),
            );
            return { listPost: list };
        },
        "op.take": ({ inputs }) => {
            const n = num(inputs.n);
            const list = asList(inputs.list);
            return { any: Number.isNaN(n) ? list : list.slice(0, Math.max(0, n)) };
        },
        "op.markExpired": ({ inputs }) => {
            const current = inputs.currentVersionId;
            return {
                listPost: asList(inputs.list).map((item) => ({
                    ...item,
                    expired: current !== undefined && current !== null && String(pick(item, ["versionId"])) !== String(current),
                })),
            };
        },
        "op.and": ({ inputs }) => ({ boolean: Boolean(inputs.a) && Boolean(inputs.b) }),
        "op.or": ({ inputs }) => ({ boolean: Boolean(inputs.a) || Boolean(inputs.b) }),
        "op.not": ({ inputs }) => ({ boolean: !inputs.value }),

        /* 条件 */
        "cond.isLogin": ({ host }) => ({ boolean: host.isLoggedIn() }),
        "cond.isEmpty": ({ inputs }) => {
            const value = inputs.value;
            const empty = value === undefined || value === null || value === "" || (Array.isArray(value) && value.length === 0);
            return { boolean: empty };
        },
        "cond.isOwner": ({ inputs, host }) => {
            const current = host.currentMemberId();
            return { boolean: current !== null && String(current) === String(inputs.memberId) };
        },
        "cond.isBoardOpen": ({ inputs }) => ({ boolean: Number(pick(inputs.board, ["status"])) === 1 }),
        "cond.isPostVisible": ({ inputs }) => ({ boolean: Number(pick(inputs.post, ["status"])) === 1 }),
        "cond.isValid": ({ inputs }) => {
            const value = inputs.value;
            const text = value === undefined || value === null ? "" : String(value);
            let ok = true;
            switch (String(inputs.rule ?? "notBlank")) {
                case "notBlank":
                    ok = text.trim().length > 0;
                    break;
                case "maxLen":
                    ok = text.length <= num(inputs.arg);
                    break;
                case "isNumber":
                    ok = text.trim() !== "" && !Number.isNaN(Number(text));
                    break;
                default:
                    ok = true;
            }
            return { boolean: ok };
        },
        /** 分支只看输入，出口由引擎按 cond 真值选 true/false */
        branch: ({ inputs }) => ({ boolean: Boolean(inputs.cond) }),

        /* 动作（接口） */
        "act.login": apiAtom("POST /app/login", ["result"], { savesToken: true }),
        "act.register": apiAtom("POST /app/register", ["result"], { savesToken: true }),
        "act.updateProfile": apiAtom("PUT /app/member/profile", ["result"]),
        "act.setGameProgress": apiAtom("PUT /app/member/progress", ["result"]),
        "act.submitPost": apiAtom("POST /app/post", ["result"]),
        "act.like": apiAtom("POST /app/post/{postId}/like", ["result"]),
        "act.favorite": apiAtom("POST /app/post/{postId}/favorite", ["result"]),
        "act.comment": apiAtom("POST /app/comment", ["result"]),
        "act.report": apiAtom("POST /app/report", ["result"]),

        /* 动作（前端） */
        "act.logout": ({ state, host }) => {
            for (const key of Object.keys(state.session)) {
                delete state.session[key];
            }
            host.logout();
            return {};
        },
        "act.navigate": ({ inputs, host }) => {
            if (!inputs.to) {
                throw new Error("跳转：没有填目标地址");
            }
            host.navigate(String(inputs.to));
            return {};
        },
        "act.openModal": ({ inputs, host }) => {
            host.openModal(inputs.nodeId ?? null);
            host.setVisible(inputs.nodeId, true);
            return {};
        },
        "act.closeModal": ({ inputs, host }) => {
            host.closeModal(inputs.nodeId ?? null);
            return {};
        },
        "act.toast": ({ inputs, host }) => {
            host.toast(String(inputs.message ?? ""), inputs.type ? String(inputs.type) : undefined);
            return {};
        },
        "act.setVisible": ({ inputs, host, state }) => {
            const visible = Boolean(inputs.visible);
            host.setVisible(inputs.nodeId, visible);
            if (inputs.nodeId !== undefined && inputs.nodeId !== null) {
                state.visible[String(inputs.nodeId)] = visible;
            }
            return {};
        },
        "act.scrollTo": ({ inputs, host }) => {
            if (inputs.nodeId !== undefined && inputs.nodeId !== null) {
                host.scrollTo(inputs.nodeId);
            }
            return {};
        },
    };
}
