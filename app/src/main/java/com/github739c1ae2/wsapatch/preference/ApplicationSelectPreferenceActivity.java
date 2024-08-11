package com.github739c1ae2.wsapatch.preference;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github739c1ae2.wsapatch.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplicationSelectPreferenceActivity extends ActivityPreferenceActivity {

    private Set<String> mSelectedApps;
    private Set<String> mDefaultApps;
    private AppListAdapter mAdapter;

    @Override
    @SuppressWarnings("unchecked")
    protected void onGetDefaultValue(@NonNull Intent intent) {
        Set<String> defaultApps = intent.getSerializableExtra(EXTRA_DEFAULT_VALUE, HashSet.class);
        mDefaultApps = getSharedPreferences().getStringSet(getKey(),
                defaultApps != null ? defaultApps : new HashSet<>());
    }

    @Override
    protected void onInitialize() {
        setContentView(R.layout.activity_application_select_preference);
        if (!TextUtils.isEmpty(getSummary())) {
            ((TextView) findViewById(R.id.tv_summary)).setText(getSummary());
        }
        RecyclerView recyclerView = findViewById(R.id.rv_list);
        Chip systemAppChip = findViewById(R.id.chip_system_app);
        mSelectedApps = new HashSet<>();
        mAdapter = new AppListAdapter();
        recyclerView.setAdapter(mAdapter);
        systemAppChip.setOnCheckedChangeListener((buttonView, isChecked) -> fetchApps(isChecked));

        if (checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED) {
            initSelectedApps();
            fetchApps(systemAppChip.isChecked());
        } else {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                    initSelectedApps();
                    fetchApps(systemAppChip.isChecked());
                }
            }).launch(Manifest.permission.QUERY_ALL_PACKAGES);
        }
    }

    private void initSelectedApps() {
        List<ApplicationInfo> installedApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : installedApps) {
            if (mDefaultApps.contains(appInfo.packageName)) {
                mSelectedApps.add(appInfo.packageName);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchApps(boolean includeSystemApps) {
        new Thread(() -> {
            List<AppItem> appItems = new ArrayList<>();
            List<ApplicationInfo> installedApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInfo : installedApps) {
                if (!includeSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }
                AppItem appItem = new AppItem(appInfo, mSelectedApps.contains(appInfo.packageName));
                appItems.add(appItem);
            }
            runOnUiThread(() -> {
                mAdapter.setAppItemList(appItems);
                mAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private static class AppItem {

        private final ApplicationInfo appInfo;
        private boolean isSelected;

        public AppItem(ApplicationInfo appInfo, boolean isSelected) {
            this.appInfo = appInfo;
            this.isSelected = isSelected;
        }

        public ApplicationInfo getAppInfo() {
            return appInfo;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {
        private List<AppItem> mAppItemList;

        public void setAppItemList(List<AppItem> appItemList) {
            mAppItemList = appItemList;
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_app,
                    parent, false);
            return new AppViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            AppItem item = mAppItemList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mAppItemList == null ? 0 : mAppItemList.size();
        }

        class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            TextView appName;
            TextView pkgName;
            MaterialCardView cardView;
            ImageView icon;

            AppViewHolder(View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                appName = itemView.findViewById(R.id.tv_app_name);
                pkgName = itemView.findViewById(R.id.tv_pkg_name);
                icon = itemView.findViewById(R.id.iv_icon);

                itemView.setOnClickListener(this);
            }

            void bind(@NonNull AppItem item) {
                appName.setText(item.getAppInfo().loadLabel(getPackageManager()).toString());
                pkgName.setText(item.getAppInfo().packageName);
                icon.setImageDrawable(item.getAppInfo().loadIcon(getPackageManager()));
                cardView.setChecked(item.isSelected());
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AppItem item = mAppItemList.get(position);
                    item.setSelected(!item.isSelected());
                    cardView.setChecked(item.isSelected());
                    if (item.isSelected()) {
                        mSelectedApps.add(item.getAppInfo().packageName);
                    } else {
                        mSelectedApps.remove(item.getAppInfo().packageName);
                    }
                    getEditor().putStringSet(getKey(), mSelectedApps).apply();
                }
            }
        }
    }
}
