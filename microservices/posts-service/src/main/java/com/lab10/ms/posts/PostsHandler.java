package com.lab10.ms.posts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab10.ms.auth.Auth0JwtVerifier;
import com.lab10.ms.aws.DynamoClientFactory;
import com.lab10.ms.http.LambdaHttp;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Microservicio de posts: crea posts (POST) con scope write:posts.
 */
public class PostsHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicReference<Auth0JwtVerifier> verifierRef = new AtomicReference<>();
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

            if (!"POST".equalsIgnoreCase(method)) {
                return LambdaHttp.text(405, "{\"error\":\"Método no permitido\"}");
            }

            Auth0JwtVerifier verifier = verifier();
            String auth = header(event, "authorization");
            DecodedJWT jwt = verifier.verifyBearer(auth);
            if (!Auth0JwtVerifier.hasScope(jwt, "write:posts")) {
                return LambdaHttp.text(403, "{\"error\":\"Scope write:posts requerido\"}");
            }

            String sub = jwt.getSubject();
            String email = Auth0JwtVerifier.claimString(jwt, "email");
            String name = Auth0JwtVerifier.firstNonBlank(
                    Auth0JwtVerifier.claimString(jwt, "name"),
                    Auth0JwtVerifier.claimString(jwt, "nickname"),
                    email
            );
            String picture = Auth0JwtVerifier.claimString(jwt, "picture");

            String usersTable = requiredEnv("USERS_TABLE");
            String postsTable = requiredEnv("POSTS_TABLE");
            putUser(usersTable, sub, email, name, picture);

            JsonNode root = JSON.readTree(event.getBody() == null ? "{}" : event.getBody());
            String content = root.path("content").asText("").trim();
            if (content.isEmpty() || content.length() > 140) {
                return LambdaHttp.text(400, "{\"error\":\"content: 1-140 caracteres\"}");
            }

            long createdAt = System.currentTimeMillis();
            String postId = UUID.randomUUID().toString();
            putPost(postsTable, postId, sub, content, createdAt);

            Map<String, Object> response = new HashMap<>();
            response.put("id", postId);
            response.put("content", content);
            response.put("authorId", sub);
            response.put("createdAt", Instant.ofEpochMilli(createdAt).toString());
            response.put("authorName", name != null ? name : sub);
            return LambdaHttp.json(201, response);
        } catch (IllegalArgumentException e) {
            return LambdaHttp.text(401, "{\"error\":\"" + LambdaHttp.escapeJson(e.getMessage()) + "\"}");
        } catch (DynamoDbException e) {
            String d = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : String.valueOf(e.getMessage());
            return LambdaHttp.text(500, "{\"error\":\"DynamoDB\",\"detail\":\"" + LambdaHttp.escapeJson(d) + "\"}");
        } catch (Exception e) {
            return LambdaHttp.text(500, "{\"error\":\"Interno\",\"detail\":\"" + LambdaHttp.escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Auth0JwtVerifier verifier() {
        return verifierRef.updateAndGet(v -> v != null ? v : new Auth0JwtVerifier(
                requiredEnv("AUTH0_DOMAIN"),
                requiredEnv("AUTH0_AUDIENCE")
        ));
    }

    private static String requiredEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Variable " + name + " no configurada");
        }
        return v.trim();
    }

    private static String header(APIGatewayV2HTTPEvent event, String name) {
        if (event.getHeaders() == null) {
            return null;
        }
        for (Map.Entry<String, String> e : event.getHeaders().entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private void putUser(String table, String userId, String email, String name, String picture) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        if (email != null && !email.isBlank()) {
            item.put("email", AttributeValue.builder().s(email).build());
        }
        if (name != null && !name.isBlank()) {
            item.put("name", AttributeValue.builder().s(name).build());
        }
        if (picture != null && !picture.isBlank()) {
            item.put("pictureUrl", AttributeValue.builder().s(picture).build());
        }
        dynamo.putItem(PutItemRequest.builder().tableName(table).item(item).build());
    }

    private void putPost(String table, String postId, String authorId, String content, long createdAt) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("postId", AttributeValue.builder().s(postId).build());
        item.put("authorId", AttributeValue.builder().s(authorId).build());
        item.put("content", AttributeValue.builder().s(content).build());
        item.put("createdAt", AttributeValue.builder().n(Long.toString(createdAt)).build());
        dynamo.putItem(PutItemRequest.builder().tableName(table).item(item).build());
    }
}
