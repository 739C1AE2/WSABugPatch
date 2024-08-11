package com.github739c1ae2.wsapatch.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;

public class ApplicationSelectPreference extends ActivityPreference {

    private HashSet<String> mDefaultValue;

    public ApplicationSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Nullable
    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        CharSequence[] defValue = a.getTextArray(index);
        if (defValue != null) {
            mDefaultValue = new HashSet<>();
            for (CharSequence charSequence : defValue) {
                mDefaultValue.add(charSequence.toString());
            }
            return mDefaultValue;
        }
        return super.onGetDefaultValue(a, index);
    }

    @Override
    protected void onPutDefaultValue(@NonNull Intent intent) {
        intent.putExtra(ActivityPreferenceActivity.EXTRA_DEFAULT_VALUE, mDefaultValue);
    }

    @Override
    @NonNull
    protected Class<? extends ActivityPreferenceActivity> getActivityClass() {
        return ApplicationSelectPreferenceActivity.class;
    }
}
