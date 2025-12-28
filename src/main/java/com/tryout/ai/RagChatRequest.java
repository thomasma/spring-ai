package com.tryout.ai;

/**
 * Request model for RAG-based chat queries.
 *
 * @param question the user's question to answer using RAG
 * @param topK number of most relevant document chunks to retrieve (default: 3)
 */
public record RagChatRequest(
    String question,
    Integer topK
) {
    /**
     * Constructor with default topK value.
     */
    public RagChatRequest {
        if (topK == null) {
            topK = 3;
        }
    }
}
