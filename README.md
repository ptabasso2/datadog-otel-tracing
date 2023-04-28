![GitHub](https://img.shields.io/github/license/ptabasso2/datadog-otel-tracing?style=plastic)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/ptabasso2/datadog-otel-tracing)
![GitHub last commit](https://img.shields.io/github/last-commit/ptabasso2/datadog-otel-tracing)


# Instrumenting an application using Datadog java agent and custom tracing based on the OpenTelemetry API


## Introduction


The sections of this tutorial are structured as follows

* Goal
* Pre-requisites
* Clone the repository
* Directory structure of the [project](#project)
* Overview of the [application](#app)
* Building and running the application [locally](#local).
* End



## Goal of this lab


The purpose of this lab is to help familiarizing yourself with using the OpenTelemetry API to perform custom instrumentation activities using the dataod java agent. This is intended to users who wish to benefit from the best of the two worlds as they can leverage some advanced capabilities the Datadog java agent offers (ex Application security, Runtime metrics collection, Continous Profiling, Dynamic instrumentation) while at the same time being able to comply with OpenTelemetry requirements.



## Pre-requisites


+ About 15 minutes
+ A java JDK (If building & running locally). Ex OpenJDK 11 or above
+ Gradle installed (If building & running locally). Ex Gradle 7.5.1
+ Git client
+ A Datadog account with a valid API key
+ Your favorite text editor or IDE (Ex Sublime Text, Atom, vscode...)


## Clone the repository


<pre style="font-size: 12px">
[root@pt-instance-6:~/]$ git clone https://github.com/ptabasso2/datadog-otel-tracing 
[root@pt-instance-6:~/]$ cd datadog-otel-tracing
[root@pt-instance-6:~/datadog-otel-tracing]$ 
</pre>


## Directory structure of the <a name="project"></a>project


The example below is the structure after having clone the project.

```shell
[root@pt-instance-6:~/datadog-otel-tracing]$ tree
.
├── README.md
├── build
│   ├── classes
│   │   └── java
│   │       └── main
│   │           └── com
│   │               └── datadog
│   │                   └── pej
│   │                       ├── OtelCustomInstrum$1.class
│   │                       └── OtelCustomInstrum.class
│   ├── generated
│   │   └── sources
│   │       ├── annotationProcessor
│   │       │   └── java
│   │       │       └── main
│   │       └── headers
│   │           └── java
│   │               └── main
│   ├── libs
│   │   └── datadog-otel-tracing-0.2.0.jar
│   └── tmp
│       ├── compileJava
│       │   └── previous-compilation-data.bin
│       └── shadowJar
│           └── MANIFEST.MF
├── build.gradle
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── settings.gradle
└── src
    └── main
        ├── java
        │   └── com
        │       └── datadog
        │           └── pej
        │               └── OtelCustomInstrum.java
        └── resources

28 directories, 12 files

```

## Overview of the <a name="app"></a>application


The main components of this project can be described as follows:
+ A single java class that will implement various static methods that showcases the use of the OpenTelemetry API to custom instrument java applications
+ Two distinct methods `doSomeStuffAsParent()` and `doSomeStuffAsChild()` that shows how to create spans that have a parent/child dependancy </br>
+ Two other methods `doSomeStuffInjecting()` and `doSomeStuffExtracting()` showing how context propagation can be used through Otel propagators.

The notable parts of this program can be broken down as follows:

### Dependancies

We need the following dependancies to be able to use OpenTelemetry for custom instrumentation. 

The `build.gradle` file can be updated as follows:

```java
dependencies {
    implementation 'io.opentelemetry:opentelemetry-api:1.23.1'
    implementation 'io.opentelemetry:opentelemetry-semconv:1.23.1-alpha'
}
```


### Accessing the tracer

```java
static Tracer tracer = GlobalOpenTelemetry.get().getTracer("instrumentationName");
```


### Example of span creation

```java
Span parentSpan = tracer.spanBuilder("parent").
                setAttribute("span.type", "web").setAttribute("resource.name", "GET /parent").
                startSpan();

        try (Scope scope = parentSpan.makeCurrent()) {
            doSomeStuffAsChild();
        } finally {
            parentSpan.end();
        }
```


### Example of context propagation 

Injecting the context

```java
Span spaninject = tracer.spanBuilder("inject").
                setAttribute("span.type", "web").setAttribute("resource.name", "GET /inject").
                startSpan();

        try (Scope scope = spaninject.makeCurrent()) {
            Map<String, String> headers = new HashMap<>();

            W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, (carrier, key, value) -> headers.put(key, value));

            System.out.println("After filling the headers");
            doSomeStuffExtracting(headers);


        } finally {
            spaninject.end();
        }
```



Extracting the context

```java
Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> extractedheader) {
                return headers.values();
            }

            @Override
            public String get(Map<String, String> extractedheader, String key) {
                return headers.get(key);
            }
        });

        System.out.println("After extracting headers");

        try (Scope scope = extractedContext.makeCurrent()) {
            Span spanextract = tracer.spanBuilder("extract").
                    setAttribute("span.type", "web").setAttribute("resource.name", "GET /extract").
                    startSpan();
            try {
                System.out.println("Doing stuff");
            } finally {
                spanextract.end();
            }
        }
```


## Building <a name="local"></a> the application and running it locally.

These steps assume that you have a JDK installed and configured for your environment. This tutorial has been tested with `OpenJDK 11.0.12`.
And you will also need to have gradle installed, the version used in this example is `7.5.1`


### Starting the Datadog Agent first ###

First set your API Key:

````shell
[root@pt-instance-6:~/datadog-otel-tracing]$ export DD_API_KEY=<Your api key>
````

Then let's run the agent. As docker is installed on our environment, we will use a dockerized version of the agent.

But if you wish to have it deployed as a standalone service you will want to follow the instructions as per [Datadog Agent installation](https://app.datadoghq.com/account/settings?_gl=1*17qq65s*_gcl_aw*R0NMLjE2NzY0Mzg4NTcuQ2p3S0NBaUFfNnlmQmhCTkVpd0FrbVh5NTcxNlplWmxIQ3RBS0MwdWdUeWIyNnZSRGN1Q01YUHJoZHlYU2Zaekt4eTNNZjZST1I4SVVSb0NwT2NRQXZEX0J3RQ..*_ga*NzYyNTQxODI3LjE2MDExNDI4ODA.*_ga_KN80RDFSQK*MTY3NjgwOTY3NS4zMC4xLjE2NzY4MDk3MDQuMzEuMC4w#agent/ubuntu)



By default, the Datadog Agent is enabled in your `datadog.yaml` file under `apm_config` with `enabled: true` and listens for trace data at `http://localhost:8126`


````shell
[root@pt-instance-6:~/datadog-otel-tracing]$ docker run -d --network app --name dd-agent-dogfood-jmx -v /var/run/docker.sock:/var/run/docker.sock:ro \
-v /proc/:/host/proc/:ro \
-v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
-v /home/pej/conf.d/:/etc/datadog-agent/conf.d \
-p 8126:8126 -p 8125:8125/udp \
-e DD_API_KEY \
-e DD_APM_ENABLED=true \
-e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC="true" -e DD_LOG_LEVEL=debug \
-e DD_LOGS_ENABLED=true \
-e DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true \
-e DD_CONTAINER_EXCLUDE_LOGS="name:datadog-agent" \
-e SD_JMX_ENABLE=true \
gcr.io/datadoghq/agent:latest-jmx
Unable to find image 'gcr.io/datadoghq/agent:latest-jmx' locally
latest-jmx: Pulling from datadoghq/agent
8dbf11a29570: Pull complete 
Digest: sha256:c7fe7c8d15f259185ab0c60dbfb7f5cbc67d09b5749af0d2fee45cefe2ccb05f
Status: Downloaded newer image for gcr.io/datadoghq/agent:latest-jmx
2d1eec89c2196d298d1e3edf1e9f879c0fc3be593d96f1469cfacc2cacfc18b4
````

In order to instrument our services, we will also need to use a java tracing library (`dd-java-agent.jar`). The minimum version to consider for the java agent is 1.10.0

To install the java tracing client, download `dd-java-agent.jar`, which contains the Agent class files
`wget -O dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'`

But you can skip this as the client is already available in this repo. Now let's build, instrument and run our services.
In order to allow the OpenTelemetry based custom instrumentation, the following flag needs to be set: `-Ddd.trace.otel.enabled=true` 


### Building and running the application ###

````shell
[root@pt-instance-6:~/datadog-otel-tracing]$ gradle shadowJar

BUILD SUCCESSFUL in 10s

[root@pt-instance-6:~/datadog-otel-tracing]$ java -javaagent:./dd-java-agent.jar -Ddd.trace.otel.enabled=true -Ddd.service=dd-otel-tracing -Ddd.env=otel -Ddd.version=12 -jar build/libs/datadog-otel-tracing-0.2.0.jar
After filling the headers
After extracting headers
Doing stuff
````


## End
