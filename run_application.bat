@echo off
echo Balance Master 애플리케이션을 시작합니다...
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
cd /d "%~dp0"

echo 애플리케이션 빌드 중...
"%JAVA_HOME%\bin\java.exe" -version
gradlew.bat bootJar -x test

echo 애플리케이션 실행 중...
"%JAVA_HOME%\bin\java.exe" -jar build\libs\BalanceStage-0.0.1-SNAPSHOT.jar

pause
