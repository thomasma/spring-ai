package com.tryout.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String response = chatService.processPrompt(request.getPrompt(), "gpt-4o", 0.7);
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(e.getMessage()));
        }
    }

    @GetMapping("/image/gen")
    public String generateImage(@RequestParam String message) {
        return chatService.generateImage(message);
    }

    @PostMapping("/horoscope")
    public String getMyHorosope(@RequestBody HoroscopeRequest request) {
        return chatService.getMyHorosope(request.zodiacSign(), request.days(), "gpt-4o", 0.7);
    }
}
