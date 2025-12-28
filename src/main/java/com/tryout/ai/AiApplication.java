package com.tryout.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ImageModel imageModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
        var openAiImageApi = OpenAiImageApi.builder()
                .apiKey(apiKey)
                .build();
        return new OpenAiImageModel(openAiImageApi);
    }

    @Bean
    OpenAiEmbeddingModel embeddingModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
        var openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(openAiApi);
    }
}
