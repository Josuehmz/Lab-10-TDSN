package com.lab10.ms.http;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public final class LambdaHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LambdaHttp() {
    }

    public static APIGatewayV2HTTPResponse json(int status, Object body) throws JsonProcessingException {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(corsHeaders())
                .withBody(MAPPER.writeValueAsString(body))
                .build();
    }

    public static APIGatewayV2HTTPResponse text(int status, String message) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(corsHeaders())
                .withBody(message == null ? "" : message)
                .build();
    }

    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        String t = s.length() > 400 ? s.substring(0, 400) + "…" : s;
        return t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    public static Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "application/json");
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Access-Control-Allow-Headers", "Authorization,Content-Type");
        h.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        return h;
    }
}
