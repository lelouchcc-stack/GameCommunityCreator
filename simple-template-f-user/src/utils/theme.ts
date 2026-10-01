import type { SiteConfig } from "../types/site";

type Rgb = [number, number, number];

const WHITE: Rgb = [255, 255, 255];
const BLACK: Rgb = [0, 0, 0];
const HEX = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

function toRgb(hex: string): Rgb | null {
    if (!HEX.test(hex)) {
        return null;
    }
    const raw = hex.slice(1);
    const full = raw.length === 3 ? raw.split("").map((c) => c + c).join("") : raw;
    return [
        parseInt(full.slice(0, 2), 16),
        parseInt(full.slice(2, 4), 16),
        parseInt(full.slice(4, 6), 16),
    ];
}

/** 把主色朝白色/黑色混出派生色，避免"只配了一个主色、hover 与浅底还是旧的蓝" */
function mix(hex: string, target: Rgb, ratio: number): string {
    const rgb = toRgb(hex);
    if (!rgb) {
        return hex;
    }
    const [fromR, fromG, fromB] = rgb;
    const [toR, toG, toB] = target;
    const channel = (from: number, to: number) => Math.round(from + (to - from) * ratio);
    return `rgb(${channel(fromR, toR)}, ${channel(fromG, toG)}, ${channel(fromB, toB)})`;
}

/** 主色一族变量，具体用法见 style.css 里的 token */
function primaryVars(primaryColor: string): Record<string, string> {
    if (!toRgb(primaryColor)) {
        return {};
    }
    return {
        "--site-primary": primaryColor,
        "--site-primary-hover": mix(primaryColor, BLACK, 0.14),
        "--site-primary-active": mix(primaryColor, BLACK, 0.28),
        "--site-primary-weak": mix(primaryColor, WHITE, 0.88),
    };
}

function kebab(name: string): string {
    return name.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`);
}

function setFavicon(href: string) {
    let link = document.querySelector<HTMLLinkElement>("link[rel='icon']");
    if (!link) {
        link = document.createElement("link");
        link.rel = "icon";
        document.head.appendChild(link);
    }
    link.href = href;
}

export interface ApplyOptions {
    title?: boolean;
    favicon?: boolean;
}

/**
 * 把站点配置写进 CSS 变量与文档，是"改配置 → 变样式"链路的最后一跳。
 * 已登记的特例走 special 规则，其余字符串项走通用注入（camelCase → --site-kebab-case），
 * 因此新增一个纯值型配置项只需在 site_config 加一行、样式里 var() 引用，不必改这里。
 */
export function applySiteTheme(theme: SiteConfig, options: ApplyOptions = {}) {
    const root = document.documentElement;

    const special: Record<string, () => void> = {
        primaryColor: () => {
            Object.entries(primaryVars(theme.primaryColor)).forEach(([name, value]) => {
                root.style.setProperty(name, value);
            });
        },
        darkMode: () => {
            root.dataset.theme = theme.darkMode ? "dark" : "light";
        },
        siteName: () => {
            if (options.title && theme.siteName) {
                document.title = theme.siteName;
            }
        },
        logo: () => {
            if (options.favicon && theme.logo) {
                setFavicon(theme.logo);
            }
        },
    };

    Object.entries(theme).forEach(([key, value]) => {
        const rule = special[key];
        if (rule) {
            rule();
            return;
        }
        if (typeof value === "string" && value) {
            root.style.setProperty(`--site-${kebab(key)}`, value);
        }
    });
}
