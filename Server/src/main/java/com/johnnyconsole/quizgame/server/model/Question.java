package com.johnnyconsole.quizgame.server.model;

import java.util.Map;

public class Question {
    private final String id;
    private final String questionText;
    private final Map<String, String> options; // Keys: "A", "B", "C"
    private final String correctLetter;        // "A", "B", or "C"

    public Question(String id, String questionText, Map<String, String> options, String correctLetter) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
        this.correctLetter = correctLetter;
    }

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public Map<String, String> getOptions() { return options; }
    public String getCorrectLetter() { return correctLetter; }
    public String getCorrectAnswerText() { return options.get(correctLetter); }
}