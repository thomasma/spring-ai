package com.tryout.ai;

/**
 * Request object for horoscope API endpoint.
 * Records automatically provide accessor methods - no need for manual getters.
 *
 * @param zodiacSign the user's zodiac sign
 * @param days the number of days for the prediction
 */
public record HoroscopeRequest(String zodiacSign, int days) {
}
