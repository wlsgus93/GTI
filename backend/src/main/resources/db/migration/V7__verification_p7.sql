-- V7: P7 검증 모듈 ★ — 4 케이스 (C1~C4) + 자극물 + 캠페인 + 메트릭 시계열.
-- W4 Day 1 — Pretotyping 가설 검증 백엔드.

-- 1) 검증 케이스 마스터
CREATE TABLE verification_case (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(10)  NOT NULL UNIQUE,                 -- 'C1', 'C2', 'C3', 'C4'
    title           VARCHAR(255) NOT NULL,
    concept         TEXT         NOT NULL,
    hypothesis      TEXT         NOT NULL,
    target_persona  TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNING'
        CHECK (status IN ('PLANNING', 'RUNNING', 'ANALYZING', 'DONE')),
    is_priority     BOOLEAN      NOT NULL DEFAULT FALSE,           -- C4가 ★ 핵심 슬라이드 대상
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 2) 자극물 (영상/랜딩/광고/클립 등)
CREATE TABLE stimulus (
    id           BIGSERIAL PRIMARY KEY,
    case_id      BIGINT       NOT NULL REFERENCES verification_case(id) ON DELETE CASCADE,
    type         VARCHAR(20)  NOT NULL
        CHECK (type IN ('VIDEO', 'LANDING', 'TWITCH_CLIP', 'META_AD', 'X_AD', 'COMMUNITY_SEED')),
    title        VARCHAR(255) NOT NULL,
    url          VARCHAR(500),
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_stimulus_case ON stimulus(case_id);

-- 3) 캠페인 (마케팅 채널별 가동)
CREATE TABLE campaign (
    id              BIGSERIAL PRIMARY KEY,
    case_id         BIGINT       NOT NULL REFERENCES verification_case(id) ON DELETE CASCADE,
    stimulus_id     BIGINT       REFERENCES stimulus(id) ON DELETE SET NULL,
    platform        VARCHAR(20)  NOT NULL
        CHECK (platform IN ('META', 'X', 'TWITCH', 'YOUTUBE', 'COMMUNITY_KR', 'OTHER')),
    name            VARCHAR(255) NOT NULL,
    utm_campaign    VARCHAR(100),                                  -- 4 케이스 충돌 방지 UTM
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'RUNNING', 'PAUSED', 'COMPLETED')),
    started_at      TIMESTAMPTZ,
    ended_at        TIMESTAMPTZ,
    budget_cents    BIGINT       NOT NULL DEFAULT 0,
    spent_cents     BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_campaign_case ON campaign(case_id);
CREATE INDEX idx_campaign_status ON campaign(status);

