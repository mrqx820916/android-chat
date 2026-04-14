param(
    [string]$OutputDir = "dist\update",
    [string]$PublicBaseUrl = "https://cn-nb1.rains3.com/xzd/chat"
)

$ErrorActionPreference = "Stop"

function Get-BuildVersionInfo {
    param([string]$BuildGradlePath)

    $content = Get-Content -Raw -LiteralPath $BuildGradlePath
    $versionCodeMatch = [regex]::Match($content, 'versionCode\s+(\d+)')
    $versionNameMatch = [regex]::Match($content, 'versionName\s+"([^"]+)"')

    if (-not $versionCodeMatch.Success -or -not $versionNameMatch.Success) {
        throw "无法从 $BuildGradlePath 解析 versionCode/versionName"
    }

    return @{
        VersionCode = [int]$versionCodeMatch.Groups[1].Value
        VersionName = $versionNameMatch.Groups[1].Value
    }
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildGradlePath = Join-Path $root "app\build.gradle"
$releaseApkPath = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$targetDir = Join-Path $root $OutputDir

$versionInfo = Get-BuildVersionInfo -BuildGradlePath $buildGradlePath

Write-Host "版本信息: versionCode=$($versionInfo.VersionCode), versionName=$($versionInfo.VersionName)"

if (-not (Test-Path $releaseApkPath)) {
    throw "未找到 release APK: $releaseApkPath`n请先运行: .\gradlew assembleRelease"
}

$channel = "release"
$channelDir = Join-Path $targetDir $channel

if (Test-Path $channelDir) {
    Remove-Item -LiteralPath $channelDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $channelDir | Out-Null

$sha256Output = & certutil -hashfile $releaseApkPath SHA256 2>&1
$sha256 = ($sha256Output | Select-String -Pattern "^[0-9a-fA-F]{64}$").ToString().ToLowerInvariant()
$shortSha = $sha256.Substring(0, 8)
$sizeBytes = (Get-Item -LiteralPath $releaseApkPath).Length
$versionedFileName = "lightchat-v$($versionInfo.VersionName)-$($versionInfo.VersionCode)-$shortSha.apk"

Copy-Item $releaseApkPath (Join-Path $channelDir $versionedFileName) -Force

$apkUrl = "$($PublicBaseUrl.TrimEnd('/'))/$channel/$versionedFileName"

$manifest = [ordered]@{
    channel      = $channel
    packageName  = "com.chat.lightweight"
    versionCode  = $versionInfo.VersionCode
    versionName  = $versionInfo.VersionName
    apkUrl       = $apkUrl
    sha256       = $sha256
    sizeBytes    = $sizeBytes
    releasedAt   = (Get-Date).ToUniversalTime().ToString("o")
    releaseNotes = ""
    force        = $false
}

$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $channelDir "latest.json") -Encoding UTF8

@"
已生成雨云对象存储更新发布目录: $channelDir

文件列表:
- $versionedFileName ($([math]::Round($sizeBytes / 1MB, 1)) MB)
- latest.json

上传步骤:
1. 打开 K:\AICODE\雨云对象存储上传工具\RainyunRosUploader.exe
2. Endpoint: https://cn-nb1.rains3.com
3. Bucket: xzd
4. Prefix: chat/release/
5. 选择并上传 $channelDir 目录中的文件

上传完成后清单地址: $($PublicBaseUrl.TrimEnd('/'))/$channel/latest.json
"@ | Write-Host
