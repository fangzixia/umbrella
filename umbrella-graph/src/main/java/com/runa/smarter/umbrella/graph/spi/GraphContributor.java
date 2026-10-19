package com.runa.smarter.umbrella.graph.spi;

import com.runa.smarter.umbrella.graph.GraphEditor;

/**
 * 图扩展 SPI：由 {@code umbrella-integration} 等模块通过 {@code META-INF/services} 注册，
 * 在 {@link GraphContributors#loadAndApply} 时向可变图写入节点与边。
 */
public interface GraphContributor {

    /** 向 {@code graph} 追加或填充内容（通常在同一 {@link GraphEditor} 实例上操作）。 */
    void contribute(GraphEditor graph);

}
