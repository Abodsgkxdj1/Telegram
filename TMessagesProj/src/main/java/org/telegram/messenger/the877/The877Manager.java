package org.telegram.messenger.the877;

import org.telegram.messenger.HiddenChatsManager;
import java.util.Set;

public final class The877Manager {

    private The877Manager() {
    }

    public static synchronized boolean isChatHidden(int account, long dialogId) {
        return HiddenChatsManager.isHidden(account, dialogId);
    }

    public static synchronized void hideChat(int account, long dialogId) {
        HiddenChatsManager.hideChat(account, dialogId);
    }

    public static synchronized void unhideChat(int account, long dialogId) {
        HiddenChatsManager.unhideChat(account, dialogId);
    }

    public static synchronized Set<Long> getHiddenChats(int account) {
        return HiddenChatsManager.getHiddenChats(account);
    }
}
