package com.github739c1ae2.wsapatch.hook;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.TaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.github739c1ae2.wsapatch.util.XSharedPreferencesHelper;

import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class InputMethodPatch {

    private static final String ACTION_UPDATE_CLIENT_CURSOR_LOCATION =
            "com.github739c1ae2.wsapatch.ACTION_UPDATE_CLIENT_CURSOR_LOCATION";

    private final XSharedPreferencesHelper mSharedPreferences;
    private final AtomicInteger mTitleBarHeight;
    private final ClassLoader mClassLoader;


    public InputMethodPatch(ClassLoader classLoader) {
        mClassLoader = classLoader;
        mTitleBarHeight = new AtomicInteger();
        mSharedPreferences = new XSharedPreferencesHelper();
        initPreferences();
    }

    private void initPreferences() {
        mTitleBarHeight.set(mSharedPreferences.getInt("ime_offset_y", 31));

        boolean needFixPosition = mSharedPreferences.getBoolean("fix_ime_position",
                false, true);
        if (needFixPosition) {
            fixPosition();
        }

        boolean needReplaceIsInvalidEditControl = mSharedPreferences.getBoolean(
                "ime_replace_invalid_check", false, true);
        if (needReplaceIsInvalidEditControl) {
            replaceIsInvalidEditControl();
        }

        boolean needRequestCursorUpdatesFallback = mSharedPreferences.getBoolean(
                "ime_request_cursor_updates_fallback", false, true);
        if (needRequestCursorUpdatesFallback) {
            requestCursorUpdatesFallback();
        }
    }

    /**
     * 修复输入法位置偏移
     * */
    private void fixPosition() {
        XposedHelpers.findAndHookMethod(
                "com.microsoft.windows.redirection.TsfRedirectionHandler",
                mClassLoader,
                "updateClientCursorLocation",
                float.class, float.class,
                new XC_MethodHook() {
                    @Override
                    @SuppressLint("PrivateApi")
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Object activityTaskManager = XposedHelpers.callStaticMethod(
                                    Class.forName("android.app.ActivityTaskManager"),
                                    "getService");
                            TaskInfo taskInfo = (TaskInfo) XposedHelpers.callMethod(activityTaskManager,
                                    "getFocusedRootTaskInfo");
                            Rect bounds = (Rect) XposedHelpers.getObjectField(taskInfo, "bounds");
                            if (bounds != null) {
                                param.args[0] = (float) param.args[0] - bounds.left;
                                int dy = bounds.top - mTitleBarHeight.get();
                                if (dy < 0) {
                                    dy = 0;
                                }
                                param.args[1] = (float) param.args[1] - dy;
                            }

                        } catch (Throwable e) {
                            XposedBridge.log(e);
                        }
                    }
                });
    }

    /**
     * 替换 isInvalidEditControl 方法
     * */
    private void replaceIsInvalidEditControl() {
        XposedHelpers.findAndHookMethod("com.microsoft.windows.input.MicrosoftInputMethod",
                mClassLoader,
                "isInvalidEditControl",
                EditorInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        EditorInfo attribute = (EditorInfo) param.args[0];
                        param.setResult(attribute.inputType == 0);
                    }
                });
    }

    /**
     * 实现在调用 requestCursorUpdates 失败时，从 View 左下角弹出输入法
     * */
    private void requestCursorUpdatesFallback() {
        XposedHelpers.findAndHookMethod(
                "com.microsoft.windows.input.InputAccessibilityService",
                mClassLoader,
                "onServiceConnected",
                new XC_MethodHook() {
                    @SuppressLint("UnspecifiedRegisterReceiverFlag")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        AccessibilityService service = (AccessibilityService) param.thisObject;
                        IntentFilter filter = new IntentFilter(ACTION_UPDATE_CLIENT_CURSOR_LOCATION);
                        service.registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    AccessibilityNodeInfo info
                                            = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                                    if (info != null) {
                                        Rect bounds = new Rect();
                                        info.getBoundsInScreen(bounds);
                                        Application app = service.getApplication();
                                        Object redirector = XposedHelpers.callMethod(app, "getTsfRedirector");
                                        XposedHelpers.callMethod(redirector,
                                                "updateClientCursorLocation",
                                                bounds.left, bounds.bottom);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }, filter, Context.RECEIVER_NOT_EXPORTED);
                    }
                });

        XposedHelpers.findAndHookMethod("com.microsoft.windows.input.MicrosoftInputMethod",
                mClassLoader,
                "requestCursorLocation",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        InputMethodService service = (InputMethodService) param.thisObject;
                        Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                        handler.post(() -> {
                            InputConnection inputConnection = service.getCurrentInputConnection();
                            if (inputConnection == null ||
                                    !inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)) {
                                Intent intent = new Intent(ACTION_UPDATE_CLIENT_CURSOR_LOCATION);
                                intent.setPackage("com.microsoft.windows.userapp");
                                service.sendBroadcast(intent);
                            }
                            param.setResult(null);
                        });
                    }
                });
    }
}
