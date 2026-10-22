package com.runa.smarter.umbrella.graph.spi;

import com.runa.smarter.umbrella.graph.GraphEditor;
import java.util.Optional;

/**
 * 图扩展 SPI：由 {@code umbrella-integration} 等模块通过 {@code META-INF/services} 注册，
 * 在 {@link GraphContributors#loadAndApply} 时向可变图写入节点与边。
 */
public interface GraphContributor {

    /** 若本贡献者声明图谱名称则返回非空；否则由 {@link GraphContributors#resolveGraphName} 使用默认名称。 */
    default Optional<String> graphName() {
        return Optional.empty();
    }

    /** 向 {@code graph} 追加或填充内容（通常在同一 {@link GraphEditor} 实例上操作）。 */
    void contribute(GraphEditor graph);

}
