package com.tryout.ai;

import java.util.Map;

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
     * @throws IllegalArgumentException if the prompt is null or empty
     */
    public String processPrompt(String prompt, String gptModelName, double temperature) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        return chatClient.prompt(new Prompt(
                prompt,
                OpenAiChatOptions.builder()
                        .model(gptModelName)
                        .temperature(temperature)
                        .build()
        ))
                .call()
                .content();

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
     */
    public String getMyHorosope(String zodiacSign, int days, String gptModelName, double temperature) {
        String template = "I am a fortune teller. I can predict your future. Given your zodiac sign {zodiacSign} please tell me my future for the next {days} days?";
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "zodiacSign", zodiacSign,
                "days", days
        );

        return chatClient.prompt(new Prompt(
                promptTemplate.render(params),
                OpenAiChatOptions.builder()
                        .model(gptModelName)
                        .temperature(temperature)
                        .build()
        ))
                .call()
                .content();
    }

    /**
     * Generates an image using DALL-E 3 based on the provided text prompt. The
     * image is generated with standard quality at 1024x1024 resolution.
     *
     * @param imagePrompt the text description of the image to generate
     * @return the URL of the generated image
     */
    public String generateImage(String imagePrompt) {
        return imageModel.call(
                new ImagePrompt(imagePrompt,
                        OpenAiImageOptions.builder()
                                .quality("standard")
                                .model("dall-e-3")
                                .N(1)
                                .height(1024)
                                .width(1024).build())
        ).getResult().getOutput().getUrl();
    }
}
