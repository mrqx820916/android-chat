$env:AWS_ACCESS_KEY_ID = "0qrq8ElWaEqxKxYO"
$env:AWS_SECRET_ACCESS_KEY = "urJL74b55YTYbVDz7fCB5I3jkFCh8y"
$env:AWS_DEFAULT_REGION = "rainyun"

Set-Location "K:\AICODE\chat\android-chat"

Write-Host "上传 APK..."
aws --endpoint-url https://cn-nb1.rains3.com s3 cp "dist\update\release\lightchat-v1.3.1-14-1b417fa9.apk" s3://xzd/chat/release/

Write-Host "上传 latest.json..."
aws --endpoint-url https://cn-nb1.rains3.com s3 cp "dist\update\release\latest.json" s3://xzd/chat/release/latest.json --content-type "application/json"

Write-Host "验证上传..."
aws --endpoint-url https://cn-nb1.rains3.com s3 ls s3://xzd/chat/release/
