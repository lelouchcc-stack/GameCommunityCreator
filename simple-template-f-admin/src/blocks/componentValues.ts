/**
 * 组件值注册表（管理端与用户端同源副本）。
 *
 * 输入类组件把自己的当前值登记到这里，逻辑图的 data.componentValue 原子按
 * 「节点 id + 字段名」读——图不认识组件类型、只认识 id，所以这是两者之间唯一的桥。
 * 键是 `<节点id>.<字段名>`，输入框登记的是 `<id>.value`。
 *
 * 注意方向是单向的：注册表给图读值用，不要反过来拿它驱动输入框的显示值
 * （会让输入法组合输入中断）——输入框的显示值仍是组件自己的本地状态。
 */
import { reactive } from "vue";

const values = reactive<Record<string, any>>({});

export function setComponentValue(nodeId: number | string, field: string, value: any): void {
    values[`${nodeId}.${field}`] = value;
}

/** 图读值：没登记过返回 undefined，让「值为空」与「组件没登记」在图上可区分 */
export function readComponentValue(nodeId: number | string, field = "value"): any {
    return values[`${nodeId}.${field}`];
}

/** 组件卸载时清掉自己那一项，避免留下读得到却已经不在页面上的值 */
export function clearComponentValue(nodeId: number | string, field = "value"): void {
    delete values[`${nodeId}.${field}`];
}

/** 整体清空：换页时用（节点 id 全局唯一，但换页清一次更干净） */
export function clearComponentValues(): void {
    for (const key of Object.keys(values)) {
        delete values[key];
    }
}
