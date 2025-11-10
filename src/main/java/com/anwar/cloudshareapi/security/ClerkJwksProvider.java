package com.anwar.cloudshareapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class ClerkJwksProvider {

    @Value("${clerk.jwks-url}")
    private String jwksUrl;

    private final Map<String, PublicKey> keyCache = new HashMap<>();
    private long lastFetchTime = 0;
    private static final long CACHE_TTL = 3600000; // 1 hour

    public PublicKey getPublicKey(String kId) throws Exception {
        // Check if key is cached and not expired
        if (keyCache.containsKey(kId) && (System.currentTimeMillis() - lastFetchTime) < CACHE_TTL) {
            return keyCache.get(kId);
        }
        // Otherwise refresh cache
        refreshKeys();
        return keyCache.get(kId);
    }

    private void refreshKeys() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jwks = mapper.readTree(new URL(jwksUrl));

        keyCache.clear(); // Clear old keys

        for (JsonNode keyNode : jwks.get("keys")) {
            String kid = keyNode.get("kid").asText();
            String n = keyNode.get("n").asText();  // modulus
            String e = keyNode.get("e").asText();  // exponent

            try {
                byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
                byte[] exponentBytes = Base64.getUrlDecoder().decode(e);

                // Build RSA public key
                java.math.BigInteger modulus = new java.math.BigInteger(1, modulusBytes);
                java.math.BigInteger exponent = new java.math.BigInteger(1, exponentBytes);
                java.security.spec.RSAPublicKeySpec keySpec =
                        new java.security.spec.RSAPublicKeySpec(modulus, exponent);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                keyCache.put(kid, publicKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        lastFetchTime = System.currentTimeMillis();
    }
}
