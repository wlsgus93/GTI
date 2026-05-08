package com.gametrend.insight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.insight.application.agent.IntentClassifier;
import com.gametrend.insight.infrastructure.agent.HardcodedSmallTalkFilter;
import com.gametrend.insight.infrastructure.agent.LocalIntentClassifier;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 3-Layer Agent 빈 와이어링 — Local Intent Classifier (Layer 1).
 *
 * <p>{@code gti.agent.classifier=local} → {@link LocalIntentClassifier} (Ollama 로컬 — Phase 1).
 * {@code gti.agent.classifier=hardcoded} (default) → 정규식 + conservative pass (Ollama 미설치 환경).
 *
 * <p>Local 모드는 LLM provider (cloud)와 별개로 분류기 전용 Ollama URL ({@code gti.agent.classifier.url})
 * 을 사용. 따라서 cloud LLM은 Gemini/Groq 라우터로 두고 분류는 로컬에서 수행 가능.
 */
@Configuration
public class AgentConfig {

    private static final int CLASSIFIER_TIMEOUT_MS = 30_000;
    private static final int CLASSIFIER_MAX_BYTES = 1 * 1024 * 1024;

    /** 로컬 분류기 전용 WebClient — 메인 LLM provider 와 무관. */
    @Bean
    @ConditionalOnProperty(name = "gti.agent.classifier", havingValue = "local")
    public WebClient classifierWebClient(WebClient.Builder builder,
            @Value("${gti.agent.classifier.url:http://localhost:11434}") String url) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CLASSIFIER_TIMEOUT_MS)
                .responseTimeout(Duration.ofMillis(CLASSIFIER_TIMEOUT_MS))
                .doOnConnected(c -> c
                        .addHandlerLast(new ReadTimeoutHandler(CLASSIFIER_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(CLASSIFIER_TIMEOUT_MS, TimeUnit.MILLISECONDS)));
        return builder.clone()
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(CLASSIFIER_MAX_BYTES))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "gti.agent.classifier", havingValue = "local")
    @ConditionalOnMissingBean(IntentClassifier.class)
    public IntentClassifier localIntentClassifier(
            WebClient classifierWebClient,
            @Value("${gti.agent.classifier.model:llama3.2:1b-instruct-q4_K_M}") String model,
            ObjectMapper objectMapper) {
        return new LocalIntentClassifier(classifierWebClient, model, objectMapper);
    }

    /**
     * Hardcoded fallback — Ollama 없거나 명시 비활성 시 (default).
     * 정규식/키워드만으로 분류. UNCLEAR 시 conservative GAME PASS (cloud 호출 허용).
     */
    @Bean
    @ConditionalOnProperty(name = "gti.agent.classifier", havingValue = "hardcoded", matchIfMissing = true)
    @ConditionalOnMissingBean(IntentClassifier.class)
    public IntentClassifier hardcodedIntentClassifier() {
        return query -> {
            if (HardcodedSmallTalkFilter.isSmallTalk(query)) {
                return new IntentClassifier.Classification(
                        IntentClassifier.Topic.SMALL_TALK, IntentClassifier.Intent.UNCLEAR,
                        1.0, "hardcoded smalltalk");
            }
            if (HardcodedSmallTalkFilter.isMeta(query)) {
                return new IntentClassifier.Classification(
                        IntentClassifier.Topic.GAME, IntentClassifier.Intent.META,
                        1.0, "hardcoded meta");
            }
            return new IntentClassifier.Classification(
                    IntentClassifier.Topic.GAME, IntentClassifier.Intent.NEW_QUERY,
                    0.5, "hardcoded conservative pass");
        };
    }
}
