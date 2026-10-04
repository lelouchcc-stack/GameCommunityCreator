package com.lelcc.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 逻辑图校验器：图是数据，必须挡住"图上写出不存在的能力/端口/引用"。
 * 与设计文档 validation.logicLayer 对应，逐项：
 *   1 结构与尺寸：nodes 必须是非空数组，节点数/连线数/JSON 长度有上限
 *   2 节点：typeCode 必须在原子库内；typeCode=preset 时 presetCode+version 必须存在
 *   3 端口：每条边的 from/to 端口都存在，且类型匹配（相等或目标为 any）
 *   4 config：键必须是"输入端口名"或"声明的设置项"；$ref 指向的节点与端口必须存在；
 *      $param 必须在预设的 inputs_schema 内
 *   5 exports：必须指向存在的 节点.端口
 *   6 无环：整图不允许回环（本期不做循环，迭代交给列表组件）
 *
 * 端口约定与原子种子一致：触发原子没有输入、输出 out(signal)；其余原子隐式带
 * signal 输入与 out 输出；branch 输出 true/false 两支；数据端口名即类型名。
 */
public final class GraphValidator {

    private static final int MAX_NODES = 100;
    private static final int MAX_EDGES = 300;
    private static final int MAX_JSON = 20000;
    private static final int MAX_CONFIG_KEYS = 40;

    private static final Pattern REF = Pattern.compile("^\\$ref\\.([^.]+)\\.([^.]+)");
    private static final Pattern PARAM = Pattern.compile("^\\$param\\.([A-Za-z0-9_]+)$");

    private GraphValidator() {
    }

    /** 一个节点的端口集合：可连线输入、输出、以及节点自身设置项 */
    public record Ports(Map<String, String> inputs, Map<String, String> outputs, Set<String> configKeys) {
    }

    /** 由原子库一行的 schema 推出端口集合 */
    public static Ports atomPorts(String kind, String typeCode,
                                 List<Map<String, Object>> inputs,
                                 List<Map<String, Object>> outputs,
                                 List<Map<String, Object>> config) {
        Map<String, String> in = new LinkedHashMap<>();
        if (!"trigger".equals(kind)) {
            in.put("signal", "signal");
        }
        for (Map<String, Object> port : inputs) {
            in.put(String.valueOf(port.get("key")), String.valueOf(port.get("type")));
        }
        Map<String, String> out = new LinkedHashMap<>();
        if ("branch".equals(typeCode)) {
            out.put("true", "signal");
            out.put("false", "signal");
        } else {
            for (Map<String, Object> port : outputs) {
                String name = String.valueOf(port.get("key"));
                // "signal" 输出统一叫 out，与触发/普通原子一致
                if ("signal".equals(name)) {
                    continue;
                }
                out.put(name, String.valueOf(port.get("type")));
            }
            out.put("out", "signal");
        }
        Set<String> configKeys = new LinkedHashSet<>();
        for (Map<String, Object> item : config) {
            configKeys.add(String.valueOf(item.get("key")));
        }
        return new Ports(in, out, configKeys);
    }

    /** 由预设的入参/出参声明推出端口集合（预设节点没有自己的设置项，设置项就是它的入参） */
    public static Ports presetPorts(List<Map<String, Object>> inputs, List<Map<String, Object>> outputs) {
        Map<String, String> in = new LinkedHashMap<>();
        in.put("signal", "signal");
        Set<String> configKeys = new LinkedHashSet<>();
        for (Map<String, Object> port : inputs) {
            String key = String.valueOf(port.get("key"));
            in.put(key, String.valueOf(port.get("type")));
            configKeys.add(key);
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> port : outputs) {
            out.put(String.valueOf(port.get("key")), String.valueOf(port.get("type")));
        }
        out.put("out", "signal");
        return new Ports(in, out, configKeys);
    }

    /** 预设节点在 portsByKey 里的键 */
    public static String presetKey(String presetCode, Object presetVersion) {
        return "preset:" + presetCode + "@" + presetVersion;
    }

