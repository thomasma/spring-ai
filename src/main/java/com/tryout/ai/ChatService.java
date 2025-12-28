package com.tryout.ai;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for handling AI chat and image generation operations. Provides
 * methods to interact with OpenAI's chat models and image generation
 * capabilities.
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ImageModel imageModel;

    /**
     * Processes a user prompt using the specified GPT model and temperature
     * setting.
     *
     * @param prompt the user's input prompt to be processed by the AI model
     * @param gptModelName the name of the GPT model to use (e.g., "gpt-5.2",
     * "gpt-4")
     * @param temperature controls randomness in the response (0.0 =
     * deterministic, 2.0 = very random)
     * @return the AI-generated response content
     * @throws IllegalArgumentException if the prompt is null, empty, too long, or contains invalid characters
     */
    public String processPrompt(String prompt, String gptModelName, double temperature) {
        logger.info("Processing prompt request - model: {}, temperature: {}, promptLength: {}",
                    gptModelName, temperature, prompt != null ? prompt.length() : 0);

        try {
            validatePrompt(prompt, "Prompt");
            validateModelName(gptModelName);
            validateTemperature(temperature);

            String response = chatClient.prompt(new Prompt(
                    prompt,
                    OpenAiChatOptions.builder()
                            .model(gptModelName)
                            .temperature(temperature)
                            .build()
            ))
                    .call()
                    .content();

            logger.info("Successfully processed prompt - responseLength: {}", response != null ? response.length() : 0);
            return response;
        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for prompt request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing prompt with model {}: {}", gptModelName, e.getMessage(), e);
            throw new RuntimeException("Failed to process prompt", e);
        }
    }

    /**
     * Generates a personalized horoscope prediction based on zodiac sign and
     * number of days. Uses a fortune-telling prompt template to create
     * predictions.
     *
     * @param zodiacSign the zodiac sign for the horoscope (e.g., "Aries",
     * "Taurus")
     * @param days the number of days to predict into the future
     * @param gptModelName the name of the GPT model to use (e.g., "gpt-5.2",
     * "gpt-4")
     * @param temperature controls randomness in the response (0.0 =
     * deterministic, 2.0 = very random)
     * @return the AI-generated horoscope prediction
     * @throws IllegalArgumentException if validation fails on any parameter
     */
    public String getMyHoroscope(String zodiacSign, int days, String gptModelName, double temperature) {
        logger.info("Processing horoscope request - zodiacSign: {}, days: {}, model: {}, temperature: {}",
                    zodiacSign, days, gptModelName, temperature);

        try {
            // Validate all inputs
            validateZodiacSign(zodiacSign);
            validateDays(days);
            validateModelName(gptModelName);
            validateTemperature(temperature);

            // Sanitize the zodiac sign input to prevent prompt injection
            String sanitizedZodiacSign = sanitizeInput(zodiacSign);
            logger.debug("Sanitized zodiac sign from '{}' to '{}'", zodiacSign, sanitizedZodiacSign);

            String template = "I am a fortune teller. I can predict your future. Given your zodiac sign {zodiacSign} please tell me my future for the next {days} days?";
            PromptTemplate promptTemplate = new PromptTemplate(template);
            Map<String, Object> params = Map.of(
                    "zodiacSign", sanitizedZodiacSign,
                    "days", days
            );

            String response = chatClient.prompt(new Prompt(
                    promptTemplate.render(params),
                    OpenAiChatOptions.builder()
                            .model(gptModelName)
                            .temperature(temperature)
                            .build()
            ))
                    .call()
                    .content();

            logger.info("Successfully generated horoscope for {} ({} days) - responseLength: {}",
                        sanitizedZodiacSign, days, response != null ? response.length() : 0);
            return response;
        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for horoscope request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error generating horoscope for {} with model {}: {}",
                        zodiacSign, gptModelName, e.getMessage(), e);
            throw new RuntimeException("Failed to generate horoscope", e);
        }
    }

    /**
     * Generates an image using DALL-E 3 based on the provided text prompt. The
     * image is generated with standard quality at 1024x1024 resolution.
     *
     * @param imagePrompt the text description of the image to generate
     * @return the URL of the generated image
     * @throws IllegalArgumentException if the image prompt is null, empty, or too long
     */
    public String generateImage(String imagePrompt) {
        logger.info("Processing image generation request - promptLength: {}",
                    imagePrompt != null ? imagePrompt.length() : 0);

        try {
            validatePrompt(imagePrompt, "Image prompt");

            String imageUrl = imageModel.call(
                    new ImagePrompt(imagePrompt,
                            OpenAiImageOptions.builder()
                                    .quality("standard")
                                    .model("dall-e-3")
                                    .N(1)
                                    .height(1024)
                                    .width(1024).build())
            ).getResult().getOutput().getUrl();

            logger.info("Successfully generated image - URL: {}", imageUrl);
            return imageUrl;
        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for image generation request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error generating image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate image", e);
        }
    }

    /**
     * Validates a text prompt for AI processing.
     * Protects against prompt injection and ensures input quality.
     *
     * @param prompt the prompt to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePrompt(String prompt, String fieldName) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        if (prompt.length() > 10000) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 10,000 characters");
        }

        // Check for potential prompt injection patterns
        String normalizedPrompt = prompt.toLowerCase();
        if (normalizedPrompt.contains("ignore previous instructions") ||
            normalizedPrompt.contains("ignore above") ||
            normalizedPrompt.contains("disregard previous") ||
            normalizedPrompt.contains("forget previous") ||
            normalizedPrompt.contains("system:") ||
            normalizedPrompt.contains("assistant:") ||
            normalizedPrompt.contains("<|im_sep|>") ||
            normalizedPrompt.contains("<|endoftext|>")) {
            throw new IllegalArgumentException(fieldName + " contains potentially malicious content");
        }
    }

    /**
     * Validates the model name parameter.
     * Allows GPT models from OpenAI including gpt-3.5, gpt-4.x, gpt-5, and their variants.
     *
     * @param modelName the model name to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateModelName(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
    }

    /**
     * Validates the temperature parameter.
     *
     * @param temperature the temperature value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateTemperature(double temperature) {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0");
        }
    }

    /**
     * Validates the zodiac sign parameter.
     *
     * @param zodiacSign the zodiac sign to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateZodiacSign(String zodiacSign) {
        if (zodiacSign == null || zodiacSign.trim().isEmpty()) {
            throw new IllegalArgumentException("Zodiac sign cannot be null or empty");
        }

        if (zodiacSign.length() > 50) {
            throw new IllegalArgumentException("Zodiac sign cannot exceed 50 characters");
        }

        // Whitelist valid zodiac signs (case-insensitive)
        String normalizedSign = zodiacSign.trim().toLowerCase();
        String[] validSigns = {
            "aries", "taurus", "gemini", "cancer", "leo", "virgo",
            "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces"
        };

        boolean isValid = false;
        for (String validSign : validSigns) {
            if (normalizedSign.equals(validSign)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid zodiac sign. Must be one of: Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius, Capricorn, Aquarius, Pisces");
        }
    }

    /**
     * Validates the days parameter for horoscope predictions.
     *
     * @param days the number of days to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be a positive number");
        }

        if (days > 365) {
            throw new IllegalArgumentException("Days cannot exceed 365 (one year)");
        }
    }

    /**
     * Sanitizes user input to prevent prompt injection attacks.
     * Removes or escapes potentially dangerous characters and patterns.
     *
     * @param input the input to sanitize
     * @return sanitized input
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove control characters except newlines and tabs
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ");

        // Remove any potential role/system markers
        sanitized = sanitized.replaceAll("(?i)(system:|user:|assistant:|human:|ai:)", "");

        return sanitized.trim();
    }
}
