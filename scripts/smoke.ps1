# Smoke test nhanh sau khi `docker compose up -d --build`
# Chay: powershell -ExecutionPolicy Bypass -File scripts/smoke.ps1

$ErrorActionPreference = 'Stop'
$base = if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { 'http://localhost:8080' }
[Console]::OutputEncoding = [Text.Encoding]::UTF8

Write-Host "1) Gateway health"
$core = Invoke-RestMethod "$base/api/health"
Write-Host "   status=$($core.data.status) app=$($core.data.app)"

Write-Host "2) Gateway -> AI health"
$ai = Invoke-RestMethod "$base/api/ai/health"
Write-Host "   status=$($ai.data.status) provider=$($ai.data.provider) chunks=$($ai.data.chunks)"

Write-Host "3) Danh muc"
$events = Invoke-RestMethod "$base/api/public/events"
$tours = Invoke-RestMethod "$base/api/public/tours"
Write-Host "   events=$($events.data.Count) tours=$($tours.data.Count)"

Write-Host "4) Goi y theo so thich"
$recs = Invoke-RestMethod "$base/api/ai/recommendations?limit=4&interests=bi%E1%BB%83n%20%C4%91%E1%BA%A3o"
Write-Host "   recommendations=$($recs.data.Count)"
$recs.data | Select-Object -First 3 type, title, reason | Format-Table -AutoSize

Write-Host "5) Chat RAG (co citation)"
$body = @{ message = 'Tour Phu Quoc co bao gom ve may bay khong?' } | ConvertTo-Json
$chat = Invoke-RestMethod -Method Post -Uri "$base/api/ai/chat" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($body))
Write-Host "   refused=$($chat.data.refused) citations=$($chat.data.citations.Count) provider=$($chat.data.provider)"
Write-Host "   $($chat.data.answer)"

Write-Host "6) Chat ngoai pham vi phai tu choi"
$off = @{ message = 'Hom nay thoi tiet Ha Noi the nao?' } | ConvertTo-Json
$offAnswer = Invoke-RestMethod -Method Post -Uri "$base/api/ai/chat" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($off))
Write-Host "   refused=$($offAnswer.data.refused)"
if (-not $offAnswer.data.refused) {
    throw 'Guardrail khong tu choi cau hoi ngoai pham vi'
}

Write-Host "`nTAT CA KIEM TRA CO BAN DA PASS."
