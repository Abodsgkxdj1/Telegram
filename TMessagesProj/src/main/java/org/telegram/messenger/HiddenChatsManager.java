
package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class HiddenChatsManager {

    private static final String PREFS_NAME = "hidden_chats";
    private static final String HIDDEN_DIALOGS_KEY = "hidden_dialogs_";

    private static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static String getAccountKey(int account) {
        return HIDDEN_DIALOGS_KEY + account;
    }

    public static synchronized boolean isHidden(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }

        Set<String> hiddenChats = getPreferences().getStringSet(
                getAccountKey(account),
                new HashSet<>()
        );

        return hiddenChats != null && hiddenChats.contains(String.valueOf(dialogId));
    }

    public static synchronized void hideChat(int account, long dialogId) {
        if (dialogId == 0) {
            return;
        }

        Set<String> hiddenChats = new HashSet<>(
                getPreferences().getStringSet(
                        getAccountKey(account),
                        new HashSet<>()
                )
        );

        hiddenChats.add(String.valueOf(dialogId));

        getPreferences().edit()
                .putStringSet(getAccountKey(account), hiddenChats)
                .apply();
    }

    public static synchronized void unhideChat(int account, long dialogId) {
        Set<String> hiddenChats = new HashSet<>(
                getPreferences().getStringSet(
                        getAccountKey(account),
                        new HashSet<>()
                )
        );

        hiddenChats.remove(String.valueOf(dialogId));

        getPreferences().edit()
                .putStringSet(getAccountKey(account), hiddenChats)
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
