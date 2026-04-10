package com.example.cs360weighttracker.data;

import androidx.annotation.NonNull;

import java.util.Arrays;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtils {

    private static final int BCRYPT_COST = 12;

    public static String hashPassword(@NonNull String plainPassword) {
        char[] chars = plainPassword.toCharArray();
        try {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    public static boolean checkPassword(@NonNull String plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        char[] chars = plainPassword.toCharArray();
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(chars, hashedPassword);
            return result.verified;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
