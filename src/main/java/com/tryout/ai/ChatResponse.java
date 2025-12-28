package com.tryout.ai;

/**
 * Response object for chat API endpoint.
 * Records automatically provide accessor methods - no need for manual getters.
 *
 * @param message the AI-generated response message
 */
public record ChatResponse(String message) {
}
