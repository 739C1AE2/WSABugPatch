package com.github739c1ae2.wsapatch.util;

import androidx.annotation.Nullable;

import com.github739c1ae2.wsapatch.BuildConfig;

import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

public class XSharedPreferencesHelper {
    @Nullable
    private final XSharedPreferences mSharedPreferences;

    public XSharedPreferencesHelper() {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        mSharedPreferences = pref.getFile().canRead() ? pref : null;
    }

    public int getInt(String key, int defValue) {
        if (mSharedPreferences == null) {
            return defValue;
        }
        return mSharedPreferences.getInt(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue, boolean defValueWhenUnavailable) {
        if (mSharedPreferences == null) {
            return defValueWhenUnavailable;
        }
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public Set<String> getStringSet(String key, Set<String> defValue) {
        if (mSharedPreferences == null) {
            return defValue;
        }
        return mSharedPreferences.getStringSet(key, defValue);
    }
}
