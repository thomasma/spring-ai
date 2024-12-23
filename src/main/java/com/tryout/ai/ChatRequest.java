package com.tryout.ai;

public record ChatRequest(String prompt) {
    public String getPrompt() {
        return prompt;
    }
}