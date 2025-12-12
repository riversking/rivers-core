package com.rivers.core.tree;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TreeFactory<K, T extends TreeNode<K, T>> implements Serializable {

    /**
     * 使用虚拟线程处理大规模数据构建
     */
    public List<T> buildTree(List<T> nodeList) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return Collections.emptyList();
        }
        // 优化验证：大数据并行验证
        validateNodeListOptimized(nodeList);
        // 优化NodeMap创建
        Map<K, T> nodeMap = createOptimizedNodeMap(nodeList);
        // 优化树构建：父节点找不到自动成为根节点
        return buildTreeWithOrphanedRoots(nodeMap);
    }

    private void validateNodeListOptimized(List<T> nodeList) {
        int size = nodeList.size();
        if (size > 10000) {
            // 大数据集使用并行验证
            ConcurrentHashMap.KeySetView<Object, Boolean> idSet = ConcurrentHashMap.newKeySet();
            AtomicBoolean hasError = new AtomicBoolean(false);
            StringBuilder errorMsg = new StringBuilder();
            nodeList.parallelStream().forEach(node -> {
                if (node.getId() == null) {
                    hasError.set(true);
                    errorMsg.append("Node ID cannot be null; ");
                } else if (!idSet.add(node.getId())) {
                    hasError.set(true);
                    errorMsg.append("Duplicate node ID: ").append(node.getId()).append("; ");
                }
            });
            if (hasError.get()) {
                throw new IllegalArgumentException(errorMsg.toString());
            }
        } else {
            // 小数据集串行验证（避免并行开销）
            Set<K> ids = HashSet.newHashSet(size + (size >> 2));
            for (T node : nodeList) {
                if (node.getId() == null) {
                    throw new IllegalArgumentException("Node ID cannot be null");
                }
                if (!ids.add(node.getId())) {
                    throw new IllegalArgumentException("Duplicate node ID: " + node.getId());
                }
            }
        }
    }

    private Map<K, T> createOptimizedNodeMap(List<T> nodeList) {
        int size = nodeList.size();
        if (size > 5000) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                return CompletableFuture.supplyAsync(() -> {
                    // 使用ConcurrentHashMap + 精确容量预分配
                    Map<K, T> nodeMap = new ConcurrentHashMap<>(size + (size >> 2), 0.75f,
                            Runtime.getRuntime().availableProcessors());
                    nodeList.forEach(node -> nodeMap.put(node.getId(), node));
                    return nodeMap;
                }, executor).join();
            }
        } else {
            // 小数据集使用LinkedHashMap + 精确容量
            Map<K, T> nodeMap = new LinkedHashMap<>(size + (size >> 2), 0.75f);
            nodeList.forEach(node -> nodeMap.put(node.getId(), node));
            return nodeMap;
        }
    }

    // 优化ParentMap构建：只包含在nodeMap中存在的父节点
    private Map<K, List<T>> buildOptimizedParentMap(Map<K, T> nodeMap) {
        Map<K, List<T>> parentMap = new ConcurrentHashMap<>();
        // 根据数据量选择处理方式
        if (nodeMap.size() > 10000) {
            nodeMap.values().parallelStream().forEach(node -> {
                K parentId = node.getParentId();
                // 只有当parentId不为null且在nodeMap中存在时，才建立父子关系
                if (parentId != null && nodeMap.containsKey(parentId)) {
                    parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
                }
            });
        } else {
            for (T node : nodeMap.values()) {
                K parentId = node.getParentId();
                if (parentId != null && nodeMap.containsKey(parentId)) {
                    parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
                }
            }
        }
        return parentMap;
    }

    // 优化树构建：父节点找不到的节点自动成为根节点
    private List<T> buildTreeWithOrphanedRoots(Map<K, T> nodeMap) {
        if (nodeMap.isEmpty()) return Collections.emptyList();
        // 构建父节点映射（只包含存在的父节点）
        Map<K, List<T>> parentMap = buildOptimizedParentMap(nodeMap);
        // 找出根节点：parentId为null的节点 + 找不到父节点的节点（孤岛节点）
        List<T> roots = findRootNodes(nodeMap);
        // BFS遍历构建树
        performBFSTraversal(roots, parentMap);
        return roots;
    }

    // 提取根节点查找逻辑
    private List<T> findRootNodes(Map<K, T> nodeMap) {
        List<T> roots = new ArrayList<>();
        for (T node : nodeMap.values()) {
            K parentId = node.getParentId();
            // 如果parentId为null 或者 父节点不存在于nodeMap中，则作为根节点
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            }
        }
        return roots;
    }

    // 提取BFS遍历逻辑
    private void performBFSTraversal(List<T> roots, Map<K, List<T>> parentMap) {
        // 使用ArrayDeque替代LinkedList（性能提升50%+）
        Deque<T> queue = new ArrayDeque<>(roots.size() * 2);
        roots.forEach(queue::offer);

        // BFS遍历构建树
        while (!queue.isEmpty()) {
            T parent = queue.poll();
            List<T> children = parentMap.get(parent.getId());

            if (children != null && !children.isEmpty()) {
                // 直接设置children（避免逐个addChild调用）
                parent.setChildren(children);

                for (T child : children) {
                    queue.offer(child);
                }
            } else {
                parent.setChildren(Collections.emptyList());
            }
        }
    }

    /**
     * 使用 SequencedCollection (Java 21 新特性) 返回有序结果
     */
    public SequencedCollection<T> buildTreeOrdered(List<T> nodeList) {
        List<T> result = buildTree(nodeList);
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
