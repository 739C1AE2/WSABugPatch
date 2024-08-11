package com.github739c1ae2.wsapatch.preference;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.github739c1ae2.wsapatch.R;

import java.util.List;

public abstract class ActivityPreference extends Preference {

    private static final String TAG = "PackageSelectPreference";
    private static final int windowWidth = 420;
    private static final int windowHeight = 620;

    public ActivityPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ActivityPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ActivityPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.activityPreferenceStyle);
    }

    protected abstract void onPutDefaultValue(@NonNull Intent intent);

    @NonNull
    protected abstract Class<? extends ActivityPreferenceActivity> getActivityClass();

    @Override
    protected void onClick() {
        // 检查是否存在相同输入的 Activity 实例
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
        boolean foundExistingTask = false;

        for (ActivityManager.AppTask task : tasks) {
            ActivityManager.RecentTaskInfo info = task.getTaskInfo();
            Intent baseIntent = info.baseIntent;

            ComponentName componentName = baseIntent.getComponent();
            if (componentName == null) {
                continue;
            }
            try {
                String className = componentName.getClassName();
                Class<?> activityClass = Class.forName(className);
                if (!ActivityPreferenceActivity.class.isAssignableFrom(activityClass)) {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            // 检查基本意图是否匹配，并且其他条件（如任务栈等）是否满足
            if (baseIntent.hasExtra(ActivityPreferenceActivity.EXTRA_KEY)
                    && getKey().equals(baseIntent.getStringExtra(ActivityPreferenceActivity.EXTRA_KEY))) {

                // 找到匹配的任务，移到前台并重用该任务
                task.moveToFront();
                foundExistingTask = true;
                break;
            }
        }

        // 如果没有找到匹配的任务，则正常启动新的 Activity
        if (!foundExistingTask) {
            Intent intent = new Intent(getContext(), getActivityClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(ActivityPreferenceActivity.EXTRA_SHARED_PREFERENCES_NAME,
                    getPreferenceManager().getSharedPreferencesName());
            intent.putExtra(ActivityPreferenceActivity.EXTRA_SHARED_PREFERENCES_MODE,
                    getPreferenceManager().getSharedPreferencesMode());
            intent.putExtra(ActivityPreferenceActivity.EXTRA_KEY, getKey());
            intent.putExtra(ActivityPreferenceActivity.EXTRA_TITLE, getTitle());
            intent.putExtra(ActivityPreferenceActivity.EXTRA_SUMMARY, getSummary());
            onPutDefaultValue(intent);

            Bundle bundle = null;
            if (getContext() instanceof Activity) {
                Rect windowBounds = ((Activity) getContext()).getWindowManager().getCurrentWindowMetrics()
                        .getBounds();
                Log.i(TAG, "onClick: windowBounds: " + windowBounds);
                Rect bounds = new Rect(windowBounds.centerX() - windowWidth / 2,
                        windowBounds.centerY() - windowHeight / 2,
                        windowBounds.centerX() + windowWidth / 2,
                        windowBounds.centerY() + windowHeight / 2);
                Log.i(TAG, "onClick: bounds: " + bounds);
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchBounds(bounds);
                bundle = options.toBundle();
            }
            getContext().startActivity(intent, bundle);
        }
    }
}
