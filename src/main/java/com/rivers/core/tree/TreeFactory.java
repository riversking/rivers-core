package com.rivers.core.tree;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 树构建工厂。
 * <p>
 * 全链路单线程实现：校验、建图、父子映射、BFS 均为 O(n) 纯内存操作。
 * 40k 节点整体耗时应在几十毫秒内；并行流/虚拟线程对这类 CPU 密集且
 * 无阻塞的小任务只会引入线程调度开销，反而拖慢构建。
 */
public class TreeFactory<K, T extends TreeNode<K, T>> implements Serializable {

    /**
     * 构建森林：parentId 为 null 或父节点不存在的节点自动成为根节点。
     */
    public List<T> buildTree(List<T> nodeList) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return Collections.emptyList();
        }
        // 一次遍历完成：ID 校验 + 节点建图（fail-fast）
        Map<K, T> nodeMap = createNodeMap(nodeList);
        // 建父子映射 + 找根 + BFS 组装
        return buildTreeWithOrphanedRoots(nodeMap);
    }

    /**
     * 单次遍历：校验 ID 非空且唯一，同时构建节点映射。
     * 精确预分配容量避免扩容；LinkedHashMap 单线程写性能最优。
     */
    private Map<K, T> createNodeMap(List<T> nodeList) {
        int size = nodeList.size();
        Map<K, T> nodeMap = new LinkedHashMap<>(size + (size >> 2), 0.75f);
        Set<K> seen = HashSet.newHashSet(size + (size >> 2));
        for (T node : nodeList) {
            K id = node.getId();
            if (id == null) {
                throw new IllegalArgumentException("Node ID cannot be null");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate node ID: " + id);
            }
            nodeMap.put(id, node);
        }
        return nodeMap;
    }

    /**
     * 构建父子映射：只包含父节点实际存在的子节点，孤岛节点不进映射。
     */
    private Map<K, List<T>> buildOptimizedParentMap(Map<K, T> nodeMap) {
        Map<K, List<T>> parentMap = HashMap.newHashMap(nodeMap.size());
        for (T node : nodeMap.values()) {
            K parentId = node.getParentId();
            if (parentId != null && nodeMap.containsKey(parentId)) {
                parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
            }
        }
        return parentMap;
    }

    /**
     * 树构建：父节点缺失的节点（孤岛）自动提升为根节点。
     */
    private List<T> buildTreeWithOrphanedRoots(Map<K, T> nodeMap) {
        if (nodeMap.isEmpty()) {
            return Collections.emptyList();
        }
        Map<K, List<T>> parentMap = buildOptimizedParentMap(nodeMap);
        List<T> roots = findRootNodes(nodeMap);
        performBFSTraversal(roots, parentMap);
        return roots;
    }

    /**
     * 根节点：parentId 为 null，或父节点不在当前数据集中。
     */
    private List<T> findRootNodes(Map<K, T> nodeMap) {
        List<T> roots = new ArrayList<>();
        for (T node : nodeMap.values()) {
            K parentId = node.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            }
        }
        return roots;
    }

    /**
     * BFS 组装：一次遍历设置每层 children。
     */
    private void performBFSTraversal(List<T> roots, Map<K, List<T>> parentMap) {
        Deque<T> queue = new ArrayDeque<>(Math.max(roots.size() * 2, 16));
        roots.forEach(queue::offer);
        while (!queue.isEmpty()) {
            T parent = queue.poll();
            List<T> children = parentMap.get(parent.getId());
            if (children != null && !children.isEmpty()) {
                parent.setChildren(children);
                children.forEach(queue::offer);
            } else {
                parent.setChildren(Collections.emptyList());
            }
        }
    }

    /**
     * 返回有序（只读）结果。
     */
    public SequencedCollection<T> buildTreeOrdered(List<T> nodeList) {
        List<T> result = buildTree(nodeList);
        return Collections.unmodifiableSequencedCollection(result);
    }

    /**
     * 构建「子 → 父」映射（节点在数据集中且父节点存在时）。
     */
    public SequencedMap<K, T> buildParentMap(List<T> nodeList) {
        SequencedMap<K, T> parentMap = new LinkedHashMap<>();
        SequencedMap<K, T> nodeMap = new LinkedHashMap<>();
        for (T node : nodeList) {
            nodeMap.put(node.getId(), node);
        }
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

    /**
     * 获取从指定子节点到根节点的所有父节点路径（从根到子顺序）。
     */
    public SequencedCollection<T> findPathToRoot(K childId, SequencedMap<K, T> parentMap) {
        List<T> path = new ArrayList<>();
        T current = parentMap.get(childId);
        while (current != null) {
            path.addFirst(current);
            current = parentMap.get(current.getId());
        }
        return Collections.unmodifiableSequencedCollection(path);
    }

    /**
     * 构建从根节点到指定子节点的完整树形路径。
     */
    public SequencedCollection<T> buildPathTree(K childId, List<T> nodeList) {
        SequencedMap<K, T> parentMap = buildParentMap(nodeList);
        SequencedCollection<T> pathNodes = findPathToRoot(childId, parentMap);
        if (pathNodes.isEmpty()) {
            return Collections.unmodifiableSequencedCollection(new ArrayList<>());
        }
        List<T> tree = new ArrayList<>();
        T rootNode = pathNodes.getFirst();
        tree.add(rootNode);
        List<T> pathList = new ArrayList<>(pathNodes);
        for (int i = 1; i < pathList.size(); i++) {
            T parent = pathList.get(i - 1);
            T child = pathList.get(i);
            parent.getChildren().clear();
            parent.addChild(child);
        }
        return Collections.unmodifiableSequencedCollection(tree);
    }
}