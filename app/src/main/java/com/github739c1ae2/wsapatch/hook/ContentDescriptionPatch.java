package com.github739c1ae2.wsapatch.hook;

import android.view.View;

import com.github739c1ae2.wsapatch.util.XSharedPreferencesHelper;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ContentDescriptionPatch {

    public ContentDescriptionPatch(XC_LoadPackage.LoadPackageParam lpp) {
        XSharedPreferencesHelper preferencesHelper = new XSharedPreferencesHelper();
        Set<String> scope = preferencesHelper.getStringSet("content_description_as_tooltip_scope",
                new HashSet<>());
        if (!scope.isEmpty() && !scope.contains(lpp.packageName)) {
            return;
        }
        if (preferencesHelper.getBoolean("content_description_as_tooltip", false,
                true)) {
            hookContentDescription();
        }
    }

    private void hookContentDescription() {
        XposedHelpers.findAndHookMethod(View.class, "setContentDescription",
                CharSequence.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View view = (View) param.thisObject;
                        CharSequence contentDescription = (CharSequence) param.args[0];
                        if ((view.getTooltipText() == null || view.getTooltipText().length() == 0)
                                && contentDescription != null && contentDescription.length() > 0) {
                            view.setTooltipText(contentDescription);
                        }
                    }
                });
    }
}
