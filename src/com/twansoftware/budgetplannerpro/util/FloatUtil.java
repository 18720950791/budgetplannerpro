package com.twansoftware.budgetplannerpro.util;

import java.text.NumberFormat;
import java.util.Locale;

public class FloatUtil {
    public static String makeDollarStringWithCurrencySymbol(final Float value) {
        final NumberFormat currencyInstance = NumberFormat.getCurrencyInstance(Locale.getDefault());
        return currencyInstance.format(value);
    }
}
