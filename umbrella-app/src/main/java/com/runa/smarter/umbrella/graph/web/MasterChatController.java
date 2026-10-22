package com.runa.smarter.umbrella.graph.web;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.runtime.agent.MasterAgentChatService;
import com.runa.smarter.umbrella.runtime.agent.NodeAgentChatService;
import com.runa.smarter.umbrella.runtime.agent.NodeAgentChatService.Turn;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** 总控 Agent 对话 HTTP 接口。 */
@RestController
public class MasterChatController {

    private static final Logger log = LoggerFactory.getLogger(MasterChatController.class);
    private static final int MAX_MESSAGE = 8000;

    private final MasterAgentChatService masterChat;

    public MasterChatController(MasterAgentChatService masterChat) {
        this.masterChat = masterChat;
    }

    @PostMapping("/api/graph/master/chat")
    public Mono<ResponseEntity<Reply>> chat(@RequestBody Request request) {
        List<Turn> history = request.history() == null ? List.of() : request.history();
        if (!validText(request.message()) || history.size() > 40 || history.size() % 2 != 0) {
            return failure(HttpStatus.BAD_REQUEST, BusinessErrorMessage.format(
                    "单条消息超过 8000 字、或历史条数不是偶数（超过 20 轮）。",
                    "单条消息控制在 8000 字以内，并保证历史为 user/assistant 成对且不超过 20 轮。"));
        }
        for (int i = 0; i < history.size(); i++) {
            Turn turn = history.get(i);
            if (turn == null || !validText(turn.content())
                    || !(i % 2 == 0 ? "user" : "assistant").equals(turn.role())) {
                return failure(HttpStatus.BAD_REQUEST, BusinessErrorMessage.format(
                        "对话历史中某条消息为空、超长，或角色未按 user/assistant 交替排列。",
                        "请从首轮 user 开始，严格交替 assistant，且每条 content 非空且不超过 8000 字。"));
            }
        }
        return masterChat.chat(request.message().strip(), history)
                .map(text -> ResponseEntity.ok(new Reply("umbrella-master", text, null)))
                .onErrorResume(NodeAgentChatService.ModelUnavailableException.class,
                        error -> failure(HttpStatus.SERVICE_UNAVAILABLE, BusinessErrorMessage.format(
                                "DashScope 对话模型未启用或未配置 api-key。",
                                "在 application.yml 中设置 agentscope.dashscope.enabled=true 并配置 agentscope.dashscope.api-key。")))
                .onErrorResume(TimeoutException.class,
                        error -> failure(HttpStatus.GATEWAY_TIMEOUT, BusinessErrorMessage.format(
                                "总控 Agent 在 90 秒内未返回完整回复。",
                                "请稍后重试。")))
                .onErrorResume(IllegalStateException.class, error -> failure(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("总控对话失败 ({})", error.getClass().getSimpleName());
                    return failure(HttpStatus.BAD_GATEWAY, BusinessErrorMessage.format(
                            "总控 Agent 调用过程中发生未预期错误（" + error.getClass().getSimpleName() + "）。",
                            "请查看服务端日志后重试。"));
                });
    }

    private static boolean validText(String text) {
        return text != null && !text.isBlank() && text.length() <= MAX_MESSAGE;
    }

    private static Mono<ResponseEntity<Reply>> failure(HttpStatus status, String message) {
        return Mono.just(ResponseEntity.status(status).body(new Reply(null, null, message)));
    }

    public record Request(String message, List<Turn> history) {}

    public record Reply(String agentId, String message, String error) {}
}
