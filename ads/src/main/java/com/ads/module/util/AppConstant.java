package com.ads.module.util;

import androidx.annotation.StringDef;

public class AppConstant {

    @StringDef({
            CollapsibleGravity.TOP,
            CollapsibleGravity.BOTTOM
    })
    public @interface CollapsibleGravity {
        public static final String TOP = "top";
        public static final String BOTTOM = "bottom";
    }
}
