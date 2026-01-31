package com.nexters.sseotdabwa.domain.auth.service.external;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Apple 공개키 응답 DTO
 * - https://appleid.apple.com/auth/keys 응답
 */
@Getter
@NoArgsConstructor
public class ApplePublicKeys {

    private List<Key> keys;

    @Getter
    @NoArgsConstructor
    public static class Key {
        private String kty;  // Key Type (RSA)
        private String kid;  // Key ID
        private String use;  // Key Usage (sig)
        private String alg;  // Algorithm (RS256)
        private String n;    // RSA Modulus (Base64 URL encoded)
        private String e;    // RSA Exponent (Base64 URL encoded)
    }

    /**
     * kid와 일치하는 공개키 찾기
     */
    public Key getMatchedKey(String kid) {
        if (keys == null) {
            return null;
        }
        return keys.stream()
                .filter(key -> key.getKid().equals(kid))
                .findFirst()
                .orElse(null);
    }
}
