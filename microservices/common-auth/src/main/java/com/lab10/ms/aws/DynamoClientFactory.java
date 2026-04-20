package com.lab10.ms.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Cliente DynamoDB: en AWS usa el endpoint por defecto; en local puede apuntar a
 * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html">DynamoDB Local</a>
 * con la variable {@code DYNAMODB_ENDPOINT} (p. ej. {@code http://host.docker.internal:8000} desde el contenedor de SAM).
 */
public final class DynamoClientFactory {

    private DynamoClientFactory() {
    }

    public static DynamoDbClient create() {
        String endpoint = System.getenv("DYNAMODB_ENDPOINT");
        var builder = DynamoDbClient.builder().region(Region.of(firstNonBlank(System.getenv("AWS_REGION"), "us-east-1")));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()));
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b;
    }
}
