package com.github739c1ae2.wsapatch.preference;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ActivityPreferenceActivity extends AppCompatActivity {

    public static final String EXTRA_SHARED_PREFERENCES_NAME = "sharedPreferencesName";
    public static final String EXTRA_SHARED_PREFERENCES_MODE = "sharedPreferencesMode";
    public static final String EXTRA_KEY = "key";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SUMMARY = "summary";
    public static final String EXTRA_DEFAULT_VALUE = "defaultValue";

    private String mKey;
    private CharSequence mSummary;
    private SharedPreferences mSharedPreferences;

    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String sharedPreferencesName = getIntent().getStringExtra(EXTRA_SHARED_PREFERENCES_NAME);
        int sharedPreferencesMode = getIntent().getIntExtra(EXTRA_SHARED_PREFERENCES_MODE, -1);
        CharSequence title = getIntent().getCharSequenceExtra(EXTRA_TITLE);
        mKey = getIntent().getStringExtra(EXTRA_KEY);
        mSummary = getIntent().getCharSequenceExtra(EXTRA_SUMMARY);
        if (TextUtils.isEmpty(mKey) || sharedPreferencesMode == -1 || sharedPreferencesName == null) {
            finish();
            return;
        }
        if (!TextUtils.isEmpty(title)) {
            setTaskDescription(new ActivityManager.TaskDescription.Builder()
                    .setLabel(title.toString()).build());
        }
        mSharedPreferences = getSharedPreferences(sharedPreferencesName, sharedPreferencesMode);
        onGetDefaultValue(getIntent());
        onInitialize();
    }

    protected abstract void onInitialize();

    protected abstract void onGetDefaultValue(@NonNull Intent intent);

    public String getKey() {
        return mKey;
    }

    public CharSequence getSummary() {
        return mSummary;
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public SharedPreferences.Editor getEditor() {
        return mSharedPreferences.edit();
    }
}
