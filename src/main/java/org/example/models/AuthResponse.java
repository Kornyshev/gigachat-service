package org.example.models;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("expires_at")
    private String expiresAt;
}
