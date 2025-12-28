package com.tryout.ai;

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
}
