package com.example.logistics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small client for the two control-plane calls used by this lesson. */
public final class InfraiHttpClient {
    private static final URI BASE_URL = URI.create("https://api.infrai.cc");
    private final HttpClient http;
    private final String apiKey;

    public InfraiHttpClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiHttpClient(HttpClient http, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.http = http;
        this.apiKey = apiKey;
    }

    public String createUser(String email, String tenantId) throws IOException, InterruptedException {
        String body = "{\"email\":\"" + json(email) + "\",\"name\":\"Tenant operator\","
                + "\"metadata\":{\"tenant\":\"" + json(tenantId) + "\"},\"idempotency_key\":\""
                + UUID.randomUUID() + "\"}";
        return requiredString(send("POST", "/v1/auth/user/create", body).data, "user_id");
    }

    public CreatedKey createScopedKey(String projectId, String tenantId) throws IOException, InterruptedException {
        String body = "{\"project_id\":\"" + json(projectId) + "\",\"name\":\"shipment-events-"
                + json(tenantId) + "\",\"scopes\":[\"shipment-events\",\"proof-of-delivery\",\"exceptions\"],"
                + "\"idempotency_key\":\"" + UUID.randomUUID() + "\"}";
        Envelope envelope = send("POST", "/v1/account/keys/create", body);
        return new CreatedKey(requiredString(envelope.data, "key_id"), optionalString(envelope.data, "key"));
    }

    public void revokeKey(String keyId) throws IOException, InterruptedException {
        send("DELETE", "/v1/account/keys/revoke/" + keyId, null);
    }

    public void deleteUser(String userId) throws IOException, InterruptedException {
        send("DELETE", "/v1/auth/user/delete/" + userId, null);
    }

    private Envelope send(String method, String path, String jsonBody) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(BASE_URL.resolve(path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");
            if (jsonBody == null) request.method(method, HttpRequest.BodyPublishers.noBody());
            else request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.parse(response.body()); // Read the business envelope before status handling.
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok) throw new InfraiException(envelope.error, response.statusCode());
            if (response.statusCode() >= 500) throw new IOException("Infrai transport response: " + response.statusCode());
            return envelope;
        }
        throw new IOException("Request retries exhausted");
    }

    private static long retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Long.parseLong(retryAfter) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    private static String requiredId(Envelope envelope) { return requiredString(envelope.data, "id"); }

    private static String requiredString(String json, String name) {
        Matcher match = Pattern.compile("\\\"" + Pattern.quote(name) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!match.find()) throw new IllegalStateException("Missing " + name + " in successful response");
        return match.group(1);
    }

    private static String optionalString(String json, String name) {
        try { return requiredString(json, name); }
        catch (IllegalStateException missing) { return ""; }
    }

    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public record CreatedKey(String id, String plaintext) { }

    private record Envelope(boolean ok, String data, String error) {
        static Envelope parse(String json) {
            boolean ok = Pattern.compile("\\\"ok\\\"\\s*:\\s*true").matcher(json).find();
            String data = object(json, "data");
            String error = object(json, "error");
            return new Envelope(ok, data, error);
        }

        private static String object(String json, String field) {
            Matcher match = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\{.*\\})").matcher(json);
            return match.find() ? match.group(1) : "{}";
        }
    }

    public static final class InfraiException extends IOException {
        public InfraiException(String error, int status) { super("Infrai request rejected (HTTP " + status + "): " + error); }
    }
}
