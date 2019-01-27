/*
 * Copyright (C) 2019 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.darkkatrom.dkmusic.utils;

import android.content.Context;
import android.util.TypedValue;

import net.darkkatrom.dkmusic.R;

public class ThemeUtil {
    public static final int STATUS_BAR_DARKEN_COLOR = 0x30000000;

    public static int getThemeResId(Context context, boolean appTheme) {
        int themeResId = 0;
        if (Config.getTheme(context) == Config.THEME_MATERIAL_DARKKAT) {
            themeResId = appTheme ? R.style.AppThemeDarkKat : R.style.SettingsDarkKat;
        } else if (Config.getTheme(context) == Config.THEME_MATERIAL) {
            themeResId = appTheme ? R.style.AppThemeDark : R.style.SettingsDark;
        } else {
            themeResId = appTheme ? R.style.AppThemeLight : R.style.SettingsLight;
        }
        return themeResId;
    }

    public static int getColorFromThemeAttribute(Context context, int resId) {
        TypedValue tv = new TypedValue();
        int color = 0;
        context.getTheme().resolveAttribute(resId, tv, true);
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            color = tv.data;
        } else {
            color = context.getColor(tv.resourceId);
        }
        return color;
    }

    public static int getStatusBarBackgroundColor(int actionBarColor) {
        return ColorHelper.compositeColors(STATUS_BAR_DARKEN_COLOR, actionBarColor);
    }
}