    /**
     * @param graph           节点的 graph_json（nodes/edges/exports）
     * @param portsByKey      原子 typeCode -> 端口；预设用 {@link #presetKey} 作键
     * @param declaredParams  本图若是预设，其 inputs_schema 的 key（非预设传 null）
     * @param declaredOutputs 本图若是预设，其 outputs_schema 的 出参名 -> 类型（非预设传 null），
     *                        用于核对 exports 指向的端口类型与声明一致
     * @return 通过返回 null，否则返回可直接展示的中文提示
     */
    public static String validate(Map<String, Object> graph, Map<String, Ports> portsByKey,
                                 List<String> declaredParams, Map<String, String> declaredOutputs) {
        if (graph == null || graph.isEmpty()) {
            return "逻辑图不能为空";
        }
        if (UiJsonUtils.toJson(graph).length() > MAX_JSON) {
            return "逻辑图过大，超过 " + MAX_JSON + " 个字符";
        }
        Object nodesRaw = graph.get("nodes");
        if (!(nodesRaw instanceof List<?> nodeList)) {
            return "逻辑图缺少 nodes 数组";
        }
        if (nodeList.isEmpty()) {
            return "逻辑图至少要有一个节点";
        }
        if (nodeList.size() > MAX_NODES) {
            return "逻辑图节点数不能超过 " + MAX_NODES + " 个";
        }

        Map<String, Ports> portsByNodeId = new LinkedHashMap<>();
        for (Object item : nodeList) {
            if (!(item instanceof Map<?, ?> node)) {
                return "逻辑图的节点必须是对象";
            }
            String id = text(node.get("id"));
            if (id == null) {
                return "逻辑图存在缺少 id 的节点";
            }
            String typeCode = text(node.get("typeCode"));
            if (typeCode == null) {
                return "节点 " + id + " 缺少 typeCode";
            }
            String key = "preset".equals(typeCode)
                    ? presetKey(text(node.get("presetCode")), node.get("presetVersion"))
                    : typeCode;
            Ports ports = portsByKey.get(key);
            if (ports == null) {
                return "preset".equals(typeCode)
                        ? "节点 " + id + " 引用的预设不存在或版本无效：" + text(node.get("presetCode"))
                                + "@" + node.get("presetVersion")
                        : "节点 " + id + " 的原子不存在：" + typeCode;
            }
            if (portsByNodeId.put(id, ports) != null) {
                return "逻辑图节点 id 重复：" + id;
            }
        }

        Object edgesRaw = graph.get("edges");
        List<?> edgeList = edgesRaw instanceof List<?> list ? list : List.of();
        if (edgeList.size() > MAX_EDGES) {
            return "逻辑图连线不能超过 " + MAX_EDGES + " 条";
        }
        for (Object item : edgeList) {
            if (!(item instanceof Map<?, ?> edge)) {
                return "逻辑图的连线必须是对象";
            }
            String fromNode = text(nested(edge, "from", "node"));
            String fromPort = text(nested(edge, "from", "port"));
            String toNode = text(nested(edge, "to", "node"));
            String toPort = text(nested(edge, "to", "port"));
            Ports from = fromNode == null ? null : portsByNodeId.get(fromNode);
            Ports to = toNode == null ? null : portsByNodeId.get(toNode);
            if (from == null || to == null) {
                return "连线引用了不存在的节点：" + fromNode + " -> " + toNode;
            }
            String fromType = from.outputs().get(fromPort);
            if (fromType == null) {
                return "节点 " + fromNode + " 没有输出端口：" + fromPort;
            }
            String toType = to.inputs().get(toPort);
            if (toType == null) {
                return "节点 " + toNode + " 没有输入端口：" + toPort;
            }
            if (!fromType.equals(toType) && !"any".equals(toType)) {
                return "连线两端类型不匹配：" + fromNode + "." + fromPort + "（" + fromType
                        + "）不能接到 " + toNode + "." + toPort + "（" + toType + "）";
            }
        }

        for (Object item : nodeList) {
            Map<?, ?> node = (Map<?, ?>) item;
            String id = text(node.get("id"));
            Ports ports = portsByNodeId.get(id);
            Object configRaw = node.get("config");
            if (!(configRaw instanceof Map<?, ?> config) || config.isEmpty()) {
                continue;
            }
            if (config.size() > MAX_CONFIG_KEYS) {
                return "节点 " + id + " 的设置项过多";
            }
            for (Map.Entry<?, ?> entry : config.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!ports.inputs().containsKey(key) && !ports.configKeys().contains(key)) {
                    return "节点 " + id + " 的设置项 " + key + " 既不是输入端口也不是声明过的设置项";
                }
                Object value = entry.getValue();
                if (!(value instanceof String raw) || raw.isEmpty()) {
                    continue;
                }
                Matcher ref = REF.matcher(raw);
                if (ref.find()) {
                    Ports target = portsByNodeId.get(ref.group(1));
                    if (target == null) {
                        return "节点 " + id + " 的设置项 " + key + " 引用了不存在的节点：" + ref.group(1);
                    }
                    if (!target.outputs().containsKey(ref.group(2))) {
                        return "节点 " + id + " 的设置项 " + key + " 引用了 " + ref.group(1)
                                + " 上不存在的端口：" + ref.group(2);
                    }
                }
                Matcher param = PARAM.matcher(raw);
                if (param.matches() && declaredParams != null && !declaredParams.contains(param.group(1))) {
                    return "节点 " + id + " 的设置项 " + key + " 用了未声明的入参：" + param.group(1);
                }
            }
        }

