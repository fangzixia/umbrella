package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.GraphView;

/** 创建可变图的工厂入口。 */
public final class Graphs {

    private Graphs() {
    }

    /** 空图。 */
    public static GraphEditor empty() {
        return new MutableGraph();
    }

    /** 指定名称的空图。 */
    public static GraphEditor empty(String name) {
        return new MutableGraph(name);
    }

    /** 从已有视图复制为新的可变图。 */
    public static GraphEditor from(GraphView view) {
        return new MutableGraph(view);
    }

}
