package com.runa.smarter.umbrella.graph;

/** 图结构不合法时抛出（重复 id、边端点缺失等）。 */
public class GraphDefinitionException extends RuntimeException {

    public GraphDefinitionException(String message) {
        super(message);
    }

}