        Object exportsRaw = graph.get("exports");
        if (exportsRaw instanceof Map<?, ?> exports) {
            for (Map.Entry<?, ?> entry : exports.entrySet()) {
                String name = String.valueOf(entry.getKey());
                String target = text(entry.getValue());
                if (target == null) {
                    continue;
                }
                int dot = target.indexOf('.');
                String nodeId = dot < 0 ? target : target.substring(0, dot);
                String port = dot < 0 ? null : target.substring(dot + 1);
                Ports ports = portsByNodeId.get(nodeId);
                if (ports == null) {
                    return "出参 " + name + " 指向不存在的节点：" + nodeId;
                }
                String actual = port == null ? null : ports.outputs().get(port);
                if (actual == null) {
                    return "出参 " + name + " 指向 " + nodeId + " 上不存在的端口：" + port;
                }
                // 声明的出参类型必须与导出端口一致，否则预设节点上会挂一个类型不符的端口
                String wanted = declaredOutputs == null ? null : declaredOutputs.get(name);
                if (wanted != null && !wanted.equals(actual) && !"any".equals(actual)) {
                    return "出参 " + name + " 声明为 " + wanted + "，但它导出的 " + nodeId + "." + port
                            + " 是 " + actual;
                }
            }
        }

        String cycle = findCycle(nodeList, edgeList);
        if (cycle != null) {
            return "逻辑图存在环：" + cycle;
        }
        return null;
    }

    /** 整图找环（含数据边）：返回环路径，没有环返回 null */
    private static String findCycle(List<?> nodeList, List<?> edgeList) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (Object item : nodeList) {
            adjacency.put(text(((Map<?, ?>) item).get("id")), new ArrayList<>());
        }
        for (Object item : edgeList) {
            Map<?, ?> edge = (Map<?, ?>) item;
            String from = text(nested(edge, "from", "node"));
            String to = text(nested(edge, "to", "node"));
            List<String> next = adjacency.get(from);
            if (next != null && to != null) {
                next.add(to);
            }
        }
        Set<String> visiting = new HashSet<>();
        Set<String> done = new HashSet<>();
        for (String id : adjacency.keySet()) {
            String cycle = dfs(id, adjacency, visiting, done, new ArrayList<>());
            if (cycle != null) {
                return cycle;
            }
        }
        return null;
    }

    private static String dfs(String id, Map<String, List<String>> adjacency,
                              Set<String> visiting, Set<String> done, List<String> stack) {
        if (done.contains(id)) {
            return null;
        }
        if (!visiting.add(id)) {
            List<String> path = new ArrayList<>(stack);
            path.add(id);
            return String.join(" -> ", path);
        }
        stack.add(id);
        for (String next : adjacency.getOrDefault(id, List.of())) {
            String cycle = dfs(next, adjacency, visiting, done, stack);
            if (cycle != null) {
                return cycle;
            }
        }
        stack.remove(stack.size() - 1);
        visiting.remove(id);
        done.add(id);
        return null;
    }

    private static Object nested(Map<?, ?> source, String outer, String inner) {
        Object value = source.get(outer);
        return value instanceof Map<?, ?> map ? map.get(inner) : null;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
