package org.telegram.ui;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import xyz.nextalone.nagram.helper.DialogsRecentForwardHelper;

public class DialogsRecentForwardHelperTest {
    @Test
    public void buildRecentDialogsKeepsRecentOrderAndFiltersInvalidIds() {
        TLRPC.TL_dialog existingUser = new TLRPC.TL_dialog();
        existingUser.id = 11L;
        TLRPC.TL_dialog existingChat = new TLRPC.TL_dialog();
        existingChat.id = -22L;

        assertEquals(
                Arrays.asList(11L, -22L, 33L),
                DialogsRecentForwardHelper.dialogIds(
                        DialogsRecentForwardHelper.buildRecentDialogs(
                                Arrays.asList(11L, -22L, 0L, 11L, 33L),
                                Arrays.asList(existingUser, existingChat),
                                new HashSet<>(Arrays.asList(11L, -22L, 33L)))));
    }

    @Test
    public void buildRecentDialogsReturnsEmptyListForNoValidRecentDialogs() {
        assertEquals(
                Collections.emptyList(),
                DialogsRecentForwardHelper.buildRecentDialogs(
                        Arrays.asList(0L, 44L),
                        Collections.emptyList(),
                        Collections.singleton(55L)));
    }

    @Test
    public void shouldShowRecentTabOnlyForForwardPicker() {
        assertTrue(DialogsRecentForwardHelper.shouldShowRecentTab(DialogsActivity.DIALOGS_TYPE_FORWARD, true, true));
        assertFalse(DialogsRecentForwardHelper.shouldShowRecentTab(DialogsActivity.DIALOGS_TYPE_FORWARD, false, true));
        assertFalse(DialogsRecentForwardHelper.shouldShowRecentTab(DialogsActivity.DIALOGS_TYPE_DEFAULT, true, true));
        assertFalse(DialogsRecentForwardHelper.shouldShowRecentTab(DialogsActivity.DIALOGS_TYPE_FORWARD, true, false));
    }

    @Test
    public void shouldAllowNegativeRecentTabAsSwipeTarget() {
        assertTrue(DialogsRecentForwardHelper.isSwipeTargetTabId(-100));
        assertFalse(DialogsRecentForwardHelper.isSwipeTargetTabId(-1));
    }

    @Test
    public void buildTabOrderPutsRecentBeforeFilters() {
        assertEquals(
                Arrays.asList(-100, 0, 1),
                DialogsRecentForwardHelper.buildTabOrder(2, -100, true));
        assertEquals(
                Arrays.asList(0, 1),
                DialogsRecentForwardHelper.buildTabOrder(2, -100, false));
    }

    @Test
    public void forwardPickerDialogTypesIncludeRecentForward() {
        assertTrue(DialogsActivity.isForwardPickerDialogsType(DialogsActivity.DIALOGS_TYPE_FORWARD));
        assertTrue(DialogsActivity.isForwardPickerDialogsType(DialogsActivity.DIALOGS_TYPE_FORWARD_RECENT));
        assertFalse(DialogsActivity.isForwardPickerDialogsType(DialogsActivity.DIALOGS_TYPE_DEFAULT));
    }
}
