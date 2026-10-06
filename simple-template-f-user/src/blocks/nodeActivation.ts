/**
 * 业务组件内部被点击时的统一出口（管理端与用户端同源副本）。
 *
 * 背景：业务组件的点击行为原先各自写死在组件里（板块卡片跳板块、帖子条目跳帖子详情、
 * 公告条目跳公告详情…），于是出现两种坏情况：
 *   * 组件里 stopPropagation 的（如 board_nav 的卡片）—— 事件传不到外层节点，
 *     在「逻辑编排」里给这个组件挂的「点击组件」图**永远不会触发**；
 *   * 不 stopPropagation 的（如 post_list / hot_post / notice）—— 事件会冒到外层节点，
 *     于是组件自带的跳转与节点上挂的图/动作**同时执行**，一次点击做两件事。
 *
 * 这里把"该做什么"的决策权交回节点本身，优先级与渲染器对外层盒子的处理完全一致：
 *
 *     节点上的点击图  >  节点的 onClickAction（简单动作）  >  组件的默认动作
 *
 * 只是把发起方从"外层盒子"换成"被点的那个元素"。返回值表示这次点击是否已被处理：
 * 已处理就由调用方 stopPropagation，免得外层 .ui-node 再处理一遍。
 *
 * 另：画布（interactive）下直接返回 false 且不拦事件——点击留给画布选中该节点。
 */
import type { UiNode } from "../types/uiBuilder";
import type { BlockAction, BlockRuntime } from "./blockRuntime";
import type { GraphBridge } from "./graphBridge";

export interface NodeActivation {
    node: UiNode
    runtime: BlockRuntime
    graphBridge: GraphBridge | null
    /** 管理端画布为 true：不处理点击，交给画布选中节点 */
    interactive?: boolean
    /** 组件自带的默认动作（如跳到该条目的详情页） */
    defaultAction?: BlockAction | null
}

export function activateNode(options: NodeActivation): boolean {
    const { node, runtime, graphBridge, interactive, defaultAction } = options;
    if (interactive) {
        return false;
    }
    // 1) 节点上挂了"点击组件"的图 → 由页面逻辑决定做什么，组件不再自作主张
    if (graphBridge?.hasClickGraph(node.id)) {
        void graphBridge.triggerEvent("click", node.id).catch(() => undefined);
        return true;
    }
    // 2) 节点上配了简单点击动作（跳转/提示/打开弹窗/滚动…）
    const action = node.behavior?.onClickAction as BlockAction | undefined;
    if (action && action.action && action.action !== "none") {
        runtime.run(action);
        return true;
    }
    // 3) 都没配 → 走组件默认行为，保持"不改配置就和以前一样"
    if (defaultAction && defaultAction.action && defaultAction.action !== "none") {
        runtime.run(defaultAction);
        return true;
    }
    return false;
}
