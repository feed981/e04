package com.feddoubt.test;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Base64;

public class KeyTest {
    public static void main(String[] args) {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String base64EncodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println(base64EncodedKey);
    }
}
