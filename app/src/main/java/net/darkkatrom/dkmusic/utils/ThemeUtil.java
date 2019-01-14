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

public class ThemeUtil {

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
}
