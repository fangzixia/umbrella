package com.runa.smarter.umbrella.runtime.agent;

/** HTTP 对话历史中的一轮。 */
public record SessionTurn(String role, String content) {}
