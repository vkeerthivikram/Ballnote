$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$taskJdk = Join-Path $PSScriptRoot '.tools\jdk\jdk-21.0.13+9'
if (Test-Path $taskJdk) { $env:JAVA_HOME = $taskJdk }
if (Test-Path '.tools\sdk') { $env:ANDROID_HOME = (Resolve-Path '.tools\sdk').Path }
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.tools\gradle-home'
if (Test-Path '.\gradlew.bat') { & .\gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain }
else { & .tools\gradle-8.13\bin\gradle.bat testDebugUnitTest assembleDebug lintDebug --console=plain }
exit $LASTEXITCODE
