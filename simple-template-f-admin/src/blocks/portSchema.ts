/**
 * 图节点的端口/设置项推导（管理端专用；与后端 GraphValidator.atomPorts / presetPorts 同一套规则）。
 * 画布（连线校验）与属性面板的绑定下拉都用它，避免两处各写一份后走样。
 */
import type { LogicAtom, LogicNode, LogicPort, Preset } from "../types/uiLogic";

/** 预设节点的端口：signal 进、out 出，中间是预设自己声明的入参/出参 */
export function presetPorts(preset: Preset | undefined): { inputs: LogicPort[]; outputs: LogicPort[] } {
    if (!preset) {
        return { inputs: [], outputs: [] };
    }
    return {
        inputs: [{ key: "signal", type: "signal" }, ...preset.inputs],
        outputs: [...preset.outputs, { key: "out", type: "signal" }],
    };
}

/** 一个图节点的输入/输出端口 */
export function portsOfNode(
    node: LogicNode,
    atoms: LogicAtom[],
    presets: Preset[],
): { inputs: LogicPort[]; outputs: LogicPort[] } {
    if (node.typeCode === "preset") {
        const preset = presets.find(
            (item) => item.presetCode === node.presetCode && item.version === node.presetVersion,
        );
        return presetPorts(preset);
    }
    const atom = atoms.find((item) => item.typeCode === node.typeCode);
    return { inputs: atom?.inputs ?? [], outputs: atom?.outputs ?? [] };
}

/**
 * 节点上可填的设置项 = 声明的 config 键 ∪ 数据输入端口（signal 只用于连线，不在这里填）。
 * 复用了"输入端口也能在 config 里给字面量/引用"这条约定（见设计 graphModel 的示例写法）。
 */
export function settingsOfNode(node: LogicNode, atoms: LogicAtom[], presets: Preset[]): LogicPort[] {
    const merged = new Map<string, LogicPort>();
    if (node.typeCode === "preset") {
        const preset = presets.find(
            (item) => item.presetCode === node.presetCode && item.version === node.presetVersion,
        );
        for (const port of preset?.inputs ?? []) {
            merged.set(port.key, port);
        }
    }
    const atom = atoms.find((item) => item.typeCode === node.typeCode);
    for (const item of atom?.config ?? []) {
        merged.set(item.key, item);
    }
    for (const port of atom?.inputs ?? []) {
        if (port.type !== "signal") {
            merged.set(port.key, port);
        }
    }
    return [...merged.values()];
}
