package com.rivers.core.tree;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class TreeFactory<K, T extends TreeNode<K, T>> implements Serializable {

    /**
     * 使用虚拟线程处理大规模数据构建
     */
    public List<T> buildTree(List<T> nodeList) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return Lists.newArrayList();
        }
        // 参数验证
        validateNodeList(nodeList);
        // 根据数据量决定处理策略
        Map<K, T> nodeMap = createNodeMap(nodeList);
        return buildTreeIteratively(nodeMap);
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
        // 对于大规模数据，考虑使用虚拟线程
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

    private K tryAttachNode(T node, Map<K, T> nodeMap, Set<K> processedNodes) {
        K parentId = node.getParentId();
        if (parentId == null) {
            return null;
        }

        T parentNode = nodeMap.get(parentId);
        if (parentNode != null && !parentNode.getId().equals(node.getId())) {
            parentNode.addChild(node);
            processedNodes.add(node.getId());
            return node.getId();
        }
        return null;
    }

    /**
     * 使用迭代方式构建树形结构，避免递归栈溢出
     */
    public List<T> buildTreeIteratively(Map<K, T> nodeMap) {
        if (MapUtils.isEmpty(nodeMap)) {
            return Lists.newArrayList();
        }
        Set<K> processedNodes = ConcurrentHashMap.newKeySet();
        boolean hasChanges;
        do {
            hasChanges = false;
            // 根据数据量决定是否使用并行流
            Stream<T> toRemove = nodeMap.size() > 1000 ?
                    nodeMap.values().parallelStream() :
                    nodeMap.values().stream();
            List<K> removedIds = toRemove
                    .filter(node -> !processedNodes.contains(node.getId()))
                    .map(node -> tryAttachNode(node, nodeMap, processedNodes))
                    .filter(Objects::nonNull)
                    .toList();
            if (!removedIds.isEmpty()) {
                removedIds.forEach(nodeMap::remove);
                hasChanges = true;
            }
        } while (hasChanges);
        return Lists.newArrayList(nodeMap.values());
    }

    /**
     * 使用 SequencedCollection (Java 21 新特性) 返回有序结果
     */
    public SequencedCollection<T> buildTreeOrdered(List<T> nodeList) {
        List<T> result = buildTree(nodeList);
        return Collections.unmodifiableSequencedCollection(result);
    }
}
