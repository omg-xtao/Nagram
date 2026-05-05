/*
 * Nagram - Inline Bot Rules settings page.
 *
 * Hosts the dual-track rule list (remote read-only + local CRUD).
 * Inherits BaseNekoSettingsActivity to share the standard Nekogram settings
 * layout (blur action bar, theming, edge-to-edge, copy-link long press, ...).
 *
 * Layout sections:
 *   [HEADER  Remote rules]   (only when remote list is non-empty)
 *   [CHECK   remote item ...] (toggle on right tap, view on body tap)
 *   [SHADOW]
 *   [HEADER  Local rules]
 *   [TEXT    "+ Add rule"]
 *   [CHECK   local item ...] (toggle on right tap, edit on body tap)
 *   [SHADOW]
 */

package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.InlineBotRulesRepository;
import tw.nekomimi.nekogram.helpers.InlineBotRulesRepository.RuleItem;
import tw.nekomimi.nekogram.helpers.InlineBotRulesRepository.Source;
import tw.nekomimi.nekogram.ui.InlineBotRulesEditActivity;

public class InlineBotRulesSettingActivity extends BaseNekoSettingsActivity {

    private int remoteHeaderRow;
    private int remoteStartRow;
    private int remoteEndRow;
    private int remoteShadowRow;
    private int localHeaderRow;
    private int addRow;
    private int localStartRow;
    private int localEndRow;
    private int bottomShadowRow;

    private ArrayList<RuleItem> remote = new ArrayList<>();
    private ArrayList<RuleItem> local = new ArrayList<>();

    private final Runnable repositoryListener = this::scheduleReload;

    @Override
    public boolean onFragmentCreate() {
        InlineBotRulesRepository.addListener(repositoryListener);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        InlineBotRulesRepository.removeListener(repositoryListener);
        super.onFragmentDestroy();
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.InlineBotRulesTitle);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        remote = InlineBotRulesRepository.getRemote();
        local = InlineBotRulesRepository.getLocal();

        remoteHeaderRow = remoteStartRow = remoteEndRow = remoteShadowRow = -1;
        localHeaderRow = addRow = localStartRow = localEndRow = bottomShadowRow = -1;

        if (!remote.isEmpty()) {
            remoteHeaderRow = rowCount++;
            remoteStartRow = rowCount;
            rowCount += remote.size();
            remoteEndRow = rowCount;
            remoteShadowRow = rowCount++;
        }

        localHeaderRow = rowCount++;
        addRow = rowCount++;
        if (!local.isEmpty()) {
            localStartRow = rowCount;
            rowCount += local.size();
            localEndRow = rowCount;
        }
        bottomShadowRow = rowCount++;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        InlineBotRulesRepository.triggerRemoteRefresh();
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void scheduleReload() {
        AndroidUtilities.runOnUIThread(() -> {
            updateRows();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean isRemoteRow(int position) {
        return remoteStartRow != -1 && position >= remoteStartRow && position < remoteEndRow;
    }

    private boolean isLocalRow(int position) {
        return localStartRow != -1 && position >= localStartRow && position < localEndRow;
    }

    private RuleItem itemAt(int position) {
        if (isRemoteRow(position)) {
            return remote.get(position - remoteStartRow);
        }
        if (isLocalRow(position)) {
            return local.get(position - localStartRow);
        }
        return null;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == addRow) {
            openCreate();
            return;
        }

        RuleItem item = itemAt(position);
        if (item == null) return;

        boolean rtl = LocaleController.isRTL;
        boolean clickedOnSwitch = (rtl && x < AndroidUtilities.dp(76))
                || (!rtl && x > view.getMeasuredWidth() - AndroidUtilities.dp(76));

        if (clickedOnSwitch && view instanceof TextCheckCell) {
            toggleRow((TextCheckCell) view, item);
            return;
        }

        if (item.source == Source.REMOTE) {
            openView(item);
        } else {
            openEdit(item);
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        RuleItem item = itemAt(position);
        if (item == null) return false;
        if (item.source == Source.REMOTE) {
            openView(item);
        } else {
            openEdit(item);
        }
        return true;
    }

    private void toggleRow(TextCheckCell cell, RuleItem item) {
        boolean newState = !cell.isChecked();
        cell.setChecked(newState);
        item.enabled = newState;
        if (item.source == Source.REMOTE) {
            InlineBotRulesRepository.setRemoteEnabled(item.username, newState);
        } else {
            InlineBotRulesRepository.setLocalEnabled(item.localIndex, newState);
        }
    }

    private void openCreate() {
        InlineBotRulesEditActivity edit = new InlineBotRulesEditActivity();
        edit.setDelegate(this::scheduleReload);
        presentFragment(edit);
    }

    private void openEdit(RuleItem item) {
        InlineBotRulesEditActivity edit = new InlineBotRulesEditActivity(item);
        edit.setDelegate(this::scheduleReload);
        presentFragment(edit);
    }

    private void openView(RuleItem item) {
        InlineBotRulesEditActivity edit = new InlineBotRulesEditActivity(item);
        edit.setDelegate(this::scheduleReload);
        presentFragment(edit);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == remoteHeaderRow || position == localHeaderRow) {
                return TYPE_HEADER;
            }
            if (position == remoteShadowRow || position == bottomShadowRow) {
                return TYPE_SHADOW;
            }
            if (position == addRow) {
                return TYPE_TEXT;
            }
            return TYPE_CHECK;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                     int position, boolean payload, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    holder.itemView.setBackground(Theme.getThemedDrawable(
                            mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == remoteHeaderRow) {
                        cell.setText(LocaleController.getString(R.string.InlineBotRulesRemoteHeader));
                    } else if (position == localHeaderRow) {
                        cell.setText(LocaleController.getString(R.string.InlineBotRulesLocalHeader));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == addRow) {
                        cell.setTextAndIcon(
                                LocaleController.getString(R.string.InlineBotRulesAdd),
                                R.drawable.msg_add, divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    RuleItem item = itemAt(position);
                    if (item != null) {
                        String title = item.getTitle();
                        String summary = item.getSummary();
                        if (TextUtils.isEmpty(summary)) {
                            cell.setTextAndCheck(title, item.enabled, divider);
                        } else {
                            cell.setTextAndValueAndCheck(title, summary, item.enabled, true, divider);
                        }
                    }
                    break;
                }
            }
        }
    }
}
