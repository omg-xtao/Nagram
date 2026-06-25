package org.telegram.ui.Components;

import static org.junit.Assert.assertEquals;

import android.util.SparseIntArray;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class FilterTabsViewTest {
    @Test
    public void getStableIdByTabIdUsesTabIdNotTabPosition() {
        SparseIntArray idToPosition = new SparseIntArray();
        idToPosition.put(-100, 0);
        idToPosition.put(0, 1);
        idToPosition.put(1, 2);

        SparseIntArray positionToStableId = new SparseIntArray();
        positionToStableId.put(0, 900);
        positionToStableId.put(1, 100);
        positionToStableId.put(2, 101);

        assertEquals(900, FilterTabsView.stableIdByTabId(idToPosition, positionToStableId, -100));
        assertEquals(100, FilterTabsView.stableIdByTabId(idToPosition, positionToStableId, 0));
        assertEquals(101, FilterTabsView.stableIdByTabId(idToPosition, positionToStableId, 1));
        assertEquals(-1, FilterTabsView.stableIdByTabId(idToPosition, positionToStableId, 2));
    }
}
