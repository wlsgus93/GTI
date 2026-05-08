package com.gametrend.insight.domain.insight;

import java.util.List;

/**
 * 4 이해관계자 페르소나 — 룰 ecc/data-analyst/persona.md.
 *
 * <p>각 페르소나마다 톤/포커스/Strategy 형태 다름. PromptBuilder가 system prompt 분기에 활용.
 *
 * <p>온보딩에서 default 페르소나 선택, query param `?persona=INVESTOR`로 일시 override.
 */
public enum Persona {

    INDIE(
            "인디 개발자",
            "개발자님",
            "친근, 개발자 to 개발자",
            List.of("Pain Point", "차별화", "비용 효율"),
            "구체 메카닉 / 출시 시기 / 마케팅 채널"),

    PUBLISHER(
            "퍼블리셔/기획",
            "기획자님",
            "전략적, 포트폴리오 관점",
            List.of("흥행도", "기술 적합성", "포화도"),
            "포트폴리오 구성 / 기술 선택 / 출시 윈도우"),

    MARKETER(
            "마케터/UA",
            "마케터님",
            "ROI 중심, 데이터 위주",
            List.of("CTR/CVR/CPV", "인플루언서 효율", "시청→플레이 전환"),
            "채널 우선순위 / 예산 배분 / 캠페인 시점"),

    INVESTOR(
            "투자자",
            "투자자님",
            "정밀, 리스크 명시",
            List.of("ROI", "BEP", "profit probability", "시장 점유율"),
            "투자/보류/관망 + 리스크 등급");

    private final String label;
    private final String honorific;
    private final String tone;
    private final List<String> focusKpis;
    private final String strategyType;

    Persona(String label, String honorific, String tone, List<String> focusKpis, String strategyType) {
        this.label = label;
        this.honorific = honorific;
        this.tone = tone;
        this.focusKpis = focusKpis;
        this.strategyType = strategyType;
    }

    public String label() {
        return label;
    }

    public String honorific() {
        return honorific;
    }

    public String tone() {
        return tone;
    }

    public List<String> focusKpis() {
        return focusKpis;
    }

    public String strategyType() {
        return strategyType;
    }

    /**
     * 시스템 default 페르소나 — 미인증 사용자 또는 페르소나 미설정 시 fallback.
     *
     * <p>4 페르소나는 동등 1급 시민. 이 default는 검은토끼흰토끼 시드 도메인 컨텍스트
     * (인디 스튜디오 사내 첫 사용처) 때문이지, **제품 타깃 한정 의미 X**.
     * 회원가입/온보딩에서 사용자가 선택한 페르소나가 항상 우선.
     *
     * <p>관련 룰: `.cursor/rules/00-project-overview.mdc` (4 페르소나 동등),
     * `.cursor/rules/90-data-analyst-persona.mdc` §3 (페르소나 동등 원칙).
     */
    public static final Persona DEFAULT = INDIE;
}
