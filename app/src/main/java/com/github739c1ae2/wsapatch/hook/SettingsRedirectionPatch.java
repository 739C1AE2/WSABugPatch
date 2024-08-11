package com.github739c1ae2.wsapatch.hook;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import com.github739c1ae2.wsapatch.util.XSharedPreferencesHelper;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class SettingsRedirectionPatch {

    private static final Set<String> DEFAULT_ACTIONS = new HashSet<String>() {{
        add(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }};

    private final Set<String> mActions;

    public SettingsRedirectionPatch(ClassLoader classLoader) {
        XSharedPreferencesHelper preferencesHelper = new XSharedPreferencesHelper();
        mActions = preferencesHelper.getStringSet("block_setting_redirect_actions", DEFAULT_ACTIONS);
        hook(classLoader);
    }

    private void hook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.microsoft.windows.intents.SettingsIntentHandler",
                classLoader, "onResume", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        Intent intent = activity.getIntent();
                        if (intent == null || !mActions.contains(intent.getAction())) {
                            return;
                        }
                        Intent settingsIntent = new Intent(intent);
                        settingsIntent.setComponent(null);
                        settingsIntent.setPackage("com.android.settings");
                        settingsIntent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivityForResult(settingsIntent, 0);
                        param.setResult(null);
                    }
                });
    }
}
