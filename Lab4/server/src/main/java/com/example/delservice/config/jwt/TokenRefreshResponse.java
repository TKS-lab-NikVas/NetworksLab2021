package com.example.delservice.config.jwt;

public class TokenRefreshResponse {

    private String refreshToken;

    private String jwtToken; // access token

    public TokenRefreshResponse(String refreshToken, String jwtToken) {
        this.refreshToken = refreshToken;
        this.jwtToken = jwtToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
}
