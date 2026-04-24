package project.task3.model;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;

public class SummaryGenerator {
    public class DeepseekClient {
        private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
        private final OkHttpClient client = new OkHttpClient();
        private final Gson gson = new Gson();

        public String getResponse(String apiKey, String prompt) throws IOException {
            DeepSeekRequest.Message message = new DeepSeekRequest.Message("user", prompt);
            DeepSeekRequest requestBody = new DeepSeekRequest("deepseek-chat", Collections.singletonList(message));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(gson.toJson(requestBody), MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                return response.body().string();
            }
        }
    }

    public String generate(String filePath) {
        String key = System.getenv("DEEPSEEK_APIKEY");//The computer executing this should set the environment variable to the API key of deepseek
        String text;
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return "";
        }

        try {
            String responseString = new DeepseekClient().getResponse(
                    key,
                    "Please summarize the contents below in English, with less than 300 words: \n" + text);
            DeepSeekResponse response = new Gson().fromJson(responseString, DeepSeekResponse.class);
            return response.getChoices().getFirst().getMessage().getContent();
        } catch (IOException e) {
            return "Error：" + e.getMessage();
        }
    }
}
