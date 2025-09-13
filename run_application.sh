#!/bin/bash
echo "Balance Master 애플리케이션을 시작합니다..."

# Java 17 경로 설정 (macOS에서 일반적인 경로들)
JAVA_PATHS=(
    "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
    "/usr/libexec/java_home -v 17"
    "/opt/homebrew/opt/openjdk@17"
    "/usr/local/opt/openjdk@17"
)

# Java 17 찾기
JAVA_HOME=""
for path in "${JAVA_PATHS[@]}"; do
    if [[ "$path" == *"java_home"* ]]; then
        JAVA_HOME=$(eval "$path" 2>/dev/null)
    else
        if [[ -d "$path" ]]; then
            JAVA_HOME="$path"
        fi
    fi
    
    if [[ -n "$JAVA_HOME" && -f "$JAVA_HOME/bin/java" ]]; then
        break
    fi
done

if [[ -z "$JAVA_HOME" ]]; then
    echo "Java 17를 찾을 수 없습니다. 다음 중 하나를 설치해주세요:"
    echo "1. Oracle JDK 17"
    echo "2. OpenJDK 17 (Homebrew: brew install openjdk@17)"
    echo "3. AdoptOpenJDK 17"
    exit 1
fi

echo "Java 17 경로: $JAVA_HOME"
export JAVA_HOME

cd "$(dirname "$0")"

echo "애플리케이션 빌드 중..."
"$JAVA_HOME/bin/java" -version
./gradlew bootJar -x test

if [[ $? -ne 0 ]]; then
    echo "빌드 실패!"
    exit 1
fi

echo "애플리케이션 실행 중..."
"$JAVA_HOME/bin/java" -jar build/libs/BalanceStage-0.0.1-SNAPSHOT.jar
