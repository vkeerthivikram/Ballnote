param([Parameter(ValueFromRemainingArguments=$true)][string[]]$CommandArgs)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$env:DO_NOT_TRACK = '1'
$env:HYPERFRAMES_FFMPEG_PATH = Join-Path $PSScriptRoot 'node_modules\ffmpeg-static\ffmpeg.exe'
$env:HYPERFRAMES_FFPROBE_PATH = Join-Path $PSScriptRoot 'node_modules\ffprobe-static\bin\win32\x64\ffprobe.exe'
if (Test-Path 'C:\Program Files\Google\Chrome\Application\chrome.exe') { $env:HYPERFRAMES_BROWSER_PATH = 'C:\Program Files\Google\Chrome\Application\chrome.exe' }
& .\node_modules\.bin\hyperframes.cmd @CommandArgs
exit $LASTEXITCODE
