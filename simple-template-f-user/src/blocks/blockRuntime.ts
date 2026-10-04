/**
 * 区块运行时（管理端与用户端同源副本）：管弹窗显隐、节点显隐与点击动作。
 * 阶段2 只有"简单动作"（打开/关闭弹窗、跳转、滚动、提示）；完整逻辑图在阶段3。
 *
 * 宿主应用通过 provide(BLOCK_RUNTIME_KEY, runtime) 注入，并传自己的跳转/提示实现。
 */
import { reactive, type InjectionKey } from "vue";

export type BlockActionName =
    | "none"
    | "openModal"
    | "closeModal"
    | "navigate"
    | "scrollTo"
    | "toggleVisible"
    | "toast";

export interface BlockAction {
    action?: BlockActionName
    /** 目标节点 id（openModal / closeModal / scrollTo / toggleVisible 用） */
    target?: number | null
    url?: string
    newTab?: boolean
    message?: string
    toastType?: "info" | "success" | "warning" | "error"
}

export interface BlockHost {
    navigate: (url: string, newTab?: boolean) => void
    toast: (message: string, type?: string) => void
    scrollTo: (nodeId: number) => void
}

export interface BlockRuntime {
    /** 弹窗显示状态：节点 id -> 是否显示 */
    modalVisible: Record<number, boolean>
    /** 普通节点被动作隐藏的状态：节点 id -> 是否隐藏 */
    hidden: Record<number, boolean>
    openModal: (id?: number | null) => void
    closeModal: (id?: number | null) => void
    toggleVisible: (id?: number | null) => void
    /** 显式设置显隐（逻辑图的 act.setVisible 用；与 toggleVisible 共用 hidden 表） */
    setVisible: (id?: number | null, visible?: boolean) => void
    isHidden: (id: number) => boolean
    isModalOpen: (id: number) => boolean
    run: (action?: BlockAction | null) => void
}

export const BLOCK_RUNTIME_KEY = Symbol("block-runtime") as InjectionKey<BlockRuntime>;

function defaultHost(): BlockHost {
    return {
        navigate: (url) => {
            window.location.href = url;
        },
        toast: (message) => {
            if (message) {
                window.alert(message);
            }
        },
        scrollTo: (nodeId) => {
            document
                .querySelector(`[data-node-id="${nodeId}"]`)
                ?.scrollIntoView({ behavior: "smooth", block: "center" });
        },
    };
}

export function createBlockRuntime(host?: Partial<BlockHost>): BlockRuntime {
    const merged: BlockHost = { ...defaultHost(), ...(host ?? {}) };
    const modalVisible = reactive<Record<number, boolean>>({});
    const hidden = reactive<Record<number, boolean>>({});

    function openModal(id?: number | null) {
        if (id !== null && id !== undefined) {
            modalVisible[id] = true;
        }
    }

    function closeModal(id?: number | null) {
        if (id !== null && id !== undefined) {
            modalVisible[id] = false;
        }
    }

    function toggleVisible(id?: number | null) {
        if (id !== null && id !== undefined) {
            hidden[id] = !hidden[id];
        }
    }

    function setVisible(id?: number | null, visible = true) {
        if (id !== null && id !== undefined) {
            hidden[id] = !visible;
        }
    }

    function isHidden(id: number) {
        return hidden[id] === true;
    }

    function isModalOpen(id: number) {
        return modalVisible[id] === true;
    }

    function run(action?: BlockAction | null) {
        if (!action || !action.action || action.action === "none") {
            return;
        }
        switch (action.action) {
            case "openModal":
                openModal(action.target);
                break;
            case "closeModal":
                closeModal(action.target);
                break;
            case "toggleVisible":
                toggleVisible(action.target);
                break;
            case "navigate":
                if (action.url) {
                    merged.navigate(action.url, action.newTab);
                }
                break;
            case "scrollTo":
                if (action.target !== null && action.target !== undefined) {
                    merged.scrollTo(action.target);
                }
                break;
            case "toast":
                merged.toast(action.message ?? "", action.toastType);
                break;
            default:
                break;
        }
    }

    return { modalVisible, hidden, openModal, closeModal, toggleVisible, setVisible, isHidden, isModalOpen, run };
}
