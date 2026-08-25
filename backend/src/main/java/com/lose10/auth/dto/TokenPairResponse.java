package com.lose10.auth.dto;

public record TokenPairResponse(String accessToken, String refreshToken, UserSummary user) {
}
