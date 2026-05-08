/**
 * Infrastructure layer (어댑터).
 *
 * <p>JPA 리포지토리 (persistence), 외부 API 클라이언트 (external/&lt;source&gt;),
 * AI 클라이언트 (ai), Redis 캐시 (cache), Spring Batch 잡 (batch).
 *
 * <p>모든 외부 시스템과의 경계는 여기에 캡슐화. application 레이어는 port 인터페이스로만 접근.
 */
package com.gametrend.insight.infrastructure;
