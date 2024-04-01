package sendeverything.service;

import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FileProcessor {
    private static final String API_KEY = "AIzaSyAGmYLWoY7M0UDMToGtcKBKhxg1gi10nKo";

    public String generateContent(String inputText) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"contents\": [{\"parts\":[{\"text\": \"" + inputText + "\"}]}]}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


        // Parse the response body
        JSONObject jsonObject = new JSONObject(response.body());

        // Get the "text" content
        String textContent = jsonObject.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        return textContent;

    }
}