-- 4) 캠페인 메트릭 시계열 (impressions/clicks/conversions/spent)
CREATE TABLE campaign_metric (
    id            BIGSERIAL PRIMARY KEY,
    campaign_id   BIGINT      NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    captured_at   TIMESTAMPTZ NOT NULL,
    impressions   BIGINT      NOT NULL DEFAULT 0,
    clicks        BIGINT      NOT NULL DEFAULT 0,
    conversions   BIGINT      NOT NULL DEFAULT 0,
    spent_cents   BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_campaign_metric_campaign_time ON campaign_metric(campaign_id, captured_at DESC);

-- ===== 시드 (4 케이스 + 각 자극물 1개 + sample 캠페인 + 메트릭) =====

INSERT INTO verification_case (code, title, concept, hypothesis, target_persona, status, is_priority) VALUES
    ('C1', '웹캠 표정 인식 호러',
     '사용자 웹캠으로 표정 인식, 무서워할 때 게임이 더 무섭게 반응하는 호러 게임',
     '25~34세 글로벌 스트리머는 표정 인식 호러 컨셉에 강하게 반응할 것이다 (시청자 반응이 컨텐츠 가치를 결정).',
     '25-34세 영어권 Twitch/YouTube 호러 스트리머',
     'RUNNING', FALSE),
    ('C2', '음성 외침 격투/액션',
     '마이크 음성 인식으로 외쳐서 필살기를 발동하는 격투/액션 게임',
     '한국 20대 남성은 ''외쳐서 격투''라는 페이즈/유머 코드에 강하게 반응할 것이다.',
     '한국 20-29세 남성, 디시·인벤 활성, 격투/액션 경험',
     'RUNNING', FALSE),
    ('C3', 'LLM NPC 스토리',
     'LLM 기반 NPC와 스크립트 없는 진짜 대화로 진행하는 스토리 게임',
     '영어권 30대 인디 PC 게이머는 ''AI NPC 대화'' 컨셉의 새로움에 위시리스트로 반응할 것이다.',
     '영어권 30-39세 Steam 인디 활성, 스토리/RPG 선호',
     'PLANNING', FALSE),
    ('C4', '시선 추적 퍼즐 (접근성)',
     '시선 추적만으로 풀 수 있는 퍼즐 게임. 손/마우스 없이 플레이 가능',
     '저시력/접근성 사용자는 ''시선만으로 게임 가능''에 강력한 가치를 발견할 것이다.',
     '접근성 / 저시력 / 운동 장애 사용자 + 접근성 옹호자',
     'RUNNING', TRUE);   -- ★ 핵심 슬라이드 대상

-- 각 케이스에 자극물 1개씩
INSERT INTO stimulus (case_id, type, title, url, description)
SELECT id, 'VIDEO', '30초 트레일러 — 표정 인식 호러', 'https://example.invalid/c1-trailer.mp4',
       '스트리머 표정 + 게임 반응 시뮬레이션' FROM verification_case WHERE code = 'C1';
INSERT INTO stimulus (case_id, type, title, url, description)
SELECT id, 'META_AD', '메타 광고 — 외쳐서 격투', 'https://example.invalid/c2-meta-ad',
       '격투 + 외침 임팩트 카피' FROM verification_case WHERE code = 'C2';
INSERT INTO stimulus (case_id, type, title, url, description)
SELECT id, 'X_AD', 'X 광고 — AI NPC 데모', 'https://example.invalid/c3-x-ad',
       'AI NPC 대화 짧은 클립' FROM verification_case WHERE code = 'C3';
INSERT INTO stimulus (case_id, type, title, url, description)
SELECT id, 'LANDING', '시선 추적 데모 랜딩', 'https://example.invalid/c4-landing',
       '접근성 옹호자/저시력 사용자 신청 폼' FROM verification_case WHERE code = 'C4';

-- 각 케이스에 sample 캠페인 (RUNNING 케이스만 — C1, C2, C4)
INSERT INTO campaign (case_id, stimulus_id, platform, name, utm_campaign, status, started_at, budget_cents, spent_cents)
SELECT vc.id, s.id, 'TWITCH', 'C1 Twitch 스트리머 픽업', 'gti_c1_twitch_2026q2', 'RUNNING',
       now() - interval '7 days', 100000, 35000
FROM verification_case vc JOIN stimulus s ON s.case_id = vc.id
WHERE vc.code = 'C1';

INSERT INTO campaign (case_id, stimulus_id, platform, name, utm_campaign, status, started_at, budget_cents, spent_cents)
SELECT vc.id, s.id, 'META', 'C2 메타 광고 한국 20대 남성', 'gti_c2_meta_2026q2', 'RUNNING',
       now() - interval '5 days', 200000, 92000
FROM verification_case vc JOIN stimulus s ON s.case_id = vc.id
WHERE vc.code = 'C2';

INSERT INTO campaign (case_id, stimulus_id, platform, name, utm_campaign, status, started_at, budget_cents, spent_cents)
SELECT vc.id, s.id, 'COMMUNITY_KR', 'C4 접근성 커뮤니티 시드', 'gti_c4_a11y_2026q2', 'RUNNING',
       now() - interval '3 days', 50000, 12000
FROM verification_case vc JOIN stimulus s ON s.case_id = vc.id
WHERE vc.code = 'C4';

-- 메트릭 2 시점 (어제 + 오늘)
INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '24 hours',  18000, 540, 32, 18000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C1';
INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '1 hour', 35000, 1125, 78, 35000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C1';

INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '24 hours', 80000, 1850, 140, 50000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C2';
INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '1 hour', 165000, 4060, 365, 92000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C2';

INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '24 hours', 5500, 220, 18, 6000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C4';
INSERT INTO campaign_metric (campaign_id, captured_at, impressions, clicks, conversions, spent_cents)
SELECT c.id, now() - interval '1 hour', 11200, 495, 47, 12000
FROM campaign c JOIN verification_case vc ON c.case_id = vc.id WHERE vc.code = 'C4';

ANALYZE verification_case;
ANALYZE stimulus;
ANALYZE campaign;
ANALYZE campaign_metric;
