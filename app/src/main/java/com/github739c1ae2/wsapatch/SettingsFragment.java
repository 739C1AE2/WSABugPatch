package com.github739c1ae2.wsapatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.github739c1ae2.wsapatch.preference.IntegerEditTextPreference;
import com.github739c1ae2.wsapatch.preference.IntegerEditTextPreferenceFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        try {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        } catch (SecurityException e) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.preference_error_title)
                    .setMessage(R.string.preference_error_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> requireActivity().finish())
                    .setCancelable(false)
                    .show();
        }
    }


    @Override
    @SuppressWarnings("deprecation")
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat caller,
                                             @NonNull Preference pref) {
        final DialogFragment f;
        if (pref instanceof IntegerEditTextPreference) {
            f = IntegerEditTextPreferenceFragment.newInstance(pref.getKey());
        } else {
            return false;
        }
        f.setTargetFragment(this, 0);
        f.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
        return true;
    }
}