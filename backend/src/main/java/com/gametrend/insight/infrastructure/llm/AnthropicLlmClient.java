package com.gametrend.insight.infrastructure.llm;

import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;

/**
 * Spring AI ChatClient 래핑 — Anthropic Claude 호출.
 *
 * <p>활성화 조건: {@code spring.ai.anthropic.api-key} 설정. {@link com.gametrend.insight.config.LlmConfig} 참고.
 *
 * <p>토큰 사용량은 {@link Usage}에서 추출 — Anthropic 응답의 input_tokens / output_tokens.
 */
public final class AnthropicLlmClient implements LlmClient {

    private final ChatClient chatClient;
    private final String modelId;

    public AnthropicLlmClient(ChatClient chatClient, String modelId) {
        this.chatClient = chatClient;
        this.modelId = modelId;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();
        int promptTokens = usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens().intValue();
        int completionTokens =
                usage == null || usage.getGenerationTokens() == null ? 0 : usage.getGenerationTokens().intValue();

        return new LlmResponse(content, promptTokens, completionTokens, modelId);
    }
}
