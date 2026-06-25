package xyz.nextalone.nagram.helper;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final public class DialogsRecentForwardHelper {

    public static boolean shouldShowRecentTab(int initialDialogsType, boolean onlySelect) {
        return shouldShowRecentTab(initialDialogsType, onlySelect, true);
    }

    public static boolean shouldShowRecentTab(int initialDialogsType, boolean onlySelect, boolean enabled) {
        return enabled && initialDialogsType == DialogsActivity.DIALOGS_TYPE_FORWARD && onlySelect;
    }

    public static boolean isSwipeTargetTabId(int tabId) {
        return tabId != -1;
    }

    public static ArrayList<Integer> buildTabOrder(int filterCount, int recentTabId, boolean showRecentTab) {
        ArrayList<Integer> tabOrder = new ArrayList<>();
        if (showRecentTab) {
            tabOrder.add(recentTabId);
        }
        for (int i = 0; i < filterCount; i++) {
            tabOrder.add(i);
        }
        return tabOrder;
    }

    public static ArrayList<TLRPC.Dialog> buildRecentDialogs(List<Long> recentDialogIds, List<TLRPC.Dialog> knownDialogs, Set<Long> allowedDialogIds) {
        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        if (recentDialogIds == null || recentDialogIds.isEmpty() || allowedDialogIds == null || allowedDialogIds.isEmpty()) {
            return dialogs;
        }

        HashMap<Long, TLRPC.Dialog> knownDialogsById = new HashMap<>();
        if (knownDialogs != null) {
            for (TLRPC.Dialog dialog : knownDialogs) {
                if (dialog != null && dialog.id != 0) {
                    knownDialogsById.put(dialog.id, dialog);
                }
            }
        }

        HashSet<Long> added = new HashSet<>();
        for (Long dialogId : recentDialogIds) {
            if (dialogId == null || dialogId == 0 || !allowedDialogIds.contains(dialogId) || !added.add(dialogId)) {
                continue;
            }
            TLRPC.Dialog dialog = knownDialogsById.get(dialogId);
            dialogs.add(dialog != null ? dialog : createDialog(dialogId));
        }
        return dialogs;
    }

    public static HashSet<Long> dialogIdSet(List<TLRPC.Dialog> dialogs) {
        HashSet<Long> ids = new HashSet<>();
        if (dialogs == null) {
            return ids;
        }
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog != null && dialog.id != 0) {
                ids.add(dialog.id);
            }
        }
        return ids;
    }

    private static TLRPC.Dialog createDialog(long dialogId) {
        TLRPC.TL_dialog dialog = new TLRPC.TL_dialog();
        dialog.id = dialogId;
        dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
        return dialog;
    }
}
