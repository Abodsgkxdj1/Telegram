
package org.telegram.messenger.the877;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.HashSet;
import java.util.Set;

public final class The877Manager {

    private static final String PREFS_NAME = "the877_settings";
    private static final String HIDDEN_DIALOGS_KEY = "hidden_dialogs_";

    private The877Manager() {
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;

        if (context == null) {
            throw new IllegalStateException("The877 is not initialized");
        }

        return context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static String getAccountKey(int account) {
        return HIDDEN_DIALOGS_KEY + account;
    }

    public static synchronized boolean isChatHidden(
            int account,
            long dialogId
    ) {
        if (dialogId == 0) {
            return false;
        }

        Set<String> chats = getPreferences().getStringSet(
                getAccountKey(account),
                new HashSet<>()
        );

        return chats != null && chats.contains(
                String.valueOf(dialogId)
        );
    }

    public static synchronized void hideChat(
            int account,
            long dialogId
    ) {
        if (dialogId == 0) {
            return;
        }

        SharedPreferences preferences = getPreferences();

        Set<String> chats = new HashSet<>(
                preferences.getStringSet(
                        getAccountKey(account),
                        new HashSet<>()
                )
        );

        chats.add(String.valueOf(dialogId));

        preferences.edit()
                .putStringSet(getAccountKey(account), chats)
                .apply();
    }

    public static synchronized void unhideChat(
            int account,
            long dialogId
    ) {
        SharedPreferences preferences = getPreferences();

        Set<String> chats = new HashSet<>(
                preferences.getStringSet(
                        getAccountKey(account),
                        new HashSet<>()
                )
        );

        chats.remove(String.valueOf(dialogId));

        preferences.edit()
                .putStringSet(getAccountKey(account), chats)
                .apply();
    }

    public static synchronized Set<Long> getHiddenChats(int account) {
        Set<String> savedChats = getPreferences().getStringSet(
                getAccountKey(account),
                new HashSet<>()
        );

        Set<Long> result = new HashSet<>();

        if (savedChats != null) {
            for (String id : savedChats) {
                try {
                    result.add(Long.parseLong(id));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }
}
