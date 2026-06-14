package com.twansoftware.budgetplannerpro.util;

import android.content.res.Resources;
import android.widget.TextView;
import com.twansoftware.budgetplannerpro.R;

/**
 * User: achuinard
 * Date: 12/18/12
 */
public class TextViewUtil {
    public static void setupCurrencyTextView(final TextView textView, final Resources resources, final Float amountToDisplay) {
        if (amountToDisplay > 0) {
            textView.setTextColor(resources.getColor(R.color.green));
        } else if (amountToDisplay < 0) {
            textView.setTextColor(resources.getColor(R.color.red));
        } else {
            textView.setTextColor(resources.getColor(R.color.black));
        }
        textView.setText(FloatUtil.makeDollarStringWithCurrencySymbol(amountToDisplay));
    }
}
