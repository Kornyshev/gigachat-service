package org.example;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.models.AuthResponse;
import org.example.models.Message;
import org.example.models.PromptResponse;
import org.example.models.RequestPromptModel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class App {

    private static final String ENDPOINT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final String AUTH_ENDPOINT_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    /*
    You have to use your own AUTHORIZATION_KEY from personal profile, put it in environment variable before the run.
     */
    private static final String AUTHORIZATION_KEY = System.getenv("auth_key");
    private static final String SCOPE = "GIGACHAT_API_PERS";
    private static final MediaType JSON_DATA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final MediaType FORM_DATA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    public static final String USER_ROLE = "user";
    public static final String SYSTEM_ROLE = "system";
    public static final String APPLICATION_JSON = "application/json";

    public static void main(String[] args) throws Exception {
        String systemRolePrompt = """
                Ты автоматизатор тестировщик на Java с большим опытом.
                """;
        String userPrompt = """
                Опиши что такое процесс CI/CD, почему он важен
                и какую роль в нём играет команда автоматизации тестирования.
                """;

        String accessToken = sendOAuthRequest();
        PromptResponse response = sendPrompt(accessToken, systemRolePrompt, userPrompt);
        /*
        Here you have object with response from GigaChat LLM, you can use however you want
         */
    }

    private static PromptResponse sendPrompt(String accessToken, String systemRolePrompt, String userPrompt) throws IOException {
        RequestPromptModel promptModel = generatePrompt(systemRolePrompt, userPrompt);
        Request request = new Request.Builder()
                .url(ENDPOINT_URL)
                .method("POST", RequestBody.create(JSON_DATA_TYPE, new Gson().toJson(promptModel)))
                .addHeader("Content-Type", APPLICATION_JSON)
                .addHeader("Accept", APPLICATION_JSON)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        return new Gson().fromJson(createUnsafeOkHttpClient().newCall(request).execute().body().string(), PromptResponse.class);
    }

    private static RequestPromptModel generatePrompt(String systemRolePrompt, String userPrompt) {
        Message systemMessage = Message.builder().role(SYSTEM_ROLE)
                .content(systemRolePrompt).build();
        Message userMessage = Message.builder().role(USER_ROLE)
                .content(userPrompt).build();
        return RequestPromptModel.builder().model("GigaChat")
                .messages(List.of(systemMessage, userMessage))
                .stream(false).updateInterval(0).build();
    }

    private static String sendOAuthRequest() throws IOException {
        Request request = new Request.Builder()
                .url(AUTH_ENDPOINT_URL)
                .method("POST", RequestBody.create(FORM_DATA_TYPE, "scope=" + SCOPE))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", APPLICATION_JSON)
                .addHeader("Authorization", "Basic " + AUTHORIZATION_KEY)
                .addHeader("RqUID", UUID.randomUUID().toString())
                .build();
        try (Response response = createUnsafeOkHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Ошибка при выполнении запроса: " + response);
            return new Gson().fromJson(Objects.requireNonNull(response.body()).charStream(), AuthResponse.class).getAccessToken();
        }
    }

    private static OkHttpClient createUnsafeOkHttpClient() {
        try {
            // Создаем доверительный менеджер, который принимает все сертификаты
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Создаем SSL-контекст с нашим доверительным менеджером
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Создаем клиент OkHttp с отключенной проверкой сертификатов
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true); // Отключаем проверку имени хоста

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
