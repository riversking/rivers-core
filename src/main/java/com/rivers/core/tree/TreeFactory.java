package com.rivers.core.tree;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TreeFactory<K, T extends TreeNode<K, T>> implements Serializable {

    /**
     * 使用虚拟线程处理大规模数据构建
     */
    public List<T> buildTree(List<T> nodeList, K parentId) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return Collections.emptyList();
        }
        validateNodeList(nodeList);
        Map<K, T> nodeMap = createNodeMap(nodeList);
        return buildTreeBFS(nodeMap, parentId);
    }

    private void validateNodeList(List<T> nodeList) {
        Set<K> ids = new HashSet<>();
        for (T node : nodeList) {
            if (node.getId() == null) {
                throw new IllegalArgumentException("Node ID cannot be null");
            }
            if (!ids.add(node.getId())) {
                throw new IllegalArgumentException("Duplicate node ID: " + node.getId());
            }
        }
    }

    private Map<K, T> createNodeMap(List<T> nodeList) {
        // Java 21 虚拟线程：5000+ 数据自动用虚拟线程
        if (nodeList.size() > 5000) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                return CompletableFuture.supplyAsync(() -> {
                    Map<K, T> nodeMap = LinkedHashMap.newLinkedHashMap(nodeList.size());
                    nodeList.forEach(node -> nodeMap.put(node.getId(), node));
                    return nodeMap;
                }, executor).join();
            }
        } else {
            Map<K, T> nodeMap = LinkedHashMap.newLinkedHashMap(nodeList.size());
            nodeList.forEach(node -> nodeMap.put(node.getId(), node));
            return nodeMap;
        }
    }

    // ✨ 核心优化：Java 21 的 groupingByConcurrent！
    private List<T> buildTreeBFS(Map<K, T> nodeMap, K parentId) {
        if (nodeMap.isEmpty()) return Collections.emptyList();

        // 构建父节点映射（用 Java 21 并行收集器）
        Map<K, List<T>> parentMap = buildParentMap(nodeMap);
        Queue<T> queue = new LinkedList<>();
        // 找出所有根节点（parentId == null）
        for (T node : nodeMap.values()) {
            if (node.getParentId() == null || Objects.equals(node.getParentId(), parentId)) {
                queue.offer(node);
            }
        }
        // BFS：按层级构建树（O(N) 时间！）
        while (!queue.isEmpty()) {
            T parent = queue.poll();
            List<T> children = parentMap.getOrDefault(parent.getId(), Collections.emptyList());
            for (T child : children) {
                parent.addChild(child); // 添加子节点
                queue.offer(child);     // 子节点入队（处理它们的子节点）
                nodeMap.remove(child.getId()); // 从Map移除，避免重复
            }
        }
        return new ArrayList<>(nodeMap.values()); // 返回根节点列表
    }

    private Map<K, List<T>> buildParentMap(Map<K, T> nodeMap) {
        return nodeMap.values().parallelStream()
                .collect(Collectors.groupingByConcurrent(
                        TreeNode::getParentId, Collectors.toList()
                ));
    }

    /**
     * 使用 SequencedCollection (Java 21 新特性) 返回有序结果
     */
    public SequencedCollection<T> buildTreeOrdered(List<T> nodeList, K parentId) {
        List<T> result = buildTree(nodeList, parentId);
        return Collections.unmodifiableSequencedCollection(result);
    }

    public SequencedMap<K, T> buildParentMap(List<T> nodeList) {
        SequencedMap<K, T> parentMap = new LinkedHashMap<>();
        SequencedMap<K, T> nodeMap = new LinkedHashMap<>();
        // 构建节点映射
        for (T node : nodeList) {
            nodeMap.put(node.getId(), node);
        }
        // 建立父子关系
        for (T node : nodeList) {
            K parentId = node.getParentId();
            if (parentId != null) {
                T parent = nodeMap.get(parentId);
                if (parent != null) {
                    parentMap.put(node.getId(), parent);
                }
            }
        }
        return parentMap;
    }


    // 获取从指定子节点到根节点的所有父节点路径
    // 获取从指定子节点到根节点的所有父节点路径
    public SequencedCollection<T> findPathToRoot(K childId, SequencedMap<K, T> parentMap) {
        List<T> path = new ArrayList<>();
        T current = parentMap.get(childId);
        // 向上追溯到根节点
        while (current != null) {
            path.addFirst(current); // 插入到开头保持从根到子的顺序
            current = parentMap.get(current.getId());
        }
        return Collections.unmodifiableSequencedCollection(path);
    }


    // 构建从根节点到指定子节点的完整树形路径
    public SequencedCollection<T> buildPathTree(K childId, List<T> nodeList) {
        // 构建父子关系映射
        SequencedMap<K, T> parentMap = buildParentMap(nodeList);
        // 获取路径上的所有节点
        SequencedCollection<T> pathNodes = findPathToRoot(childId, parentMap);
        // 如果找不到路径，返回空列表
        if (pathNodes.isEmpty()) {
            return Collections.unmodifiableSequencedCollection(new ArrayList<>());
        }
        // 构建树形结构
        List<T> tree = new ArrayList<>();
        T rootNode = pathNodes instanceof List<?> pathList ? (T) pathList.getFirst() : pathNodes.getFirst();
        tree.add(rootNode);
        // 重建父子关系
        List<T> pathList = new ArrayList<>(pathNodes);
        for (int i = 1; i < pathList.size(); i++) {
            T parent = pathList.get(i - 1);
            T child = pathList.get(i);
            parent.getChildren().clear(); // 清空原有子节点
            parent.addChild(child);
        }
        return Collections.unmodifiableSequencedCollection(tree);
    }


}
