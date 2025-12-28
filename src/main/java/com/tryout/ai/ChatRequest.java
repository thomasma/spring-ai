package com.tryout.ai;

/**
 * Request object for chat API endpoint.
 * Records automatically provide accessor methods - no need for manual getters.
 *
 * @param prompt the user's chat prompt
 */
public record ChatRequest(String prompt) {
}