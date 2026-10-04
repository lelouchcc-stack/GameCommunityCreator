/**
 * 轻量提示：用户端没引 Element Plus，自己建一个临时节点显示。
 * 供区块运行时（blocks/blockRuntime.ts）的 toast 动作用。
 */
const COLORS: Record<string, string> = {
    info: "#303133",
    success: "#67c23a",
    warning: "#e6a23c",
    error: "#f56c6c",
};

export function toast(message: string, type?: string) {
    if (!message) {
        return;
    }
    const el = document.createElement("div");
    el.textContent = message;
    el.style.cssText = [
        "position:fixed",
        "left:50%",
        "top:24px",
        "transform:translateX(-50%)",
        "z-index:9999",
        "padding:8px 16px",
        "border-radius:6px",
        "font-size:14px",
        "color:#fff",
        "box-shadow:0 4px 12px rgba(0,0,0,.15)",
        "opacity:0",
        "transition:opacity .2s ease",
        `background:${COLORS[type ?? "info"] ?? COLORS.info}`,
    ].join(";");
    document.body.appendChild(el);
    requestAnimationFrame(() => {
        el.style.opacity = "1";
    });
    window.setTimeout(() => {
        el.style.opacity = "0";
        window.setTimeout(() => el.remove(), 200);
    }, 2000);
}
