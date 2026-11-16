package com.johnnyconsole.quizgame.server.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QuestionService {

    private static final String API_URL = "https://the-trivia-api.com/v2/questions?limit=50";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Asynchronously fetches up to `targetCount` unique questions, trims them to 3 options (A, B, C),
     * and returns them in a thread-safe Queue.
     */
    public CompletableFuture<Queue<Question>> fetchQuestionsAsync(int targetCount) {
        return CompletableFuture.supplyAsync(() -> {
            Queue<Question> questionQueue = new ConcurrentLinkedQueue<>();
            Set<String> seenIds = new HashSet<>();

            // Fetch in batches of 50 until we reach targetCount (with a safety cap on attempts)
            int attempts = 0;
            while (questionQueue.size() < targetCount && attempts < 4) {
                attempts++;
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Accept", "application/json")
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();

                        for (JsonElement element : array) {
                            if (questionQueue.size() >= targetCount) break;

                            JsonObject obj = element.getAsJsonObject();
                            String id = obj.get("id").getAsString();
                            String text = obj.getAsJsonObject("question").get("text").getAsString();

                            // Skip duplicates or overly wordy questions to keep "The Chase" pacing snappy
                            if (seenIds.contains(id) || text.length() > 130) {
                                continue;
                            }

                            String correctAnswer = obj.get("correctAnswer").getAsString();
                            JsonArray incorrectJson = obj.getAsJsonArray("incorrectAnswers");

                            List<String> incorrectList = new ArrayList<>();
                            for (JsonElement inc : incorrectJson) {
                                incorrectList.add(inc.getAsString());
                            }

                            // Shuffle wrong answers and keep only 2
                            Collections.shuffle(incorrectList);
                            List<String> threeChoices = new ArrayList<>();
                            threeChoices.add(correctAnswer);
                            threeChoices.add(incorrectList.get(0));
                            threeChoices.add(incorrectList.get(1));

                            // Shuffle the final 3 choices and map to A, B, C
                            Collections.shuffle(threeChoices);
                            Map<String, String> optionMap = new LinkedHashMap<>();
                            String[] letters = {"A", "B", "C"};
                            String correctLetter = "A";

                            for (int i = 0; i < 3; i++) {
                                optionMap.put(letters[i], threeChoices.get(i));
                                if (threeChoices.get(i).equals(correctAnswer)) {
                                    correctLetter = letters[i];
                                }
                            }

                            seenIds.add(id);
                            questionQueue.add(new Question(id, text, optionMap, correctLetter));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching trivia batch: " + e.getMessage());
                }
            }

            return questionQueue;
        });
    }
}