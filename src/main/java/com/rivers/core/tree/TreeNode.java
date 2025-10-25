package com.rivers.core.tree;

import java.util.List;

public interface TreeNode<K, T extends TreeNode<K, T>> {

    K getId();

    K getPid();

    List<T> getChildren();

    void addChild(T child);

}
