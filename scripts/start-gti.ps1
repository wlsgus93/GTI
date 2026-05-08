# start-gti.ps1 — GTI 백엔드 풀 스택 부팅 (PowerShell)
#
# 동작 순서:
#   1. Docker (postgres + redis + crawler) 기동
#   2. backend Spring Boot jar 실행 (-Xmx1g)
#   3. cloudflared tunnel run (외부 노출)
#
# 사전 조건:
#   - Docker Desktop 실행 중 (또는 자동 시작 설정)
#   - JAVA_HOME 또는 java 21 PATH
#   - cloudflared 설치 (winget install Cloudflare.cloudflared)
#   - backend\.env 파일에 환경변수 (DB_*, REDIS_*, *_API_KEY, JWT_SECRET, GTI_ALLOWED_ORIGINS, LLM_PROVIDER=router 등)
#   - backend\build\libs\*.jar 빌드되어 있음 (없으면 자동 빌드)
#
# 실행: PowerShell 우클릭 → "관리자 권한으로 실행" 또는
#       powershell -ExecutionPolicy Bypass -File scripts\start-gti.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "[GTI] 1/4 Docker compose up (postgres + redis + crawler)" -ForegroundColor Cyan
Set-Location "$root\backend"
docker compose up -d
if ($LASTEXITCODE -ne 0) { throw "docker compose 실패" }

# DB ready 대기
Write-Host "[GTI] postgres health 대기 (max 60s)" -ForegroundColor Cyan
$timeout = 60
while ($timeout -gt 0) {
    $health = docker inspect --format '{{.State.Health.Status}}' gti-postgres 2>$null
    if ($health -eq "healthy") { Write-Host "  postgres ready" -ForegroundColor Green; break }
    Start-Sleep -Seconds 2
    $timeout -= 2
}

Write-Host "[GTI] 2/4 Spring Boot jar 빌드 확인" -ForegroundColor Cyan
$jar = Get-ChildItem "$root\backend\build\libs\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1
if (-not $jar) {
    Write-Host "  jar 없음 — gradlew bootJar 실행" -ForegroundColor Yellow
    & "$root\backend\gradlew.bat" bootJar
    $jar = Get-ChildItem "$root\backend\build\libs\*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1
}

Write-Host "[GTI] 3/4 Spring Boot 실행 — $($jar.Name)" -ForegroundColor Cyan
$springProcess = Start-Process -FilePath "java" `
    -ArgumentList "-Xmx1g","-Xms256m","-jar",$jar.FullName `
    -WorkingDirectory "$root\backend" `
    -PassThru -NoNewWindow `
    -RedirectStandardOutput "$root\logs\spring-out.log" `
    -RedirectStandardError "$root\logs\spring-err.log"
Write-Host "  Spring PID = $($springProcess.Id)" -ForegroundColor Green

# Spring health 대기
Write-Host "[GTI] Spring health 대기 (max 120s)" -ForegroundColor Cyan
$timeout = 120
while ($timeout -gt 0) {
    try {
        $h = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($h.status -eq "UP") { Write-Host "  Spring UP" -ForegroundColor Green; break }
    } catch { Start-Sleep -Seconds 3; $timeout -= 3 }
}

Write-Host "[GTI] 4/4 Cloudflare Tunnel 실행" -ForegroundColor Cyan
$cloudflaredProcess = Start-Process -FilePath "cloudflared" `
    -ArgumentList "tunnel","run","gti-tunnel" `
    -PassThru -NoNewWindow `
    -RedirectStandardOutput "$root\logs\cloudflared-out.log" `
    -RedirectStandardError "$root\logs\cloudflared-err.log"
Write-Host "  cloudflared PID = $($cloudflaredProcess.Id)" -ForegroundColor Green

Write-Host ""
Write-Host "[GTI] 풀 스택 기동 완료" -ForegroundColor Green
Write-Host "  Backend:    http://localhost:8080/swagger-ui.html"
Write-Host "  Backend:    https://api.gti.your-domain.com/swagger-ui.html  (Cloudflare 경유)"
Write-Host "  Postgres:   localhost:5432 (gti / gti)"
Write-Host "  Redis:      localhost:6379"
Write-Host "  Crawler:    http://localhost:3001"
Write-Host ""
Write-Host "  로그:       $root\logs\"
Write-Host "  종료:       scripts\stop-gti.ps1"
