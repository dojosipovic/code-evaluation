package com.codeevaluation.core.util;

import io.quarkus.elytron.security.common.BcryptUtil;

public class PasswordUtil {
    public static String hash(String password) {
        return BcryptUtil.bcryptHash(password, 12);
    }

    public static boolean verify(String password, String hash) {
        return BcryptUtil.matches(password, hash);
    }
}
