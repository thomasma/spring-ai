package com.tryout.ai;

public record ChatResponse(String message) {
    public String getMessage() {
        return message;
    }
}
