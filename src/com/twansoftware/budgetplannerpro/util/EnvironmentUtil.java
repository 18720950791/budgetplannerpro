package com.twansoftware.budgetplannerpro.util;

import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * User: achuinard
 * Date: 12/21/12
 */
public class EnvironmentUtil {
    public static boolean shouldUseManageBudgetTabletActivity(final WindowManager windowManager) {
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        float widthInInches = metrics.widthPixels / metrics.xdpi;
        float heightInInches = metrics.heightPixels / metrics.ydpi;
        double sizeInInchesSquared = (widthInInches * widthInInches) + (heightInInches * heightInInches);

        //  0.5" buffer for 7" devices (6.5^2 = 42.25) (7.5^2 = 56.25)
        return sizeInInchesSquared >= 42.25;
    }
}
