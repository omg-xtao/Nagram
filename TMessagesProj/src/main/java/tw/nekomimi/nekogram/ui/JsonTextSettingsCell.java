package tw.nekomimi.nekogram.ui;

import android.content.Context;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.Gravity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.CodeHighlighting;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.EmojiTextView;
import org.telegram.ui.Components.LayoutHelper;

public class JsonTextSettingsCell extends TextDetailSettingsCell {

    private EmojiTextView jsonTextView;
    private boolean needDivider;
    private SpannableString lockedSpannableString;

    public JsonTextSettingsCell(Context context) {
        super(context);

        jsonTextView = new EmojiTextView(context);
        jsonTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        jsonTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        jsonTextView.setLines(0);
        jsonTextView.setMaxLines(0);
        jsonTextView.setSingleLine(false);
        jsonTextView.setTextIsSelectable(true);
        jsonTextView.setPadding(0, 0, 0, AndroidUtilities.dp(12));
        jsonTextView.setTypeface(android.graphics.Typeface.MONOSPACE);

        getTextView().setVisibility(GONE);
        getValueTextView().setVisibility(GONE);

        addView(jsonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 10, 21, 0));

        setClickable(false);
        setFocusable(false);
    }

    public void setJsonText(CharSequence text, boolean divider) {
        if (text == null || text.length() == 0) {
            jsonTextView.setText("");
            lockedSpannableString = null;
            return;
        }

        CharSequence displayText = Emoji.replaceEmoji(text, jsonTextView.getPaint().getFontMetricsInt(), false);

        lockedSpannableString = CodeHighlighting.getHighlighted(displayText.toString(), "json");
        jsonTextView.setText(lockedSpannableString);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void refreshHighlighting() {
        if (lockedSpannableString != null) {
            jsonTextView.setText(lockedSpannableString);
            jsonTextView.requestLayout();
            jsonTextView.invalidate();
        }
    }

    public void setTitle(CharSequence title) {
        getTextView().setText(title);
        getTextView().setVisibility(VISIBLE);

        android.view.ViewGroup.LayoutParams params = jsonTextView.getLayoutParams();
        if (params instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams layoutParams = (android.widget.FrameLayout.LayoutParams) params;
            layoutParams.topMargin = AndroidUtilities.dp(35);
            jsonTextView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        jsonTextView.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
}
