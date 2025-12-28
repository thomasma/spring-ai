package com.tryout.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI-powered chat and image generation endpoints.
 * Provides API endpoints for chat interactions, horoscope predictions, and image generation.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagService ragService;

    @Value("${spring.ai.openai.model:gpt-4o}")
    private String defaultModelName;

    @Value("${spring.ai.openai.temperature:0.7}")
    private double defaultTemperature;

    /**
     * Processes a chat prompt and returns an AI-generated response.
     * Uses model and temperature settings from application.properties.
     *
     * @param request the chat request containing the user's prompt
     * @return ResponseEntity with the AI response or error message
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        logger.info("Received chat request - promptLength: {}, model: {}, temperature: {}",
                    request.prompt() != null ? request.prompt().length() : 0,
                    defaultModelName, defaultTemperature);

        try {
            String response = chatService.processPrompt(request.prompt(), defaultModelName, defaultTemperature);
            logger.info("Chat request processed successfully");
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (IllegalArgumentException e) {
            logger.warn("Chat request validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing chat request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("An unexpected error occurred"));
        }
    }

    /**
     * Generates an image based on a text prompt using DALL-E 3.
     *
     * @param message the image generation prompt
     * @return the URL of the generated image
     */
    @GetMapping("/image/gen")
    public String generateImage(@RequestParam String message) {
        logger.info("Received image generation request - messageLength: {}",
                    message != null ? message.length() : 0);

        try {
            String imageUrl = chatService.generateImage(message);
            logger.info("Image generated successfully");
            return imageUrl;
        } catch (IllegalArgumentException e) {
            logger.warn("Image generation validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error generating image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate image", e);
        }
    }

    /**
     * Generates a personalized horoscope prediction.
     * Uses model and temperature settings from application.properties.
     *
     * @param request the horoscope request containing zodiac sign and number of days
     * @return the AI-generated horoscope prediction
     */
    @PostMapping("/horoscope")
    public String getMyHoroscope(@RequestBody HoroscopeRequest request) {
        logger.info("Received horoscope request - zodiacSign: {}, days: {}, model: {}, temperature: {}",
                    request.zodiacSign(), request.days(), defaultModelName, defaultTemperature);

        try {
            String horoscope = chatService.getMyHoroscope(request.zodiacSign(), request.days(),
                                                          defaultModelName, defaultTemperature);
            logger.info("Horoscope generated successfully for {}", request.zodiacSign());
            return horoscope;
        } catch (IllegalArgumentException e) {
            logger.warn("Horoscope request validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error generating horoscope: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate horoscope", e);
        }
    }

    // ========== RAG Endpoints ==========

    /**
     * Ingests a PDF document into the RAG system.
     * The document is split into chunks, embedded, and stored for semantic search.
     *
     * @param request contains the file path to the PDF document
     * @return ResponseEntity with the number of chunks created
     */
    @PostMapping("/rag/ingest")
    public ResponseEntity<Map<String, Object>> ingestDocument(@RequestBody DocumentIngestRequest request) {
        logger.info("Received document ingest request - filePath: {}", request.filePath());

        try {
            int chunksCreated = ragService.ingestPdfDocument(request.filePath());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document ingested successfully");
            response.put("chunks_created", chunksCreated);
            response.put("total_chunks", ragService.getDocumentCount());

            logger.info("Document ingested successfully - {} chunks created", chunksCreated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error ingesting document: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to ingest document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Performs RAG-based chat: retrieves relevant context from ingested documents,
     * then generates an answer using that context.
     *
     * This demonstrates the core RAG pattern:
     * 1. Embed the user's question
     * 2. Find most similar document chunks
     * 3. Include those chunks as context in the prompt
     * 4. Generate answer grounded in the retrieved context
     *
     * @param request contains the question and optional topK parameter
     * @return ResponseEntity with the AI-generated answer
     */
    @PostMapping("/rag/chat")
    public ResponseEntity<ChatResponse> ragChat(@RequestBody RagChatRequest request) {
        logger.info("Received RAG chat request - question: {}, topK: {}",
                    request.question(), request.topK());

        try {
            String answer = ragService.chatWithDocuments(request.question(), request.topK());
            logger.info("RAG chat completed successfully");
            return ResponseEntity.ok(new ChatResponse(answer));
        } catch (IllegalStateException e) {
            logger.warn("RAG chat failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during RAG chat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("An unexpected error occurred"));
        }
    }

    /**
     * Searches for document chunks most similar to the query.
     * Useful for debugging or showing users what context would be retrieved.
     *
     * @param query the search query
     * @param topK number of results to return (default: 3)
     * @return list of relevant document chunks with metadata
     */
    @GetMapping("/rag/search")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        logger.info("Received document search request - query: {}, topK: {}", query, topK);

        try {
            List<Map<String, Object>> results = ragService.searchDocuments(query, topK);
            logger.info("Document search completed - {} results found", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching documents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * Returns statistics about the RAG document store.
     *
     * @return map containing document count and other metadata
     */
    @GetMapping("/rag/status")
    public ResponseEntity<Map<String, Object>> getRagStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("total_chunks", ragService.getDocumentCount());
        status.put("ready", ragService.getDocumentCount() > 0);

        logger.info("RAG status requested - {} chunks in store", ragService.getDocumentCount());
        return ResponseEntity.ok(status);
    }

    /**
     * Clears all documents from the RAG system.
     *
     * @return confirmation message
     */
    @PostMapping("/rag/clear")
    public ResponseEntity<Map<String, Object>> clearDocuments() {
        logger.info("Clearing RAG document store");

        int previousCount = ragService.getDocumentCount();
        ragService.clearDocuments();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document store cleared");
        response.put("chunks_removed", previousCount);

        logger.info("Document store cleared - {} chunks removed", previousCount);
        return ResponseEntity.ok(response);
    }
}
