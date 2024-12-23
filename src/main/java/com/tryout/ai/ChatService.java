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

@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ImageModel imageModel;

    public String processPrompt(String prompt, String gptModelName, double temperature) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        return chatClient.prompt(new Prompt(
                prompt,
                OpenAiChatOptions.builder()
                        .withModel(gptModelName)
                        .withTemperature(temperature)
                        .build()
        ))
                .call()
                .content();

    }

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
                        .withModel(gptModelName)
                        .withTemperature(temperature)
                        .build()
        ))
                .call()
                .content();
    }

    /**
     * Returns the generated image URL
     *
     * @param imagePrompt
     * @return
     */
    public String generateImage(String imagePrompt) {
        return imageModel.call(
                new ImagePrompt(imagePrompt,
                        OpenAiImageOptions.builder()
                                .withQuality("standard")
                                .withModel("dall-e-3")
                                .withN(1)
                                .withHeight(1024)
                                .withWidth(1024).build())
        ).getResult().getOutput().getUrl();
    }
}
