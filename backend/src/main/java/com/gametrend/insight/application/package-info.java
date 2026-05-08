/**
 * Application layer.
 *
 * <p>UseCase, application 서비스, 트랜잭션 경계 ({@code @Transactional}).
 * 도메인 객체와 인프라 어댑터를 조율한다. 외부 API/DB 직접 접근 금지 — port 인터페이스를 통해서만.
 *
 * <p>하위 패키지:
 * <ul>
 *   <li>port/in/* — UseCase 인터페이스 (들어오는 요청)
 *   <li>port/out/* — 외부 의존 인터페이스 (나가는 호출)
 *   <li>ingestion, trend, game, compare, watchlist, moneycalc, internal,
 *       verification, publisher, campaign, report, agent
 * </ul>
 */
package com.gametrend.insight.application;
