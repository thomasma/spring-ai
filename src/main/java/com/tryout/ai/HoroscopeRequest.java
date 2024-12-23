package com.tryout.ai;

public record HoroscopeRequest(String zodiacSign, int days) {
    public String getZodiacSign() {
        return zodiacSign;
    }

    public int getDays() {
        return days;
    }
    
}
