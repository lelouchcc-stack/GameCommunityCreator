# 排版与嵌套约定（后端不拦，但踩了就不像这个项目做的）

## 1. 页面根固定竖排，横排必须用「行」

页面根容器是 `flex-direction: column`：多个根节点从上往下排。
**不要**给根节点或普通组件设 `flexDirection: row` —— 想在"一行里并排"就插一个
`typeCode: "row"` 的容器，把要并排的节点作为它的子节点：

```json
{ "typeCode": "row", "parentId": null, "sort": 2,
  "styleJson": { "flexDirection": "row", "wrap": true, "gap": "12px", "justify": "center", "alignItems": "center",
                 "marginTop": "16px" } }
```

「行」的惯用属性：`flexDirection=row`、`wrap`（窄屏换行）、`gap`、`justify`、`alignItems`。
`validate` 会对"根节点直接横排"给提醒。

## 2. 只有容器能放子节点

`isContainer=1` 的组件：**container / card / modal / row**。
把子节点挂到别的类型下，后端会返回「父节点不是容器，不能容纳子节点」。
其余都是叶子：文本、标题、图片、按钮、分隔线、间距、图标。

## 3. 空容器在管理端画布要有占位才选得中

容器里一个子节点都没有时，在管理端画布上高度为 0、点不中，也就没法往里加东西。
用 `container`/`card` 搭结构时，先放内容再调样式；纯占位可以让它先有一个子节点。

## 4. 排序与显隐

- `sort`：**同一父节点下**从 0 开始递增；同级重复 sort 顺序不确定（`validate` 会提醒）。
- `visible`：`1` 可见 / `0` 隐藏（缺省 1）。整棵子树跟随父节点显隐。

## 5. 命名与备注

`nodeName` 是给管理端画布认的（用户端不显示），用「页面标题」「按钮行」这类能一眼认出的名字；
`remark` 写"为什么这么排"，方便日后维护。

## 6. 对齐方式的语义

`align` 是**在自身可用宽度里**怎么摆（`left` / `center` / `right` / `full`），
`textAlign` 是文字在元素内的对齐。要"整块居中"通常写 `align: "center"`。

## 7. 间距

组件类型开放的属性各不相同：容器类有 `marginTop` / `marginBottom` / `padding` / `gap`，
文本与标题类往往只有 `padding`（没有 margin）。**别假设某个组件有 margin** ——
以 `references/component-catalog.json` 里该类型的 `styleSchema` 为准。
间距的常见做法：外层容器的 `padding` + 「行」的 `gap`，而不是给每个叶子加 margin。
