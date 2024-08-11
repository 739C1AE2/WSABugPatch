package com.github739c1ae2.wsapatch.preference;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github739c1ae2.wsapatch.R;
import com.google.android.material.textfield.TextInputLayout;

public class IntegerEditTextPreferenceFragment extends MaterialPreferenceDialogFragment {

    private static final String SAVE_STATE_INTEGER = "IntegerEditTextPreferenceFragment.integer";
    private EditText mEditText;
    private int mInteger;


    @NonNull
    public static IntegerEditTextPreferenceFragment newInstance(String key) {
        final IntegerEditTextPreferenceFragment
                fragment = new IntegerEditTextPreferenceFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mInteger = getIntegerEditTextPreference().getInteger();
        } else {
            mInteger = savedInstanceState.getInt(SAVE_STATE_INTEGER);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INTEGER, mInteger);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        mEditText = view.findViewById(android.R.id.edit);
        TextInputLayout inputLayout = view.findViewById(R.id.textInputLayout);

        if (mEditText == null) {
            throw new IllegalStateException("Dialog view must contain an EditText with id" +
                    " @android:id/edit");
        }

        mEditText.requestFocus();
        mEditText.setInputType(getIntegerEditTextPreference().getInputType());
        mEditText.setText(String.valueOf(mInteger));
        // 将光标放在末尾
        mEditText.setSelection(mEditText.getText().length());
        // 编辑框内容改变时，检查是否符合要求
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Integer value;
                try {
                    value = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    value = null;
                }
                if (!getIntegerEditTextPreference().checkInteger(value)) {

                    inputLayout.setError(
                            String.format(getString(R.string.integer_edittext_preference_error_message),
                                    getIntegerEditTextPreference().getMinValue(),
                                    getIntegerEditTextPreference().getMaxValue()));
                } else if (inputLayout.isErrorEnabled()) {
                    inputLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private IntegerEditTextPreference getIntegerEditTextPreference() {
        return (IntegerEditTextPreference) getPreference();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Integer value;
            try {
                value = Integer.parseInt(mEditText.getText().toString());
            } catch (NumberFormatException e) {
                value = null;
            }
            final IntegerEditTextPreference preference = getIntegerEditTextPreference();
            if (preference.callChangeListener(value)) {
                preference.setInteger(value);
            }
        }
    }
}
