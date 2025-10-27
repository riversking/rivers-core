package com.rivers.core.tree;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreeFactory<K, T extends TreeNode<K, T>> implements Serializable {

    /**
     * 使用虚拟线程处理大规模数据构建
     */
    public List<T> buildTree(List<T> nodeList) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return new ArrayList<>();
        }
        // 使用 LinkedHashMap 保持插入顺序
        Map<K, T> nodeMap = LinkedHashMap.newLinkedHashMap(nodeList.size());
        nodeList.forEach(node -> nodeMap.put(node.getId(), node));
        return buildTreeIteratively(nodeMap);
    }

    /**
     * 使用迭代方式构建树形结构，避免递归栈溢出
     * 利用 Java 21 的模式匹配和流式处理优化
     */
    public List<T> buildTreeIteratively(Map<K, T> nodeMap) {
        if (MapUtils.isEmpty(nodeMap)) {
            return Lists.newArrayList();
        }
        Set<K> processedNodes = ConcurrentHashMap.newKeySet();
        boolean hasChanges;
        do {
            hasChanges = false;
            // 使用并行流处理大规模数据
            List<K> toRemove = nodeMap.values()
                    .parallelStream()
                    .filter(node -> !processedNodes.contains(node.getId()))
                    .map(node -> {
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
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if (!toRemove.isEmpty()) {
                toRemove.forEach(nodeMap::remove);
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
