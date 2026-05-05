package project.task2.service.llm;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP client that doesn't use java.net.http (avoids module issues)
 */
public class SimpleHttpClient {
    
    public static String post(String urlString, String requestBody, String apiKey) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        conn.disconnect();
        
        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new IOException("HTTP " + responseCode + ": " + response.toString());
        }
    }
}
