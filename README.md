# Heagent

## How to Build (Maven)
```bash
./mvnw clean package
```

## How to Build (Gradle)
```bash
./gradlew clean shadowJar
```

## How to Use
### Standard launch
```bash
java \
-javaagent:/agent/jar/file/path/heagent-1.0.0.jar \
-jar /main/jar/file/path/main.jar
```

### Launch with parameters

The order of priority between parameters is as follows.
1. agentArgs (Highest)
2. jvm-system-props
3. OS_ENV_VARS

The parameters that can be set are as follows.
* heagent-hostname
* heagent-port
* heagent-path
* heagent-max-used-heap-size
* heagent-max-used-heap-ratio
* heagent-min-available-heap-size
* heagent-min-available-heap-ratio

```bash
export HEAGENT_PATH=/env-var-test

java \
-Dheagent-path=/system-props-test \
-Dheagent-max-used-heap-size=500000000 \
-Dheagent-min-available-heap-ratio=0.2 \
-javaagent:/agent/jar/file/path/heagent-1.0.0.jar=heagentPort=8889,heagentPath=/metrics/health \
-jar /main/jar/file/path/main.jar
```

And, Requesting `suspend={1|0}` for an endpoint will force the health check state to switch.

* `http://127.0.0.1/metrics/health?suspend=1` .. Health check status to Unhealthy.
* `http://127.0.0.1/metrics/health?suspend=0` .. Returning health check decisions to dependence on metrics results.
