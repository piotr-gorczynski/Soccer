package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

final class SupportUi {
    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static TextView text(Context context, LinearLayout parent, String value, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value); text.setTextSize(16); text.setTextColor(Color.rgb(30, 30, 30));
        if (bold) text.setTypeface(text.getTypeface(), Typeface.BOLD);
        text.setPadding(0, dp(context, 3), 0, dp(context, 3));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }
    static LinearLayout panel(Context context, LinearLayout parent) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        panel.setBackgroundResource(R.drawable.help_panel);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(context, 12); parent.addView(panel, params);
        return panel;
    }
}
