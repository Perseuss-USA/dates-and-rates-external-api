import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RateRequest {
    private static final String BASE_URL =
            getenvOrDefault("DNR_BASE_URL", "https://app.datesnrates.com/api");
    private static final String SIGNING_PATH =
            getenvOrDefault("DNR_SIGNING_PATH", "/external/v1/rates");

    public static void main(String[] args)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        String body = """
                {"brand":{"domain":"%s"},"currency":"USD","destination":{"address1":"548 Market St","city":"San Francisco","country":"US","email":"customer@example.com","name":"Customer Name","phone":"5555555555","postal_code":"94104","province":"CA"},"items":[{"quantity":1,"sku":"external-parcel"}],"origin":{"provider_id":%s},"parcel":{"dimension_unit":"in","height":4,"length":10,"weight":2,"weight_unit":"lb","width":8},"subtotal_price_cents":5000}
                """
                .formatted(
                        getenvOrDefault("DNR_BRAND_DOMAIN", "https://nologo.com"),
                        getenvOrDefault("DNR_3PL_ID", "6"))
                .trim();

        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String signature = signRequest("POST", SIGNING_PATH, body, timestamp, nonce);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/external/v1/rates"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("X-DNR-3PL-ID", requiredEnv("DNR_3PL_ID"))
                .header("X-DNR-API-KEY", requiredEnv("DNR_API_KEY"))
                .header("X-DNR-TIMESTAMP", timestamp)
                .header("X-DNR-NONCE", nonce)
                .header("X-DNR-SIGNATURE", signature)
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    private static String signRequest(
            String method,
            String path,
            String body,
            String timestamp,
            String nonce)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String bodyHash = sha256Hex(body);
        String message = String.join("\n", "v1", method.toUpperCase(), path, timestamp, nonce, bodyHash);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(requiredEnv("DNR_API_SECRET").getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "v1=" + HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha256Hex(String value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value.trim();
    }

    private static String getenvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
