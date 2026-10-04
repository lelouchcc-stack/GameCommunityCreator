/**
 * 值引用求值（管理端与用户端同源副本）：字面量 / $param.x / $ref.<节点>.<端口>[.<字段>] / $runtime.x
 *
 * 求值可能"还没就绪"（上游没跑过），所以用 UNRESOLVED 显式区分"没有值"与"值是 undefined"。
 */
import type { LogicGraph, LogicNode } from "./types";

const REF = /^\$ref\.([^.]+)\.([^.]+)((?:\.[^.]*)*)$/;
const PARAM = /^\$param\.([A-Za-z0-9_]+)$/;
const RUNTIME = /^\$runtime\.([A-Za-z0-9_.]+)$/;

export const UNRESOLVED: unique symbol = Symbol("unresolved");

export interface EvalContext {
    /** 运行期产出：nodeId -> 端口 -> 值 */
    outputs: Record<string, Record<string, any>>
    /** 预设入参（图内以 $param.x 引用；页面图没有入参） */
    params: Record<string, any>
    /** 运行环境：routeParam.id / query.k / page */
    runtime: Record<string, any>
    /**
     * 预设展开时内层节点的键前缀（页面图为空）。
     * $ref 里写的是"本层图内"的节点 id，而 outputs 存的是加过前缀的键（如 s4/p1），
     * 所以解析 $ref 必须补上这个前缀——否则预设内部节点之间的 $ref 一律解析不出来。
     */
    prefix?: string
}

export function isBinding(value: unknown): boolean {
    return typeof value === "string" && value.startsWith("$");
}

/**
 * 求值：不是引用就原样返回；引用解析不出来则返回 UNRESOLVED。
 */
export function evalValue(value: unknown, ctx: EvalContext): unknown {
    if (typeof value !== "string" || !value.startsWith("$")) {
        return value;
    }
    const param = PARAM.exec(value);
    if (param?.[1]) {
        const key = param[1];
        return hasOwn(ctx.params, key) ? ctx.params[key] : UNRESOLVED;
    }
    const runtime = RUNTIME.exec(value);
    if (runtime?.[1]) {
        return readPath(ctx.runtime, runtime[1]);
    }
    const ref = REF.exec(value);
    if (ref?.[1] && ref[2]) {
        const ports = ctx.outputs[(ctx.prefix ?? "") + ref[1]];
        const port = ref[2];
        if (!ports || !hasOwn(ports, port)) {
            return UNRESOLVED;
        }
        const base = ports[port];
        const rest = ref[3] ? ref[3].slice(1) : "";
        return rest ? readPath(base, rest) : base;
    }
    // 认不出的 $ 开头：当字面量，避免把正常文案吃掉
    return value;
}

function hasOwn(source: object, key: string): boolean {
    return Object.prototype.hasOwnProperty.call(source, key);
}

/** 求值一组设置项；只要有一项没就绪就整体 UNRESOLVED（节点还不该执行） */
export function evalConfig(config: Record<string, any> | undefined, ctx: EvalContext): Record<string, any> | typeof UNRESOLVED {
    const result: Record<string, any> = {};
    for (const [key, value] of Object.entries(config ?? {})) {
        const resolved = evalValue(value, ctx);
        if (resolved === UNRESOLVED) {
            return UNRESOLVED;
        }
        result[key] = resolved;
    }
    return result;
}

function readPath(source: any, path: string): unknown {
    let current = source;
    for (const segment of path.split(".")) {
        if (current === null || current === undefined || typeof current !== "object") {
            return UNRESOLVED;
        }
        if (!Object.prototype.hasOwnProperty.call(current, segment)) {
            return UNRESOLVED;
        }
        current = current[segment];
    }
    return current;
}

/** 预设引用节点的键：code@version */
export function presetKey(node: Pick<LogicNode, "presetCode" | "presetVersion">): string {
    return `${node.presetCode}@${node.presetVersion}`;
}

export type { LogicGraph, LogicNode };
