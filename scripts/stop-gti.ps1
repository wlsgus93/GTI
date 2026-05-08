# stop-gti.ps1 — GTI 백엔드 풀 스택 종료
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "[GTI] cloudflared 종료" -ForegroundColor Cyan
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "[GTI] Spring Boot 종료" -ForegroundColor Cyan
$springPid = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -First 1
if ($springPid) { Stop-Process -Id $springPid -Force }

Write-Host "[GTI] Docker 컨테이너 종료" -ForegroundColor Cyan
Set-Location "$root\backend"
docker compose down

Write-Host "[GTI] 종료 완료" -ForegroundColor Green
