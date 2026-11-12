package org.telegram.messenger.the877;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Local PIN protection for The 877 hidden chats.
 *
 * The PIN itself is never stored. A per-install random salt and a PBKDF2
 * derived verifier are stored in private app preferences.
 */
public final class The877SecurityManager {

    private static final String PREFS = "the877_hidden_security";
    private static final String SALT_PREFIX = "pin_salt_";
    private static final String HASH_PREFIX = "pin_hash_";
    private static final String FAIL_PREFIX = "pin_failures_";
    private static final String LOCK_PREFIX = "pin_locked_until_";

    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int ITERATIONS = 150000;
    private static final int MIN_PIN_LENGTH = 6;
    private static final int MAX_PIN_LENGTH = 12;

    private The877SecurityManager() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );
    }

    private static String key(String prefix, int account) {
        return prefix + account;
    }

    public static synchronized boolean hasPin(int account) {
        return prefs().contains(key(HASH_PREFIX, account))
                && prefs().contains(key(SALT_PREFIX, account));
    }

    /**
     * Sets or replaces the hidden-chats PIN.
     * Accepts 6-12 decimal digits only.
     */
    public static synchronized boolean setPin(int account, String pin) {
        if (!isValidPin(pin)) {
            return false;
        }

        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] derived = derive(pin.toCharArray(), salt);
        if (derived == null) {
            return false;
        }

        prefs().edit()
                .putString(key(SALT_PREFIX, account), Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(key(HASH_PREFIX, account), Base64.encodeToString(derived, Base64.NO_WRAP))
                .remove(key(FAIL_PREFIX, account))
                .remove(key(LOCK_PREFIX, account))
                .apply();

        Arrays.fill(derived, (byte) 0);
        Arrays.fill(salt, (byte) 0);
        return true;
    }

    /**
     * Returns true only when the supplied PIN matches and no retry lock is active.
     */
    public static synchronized boolean verifyPin(int account, String pin) {
        if (!hasPin(account) || !isValidPin(pin) || isLocked(account)) {
            return false;
        }

        String saltText = prefs().getString(key(SALT_PREFIX, account), null);
        String hashText = prefs().getString(key(HASH_PREFIX, account), null);
        if (saltText == null || hashText == null) {
            return false;
        }

        try {
            byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
            byte[] expected = Base64.decode(hashText, Base64.NO_WRAP);
            byte[] actual = derive(pin.toCharArray(), salt);

            boolean matches = actual != null && MessageDigest.isEqual(expected, actual);
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }

            if (matches) {
                prefs().edit()
                        .remove(key(FAIL_PREFIX, account))
                        .remove(key(LOCK_PREFIX, account))
                        .apply();
                return true;
            }

            registerFailure(account);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean isLocked(int account) {
        return System.currentTimeMillis() < prefs().getLong(key(LOCK_PREFIX, account), 0L);
    }

    public static synchronized long getLockRemainingMillis(int account) {
        return Math.max(0L, prefs().getLong(key(LOCK_PREFIX, account), 0L)
                - System.currentTimeMillis());
    }

    /**
     * Removes the PIN only after the caller has independently authenticated
     * the user. This method intentionally does not perform authentication.
     */
    public static synchronized void clearPinAfterAuthentication(int account) {
        prefs().edit()
                .remove(key(SALT_PREFIX, account))
                .remove(key(HASH_PREFIX, account))
                .remove(key(FAIL_PREFIX, account))
                .remove(key(LOCK_PREFIX, account))
                .apply();
    }

    private static boolean isValidPin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return false;
        }
        for (int i = 0; i < pin.length(); i++) {
            if (pin.charAt(i) < '0' || pin.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static byte[] derive(char[] pin, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, ITERATIONS, HASH_BITS);
        try {
            SecretKeyFactory factory;
            try {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (Exception unavailable) {
                // PBKDF2-HMAC-SHA1 is available on the project's Android 6 minimum.
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return null;
        } finally {
            spec.clearPassword();
            Arrays.fill(pin, '\0');
        }
    }

    private static void registerFailure(int account) {
        SharedPreferences p = prefs();
        int failures = p.getInt(key(FAIL_PREFIX, account), 0) + 1;
        long lockDuration = 0L;

        if (failures >= 5) {
            int exponent = Math.min(failures - 5, 6);
            lockDuration = Math.min(30L * 60L * 1000L,
                    30L * 1000L * (1L << exponent));
        }

        SharedPreferences.Editor editor = p.edit()
                .putInt(key(FAIL_PREFIX, account), failures);
        if (lockDuration > 0L) {
            editor.putLong(key(LOCK_PREFIX, account),
                    System.currentTimeMillis() + lockDuration);
        }
        editor.apply();
    }
}
