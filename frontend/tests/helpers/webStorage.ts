/**
 * 测试环境 Web Storage 兜底（frontend/tests/helpers/webStorage.ts）。
 *
 * 背景（本机实测）：
 * - Node 25 提供了实验性全局 `localStorage`（webstorage，`--localstorage-file` 未配置有效路径时
 *   只是一个空对象，没有 setItem/getItem/clear 等方法）；
 * - vitest 的 jsdom 环境中 `window === globalThis`，于是 `window.localStorage` 拿到的是这个
 *   不可用的空对象，而不是 jsdom 的 Storage 实现；
 * - 结果：所有依赖 `window.localStorage.clear()/getItem()/setItem()` 的测试会在 beforeEach 就抛
 *   `TypeError: window.localStorage.clear is not a function`。
 *
 * 这里提供与浏览器 Storage 语义一致的内存实现，并在测试前覆盖 `localStorage`。
 * 不修改任何 src / 构建配置；浏览器与 Node 22 及以下环境下该覆盖同样安全（只是替换为内存实现）。
 */

/** 创建符合 Storage 接口的内存实现 */
export function createMemoryStorage(): Storage {
  const map = new Map<string, string>()
  const storage: Storage = {
    get length(): number {
      return map.size
    },
    clear(): void {
      map.clear()
    },
    getItem(key: string): string | null {
      return map.has(key) ? (map.get(key) as string) : null
    },
    key(index: number): string | null {
      const keys = Array.from(map.keys())
      return index >= 0 && index < keys.length ? keys[index] : null
    },
    removeItem(key: string): void {
      map.delete(key)
    },
    setItem(key: string, value: string): void {
      map.set(String(key), String(value))
    },
  }
  return storage
}

/**
 * 用全新的内存 Storage 覆盖全局 localStorage（同时覆盖 window，jsdom 下二者是同一对象）。
 * 返回该 Storage，便于测试直接断言。
 */
export function installMemoryStorage(): Storage {
  const storage = createMemoryStorage()
  try {
    Object.defineProperty(globalThis, 'localStorage', {
      value: storage,
      configurable: true,
      writable: true,
    })
  } catch {
    // 极端环境（属性不可配置）下退化为直接赋值
    ;(globalThis as unknown as { localStorage: Storage }).localStorage = storage
  }
  const globalWindow = globalThis as unknown as { window?: Window }
  if (globalWindow.window && (globalWindow.window as unknown) !== globalThis) {
    try {
      Object.defineProperty(globalWindow.window, 'localStorage', {
        value: storage,
        configurable: true,
        writable: true,
      })
    } catch {
      /* 忽略：jsdom 下 window 通常就是 globalThis，上面已处理 */
    }
  }
  return storage
}
