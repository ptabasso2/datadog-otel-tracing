package com.datadog.pej;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;


public class OtelCustomInstrum {
    ;
    static Tracer tracer = GlobalOpenTelemetry.get().getTracer("instrumentationName");

    static Map<String, String> globheader;

    public static void main(String[] args) {
        // Start a new Span
        //tracer.spanBuilder("mySpan").startSpan().end();

        doSomeStuffAsParent();
        doSomeStuffInjecting();
    }

    static void doSomeStuffAsParent() {
        Span parentSpan = tracer.spanBuilder("parent").
                setAttribute("span.type", "web").setAttribute("resource.name", "GET /parent").
                startSpan();

        try (Scope scope = parentSpan.makeCurrent()) {
            doSomeStuffAsChild();
        } finally {
            parentSpan.end();
        }
    }

    // NOTE: setParent(...) is not required;
    // `Span.current()` is automatically added as the parent
    static void doSomeStuffAsChild() {
        Span childSpan = tracer.spanBuilder("child").
                setAttribute("span.type", "web").setAttribute("resource.name", "GET /child").
                startSpan();
        try (Scope scope = childSpan.makeCurrent()) {
            // do stuff
        } finally {
            childSpan.end();
        }
    }

    static void doSomeStuffInjecting() {

        Span spaninject = tracer.spanBuilder("inject").
                setAttribute("span.type", "web").setAttribute("resource.name", "GET /inject").
                startSpan();

        try (Scope scope = spaninject.makeCurrent()) {
            Map<String, String> headers = new HashMap<>();
            // do stuff
            /*GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().inject(Context.current(), headers, new TextMapSetter<Map<String, String>>() {
                @Override
                public void set(Map<String, String> carrier, String key, String value) {
                    headers.put(key, value);
                }
            });*/

            W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, (carrier, key, value) -> headers.put(key, value));

            System.out.println("After filling the headers");
            doSomeStuffExtracting(headers);


        } finally {
            spaninject.end();
        }

    }

    static void doSomeStuffExtracting(Map<String, String> headers) {


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

    }
}
