export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

./gradlew :app:assembleDebug

adb install -r "app/build/outputs/apk/debug/app-debug.apk"