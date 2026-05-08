package com.gametrend.insight.config;

import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.infrastructure.llm.AnthropicLlmClient;
import com.gametrend.insight.infrastructure.llm.GeminiLlmClient;
import com.gametrend.insight.infrastructure.llm.GeminiProperties;
import com.gametrend.insight.infrastructure.llm.GeminiWebClientConfig;
import com.gametrend.insight.infrastructure.llm.GroqLlmClient;
import com.gametrend.insight.infrastructure.llm.GroqProperties;
import com.gametrend.insight.infrastructure.llm.GroqWebClientConfig;
import com.gametrend.insight.infrastructure.llm.OllamaLlmClient;
import com.gametrend.insight.infrastructure.llm.OllamaProperties;
import com.gametrend.insight.infrastructure.llm.OllamaWebClientConfig;
import com.gametrend.insight.infrastructure.llm.RoutingLlmClient;
import com.gametrend.insight.infrastructure.llm.StubLlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * LlmClient 빈 와이어링 — provider toggle.
 *
 * <p>{@code gti.llm.provider} 값:
 * <ol>
 *   <li>{@code router} → {@link RoutingLlmClient} (Gemini HIGH + Groq FAST 라우팅) ★ 권장
 *   <li>{@code gemini} → {@link GeminiLlmClient} 단독
 *   <li>{@code groq} → {@link GroqLlmClient} 단독
 *   <li>{@code ollama} → {@link OllamaLlmClient} (로컬)
 *   <li>{@code anthropic} → {@link AnthropicLlmClient}
 *   <li>설정 없고 {@code spring.ai.anthropic.api-key} 있으면 자동 Anthropic
 *   <li>그 외 → {@link StubLlmClient} (부팅 가능성 보장)
 * </ol>
 */
@Configuration
public class LlmConfig {

    /** provider=router — Gemini (HIGH) + Groq (FAST) 라우팅. */
    @Bean
    @ConditionalOnProperty(name = "gti.llm.provider", havingValue = "router")
    public LlmClient routingLlmClient(
            @Qualifier(GeminiWebClientConfig.GEMINI_WEB_CLIENT) WebClient geminiWebClient,
            @Qualifier(GroqWebClientConfig.GROQ_WEB_CLIENT) WebClient groqWebClient,
            GeminiProperties geminiProps,
            GroqProperties groqProps) {
        LlmClient gemini = new GeminiLlmClient(geminiWebClient, geminiProps);
        LlmClient groq = new GroqLlmClient(groqWebClient, groqProps);
        return new RoutingLlmClient(gemini, groq);
    }

    /** provider=gemini — Gemini 단독. */
    @Bean
    @ConditionalOnProperty(name = "gti.llm.provider", havingValue = "gemini")
    public LlmClient geminiLlmClient(
            @Qualifier(GeminiWebClientConfig.GEMINI_WEB_CLIENT) WebClient geminiWebClient,
            GeminiProperties props) {
        return new GeminiLlmClient(geminiWebClient, props);
    }

    /** provider=groq — Groq 단독. */
    @Bean
    @ConditionalOnProperty(name = "gti.llm.provider", havingValue = "groq")
    public LlmClient groqLlmClient(
            @Qualifier(GroqWebClientConfig.GROQ_WEB_CLIENT) WebClient groqWebClient,
            GroqProperties props) {
        return new GroqLlmClient(groqWebClient, props);
    }

    /** provider=ollama — 로컬 Ollama. */
    @Bean
    @ConditionalOnProperty(name = "gti.llm.provider", havingValue = "ollama")
    public LlmClient ollamaLlmClient(
            @Qualifier(OllamaWebClientConfig.OLLAMA_WEB_CLIENT) WebClient ollamaWebClient,
            OllamaProperties props) {
        return new OllamaLlmClient(ollamaWebClient, props);
    }

    /** provider=anthropic — Anthropic Claude (Spring AI ChatClient). */
    @Bean
    @ConditionalOnProperty(name = "gti.llm.provider", havingValue = "anthropic")
    public LlmClient anthropicLlmClientByProvider(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.anthropic.chat.options.model:claude-opus-4-5}") String modelId) {
        return new AnthropicLlmClient(chatClientBuilder.build(), modelId);
    }

    /** provider 미설정 + Anthropic key 있으면 자동. */
    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key", matchIfMissing = false)
    public LlmClient anthropicLlmClientFallback(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.anthropic.chat.options.model:claude-opus-4-5}") String modelId) {
        return new AnthropicLlmClient(chatClientBuilder.build(), modelId);
    }

    /** 위 모두 없으면 stub — 부팅 가능성 보장. */
    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    public LlmClient stubLlmClient() {
        return new StubLlmClient();
    }
}
