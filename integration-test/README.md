# CrashGuard Integration Test

A standalone Android application that consumes the **published** CrashGuard
artifact instead of the source module. It proves that the generated AAR, its
POM and its transitive dependencies resolve correctly for a third-party app.

## Run it

```bash
# 1. Publish the library to the local Maven repository
cd ..
./gradlew :crash-guard:publishReleasePublicationToMavenLocal

# 2. Build the consumer app
cd integration-test
../gradlew :app:assembleDebug
```

The dependency is declared as:

```gradle
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.roni.library:crash-guard:1.0.0'
}
```

To test the JitPack coordinate instead, replace `mavenLocal()` with
`maven { url 'https://jitpack.io' }` and use:

```gradle
implementation 'com.github.infoadmstudio2020-cyber.crash-guard:crash-guard:1.0.0'
```
