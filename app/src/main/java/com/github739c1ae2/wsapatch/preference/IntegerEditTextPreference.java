package com.github739c1ae2.wsapatch.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.github739c1ae2.wsapatch.R;

public class IntegerEditTextPreference extends DialogPreference {

    private int mDefaultValue = -1;
    private int mInteger;
    private final int mMinValue;
    private final int mMaxValue;


    public IntegerEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.IntegerEditTextPreference, defStyleAttr, defStyleRes);

        mMinValue = a.getInteger(R.styleable.IntegerEditTextPreference_minValue, Integer.MIN_VALUE);
        mMaxValue = a.getInteger(R.styleable.IntegerEditTextPreference_maxValue, Integer.MAX_VALUE);

        a.recycle();
    }

    public IntegerEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public IntegerEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.integerEditTextPreferenceStyle);
    }

    public void setInteger(@Nullable Integer integer) {
        final boolean wasBlocking = shouldDisableDependents();

        mInteger = integer != null ? integer : mDefaultValue;

        persistInt(mInteger);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }

        notifyChanged();
    }

    public boolean checkInteger(Integer value) {
        return value != null && value >= mMinValue && value <= mMaxValue;
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        return getOnPreferenceChangeListener() == null ? onChange(newValue) : super.callChangeListener(newValue);
    }

    private boolean onChange(Object newValue) {
        if (checkInteger((Integer) newValue)) {
            return true;
        } else {
            Toast.makeText(getContext(), R.string.illegal_data, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public int getInteger() {
        return mInteger;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public int getInputType() {
        if (mMinValue < 0) {
            return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
        } else {
            return InputType.TYPE_CLASS_NUMBER;
        }
    }

    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        mDefaultValue = a.getInteger(index, mDefaultValue);
        return mDefaultValue;
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        setInteger(getPersistedInt(defaultValue != null ? (int) defaultValue : mDefaultValue));
    }

    @Override
    public boolean shouldDisableDependents() {
        return !checkInteger(mInteger) || super.shouldDisableDependents();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mInteger = getInteger();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setInteger(myState.mInteger);
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        int mInteger;

        SavedState(Parcel source) {
            super(source);
            mInteger = source.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mInteger);
        }
    }
}
