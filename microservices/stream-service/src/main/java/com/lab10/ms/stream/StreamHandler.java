package com.lab10.ms.stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.lab10.ms.aws.DynamoClientFactory;
import com.lab10.ms.http.LambdaHttp;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Microservicio de stream/feed: lectura pública del feed desde DynamoDB.
 */
public class StreamHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final int BATCH_KEYS = 100;

    private final DynamoDbClient dynamo = DynamoClientFactory.create();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String method = event.getRequestContext().getHttp().getMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return APIGatewayV2HTTPResponse.builder()
                        .withStatusCode(204)
                        .withHeaders(LambdaHttp.corsHeaders())
                        .build();
            }

            if (!"GET".equalsIgnoreCase(method)) {
                return LambdaHttp.text(405, "{\"error\":\"Método no permitido\"}");
            }

            String path = event.getRawPath() == null ? "" : event.getRawPath();
            if (!path.endsWith("/posts") && !path.endsWith("/stream")) {
                return LambdaHttp.text(404, "{\"error\":\"No encontrado\"}");
            }

            String postsTable = requiredEnv("POSTS_TABLE");
            String usersTable = requiredEnv("USERS_TABLE");

            List<Map<String, AttributeValue>> rows = scanAllPosts(postsTable);
            rows.sort(Comparator.comparingLong(StreamHandler::createdAtMillis).reversed());

            Set<String> authorIds = new HashSet<>();
            for (Map<String, AttributeValue> row : rows) {
                AttributeValue aid = row.get("authorId");
                if (aid != null && aid.s() != null) {
                    authorIds.add(aid.s());
                }
            }
            Map<String, String> names = batchLoadNames(usersTable, authorIds);

            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, AttributeValue> row : rows) {
                String postId = attrS(row, "postId");
                String content = attrS(row, "content");
                String authorId = attrS(row, "authorId");
                long ts = createdAtMillis(row);
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", postId);
                dto.put("content", content);
                dto.put("authorId", authorId);
                dto.put("createdAt", Instant.ofEpochMilli(ts).toString());
                dto.put("authorName", names.get(authorId));
                out.add(dto);
            }

            return LambdaHttp.json(200, out);
        } catch (DynamoDbException e) {
            String d = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : String.valueOf(e.getMessage());
            return LambdaHttp.text(500, "{\"error\":\"DynamoDB\",\"detail\":\"" + LambdaHttp.escapeJson(d) + "\"}");
        } catch (Exception e) {
            return LambdaHttp.text(500, "{\"error\":\"Interno\",\"detail\":\"" + LambdaHttp.escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static String requiredEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Variable " + name + " no configurada");
        }
        return v.trim();
    }

    private List<Map<String, AttributeValue>> scanAllPosts(String table) {
        List<Map<String, AttributeValue>> all = new ArrayList<>();
        Map<String, AttributeValue> startKey = null;
        do {
            ScanRequest.Builder b = ScanRequest.builder().tableName(table);
            if (startKey != null && !startKey.isEmpty()) {
                b.exclusiveStartKey(startKey);
            }
            ScanResponse scan = dynamo.scan(b.build());
            all.addAll(scan.items());
            startKey = scan.lastEvaluatedKey();
        } while (startKey != null && !startKey.isEmpty());
        return all;
    }

    private static long createdAtMillis(Map<String, AttributeValue> row) {
        AttributeValue v = row.get("createdAt");
        if (v == null || v.n() == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v.n());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String attrS(Map<String, AttributeValue> row, String key) {
        AttributeValue v = row.get(key);
        return v == null ? null : v.s();
    }

    private Map<String, String> batchLoadNames(String usersTable, Set<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> names = new HashMap<>();
        List<String> ids = new ArrayList<>(userIds);
        Collections.sort(ids);
        for (int i = 0; i < ids.size(); i += BATCH_KEYS) {
            int end = Math.min(i + BATCH_KEYS, ids.size());
            List<Map<String, AttributeValue>> keys = new ArrayList<>();
            for (int j = i; j < end; j++) {
                String id = ids.get(j);
                keys.add(Map.of("userId", AttributeValue.builder().s(id).build()));
            }
            BatchGetItemRequest req = BatchGetItemRequest.builder()
                    .requestItems(Map.of(
                            usersTable,
                            KeysAndAttributes.builder().keys(keys).build()
                    ))
                    .build();
            BatchGetItemResponse resp = dynamo.batchGetItem(req);
            List<Map<String, AttributeValue>> items = resp.responses().getOrDefault(usersTable, List.of());
            for (Map<String, AttributeValue> it : items) {
                String uid = attrS(it, "userId");
                String name = attrS(it, "name");
                if (uid != null && name != null) {
                    names.put(uid, name);
                }
            }
        }
        return names;
    }
}
