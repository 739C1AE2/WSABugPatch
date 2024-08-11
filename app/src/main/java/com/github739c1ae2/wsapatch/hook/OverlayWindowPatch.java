package com.github739c1ae2.wsapatch.hook;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.github739c1ae2.wsapatch.util.XSharedPreferencesHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OverlayWindowPatch {

    private final Map<View, OverlayViewContainer> mViewMap = new HashMap<>();
    private final ClassLoader mClassLoader;

    public OverlayWindowPatch(XC_LoadPackage.LoadPackageParam lpp) {
        mClassLoader = lpp.classLoader;
        XSharedPreferencesHelper preferencesHelper = new XSharedPreferencesHelper();
        Set<String> scope = preferencesHelper.getStringSet("overlay_window_scope", new HashSet<>());
        if (!scope.isEmpty() && !scope.contains(lpp.packageName)) {
            return;
        }
        if (preferencesHelper.getBoolean("fix_overlay_window_drag", false,
                true)) {
            fix();
        }
    }

    private void fix() {
        XposedHelpers.findAndHookMethod(
                "android.view.WindowManagerImpl",
                mClassLoader,
                "addView",
                View.class, ViewGroup.LayoutParams.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        View view = (View) param.args[0];
                        if (view instanceof OverlayViewContainer) {
                            return;
                        }
                        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) param.args[1];
                        if (layoutParams.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY &&
                                layoutParams.type != WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY ||
                                (layoutParams.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
                            return;
                        }
                        OverlayViewContainer container = new OverlayViewContainer(view.getContext(), view, (WindowManager) param.thisObject, layoutParams);
                        mViewMap.put(view, container);
                        param.args[0] = container;
                    }
                });

        XposedHelpers.findAndHookMethod(
                "android.view.WindowManagerImpl",
                mClassLoader,
                "updateViewLayout",
                View.class, ViewGroup.LayoutParams.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        View view = (View) param.args[0];
                        if (view instanceof OverlayViewContainer) {
                            return;
                        }
                        OverlayViewContainer container = mViewMap.get(view);
                        if (container == null) {
                            return;
                        }
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) param.args[1];
                        param.args[0] = container;
                        param.args[1] = container.updateViewLayout(params);
                    }
                }
        );

        XC_MethodHook removeHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                View view = (View) param.args[0];
                if (view instanceof OverlayViewContainer) {
                    return;
                }
                OverlayViewContainer container = mViewMap.get(view);
                if (container == null) {
                    return;
                }
                mViewMap.remove(view);
                container.destroy();
                param.args[0] = container;
            }
        };
        XposedHelpers.findAndHookMethod("android.view.WindowManagerImpl", mClassLoader,
                "removeView", View.class, removeHook);
        XposedHelpers.findAndHookMethod("android.view.WindowManagerImpl", mClassLoader,
                "removeViewImmediate", View.class, removeHook);

        XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                if (view instanceof OverlayViewContainer) {
                    return;
                }
                OverlayViewContainer container = mViewMap.get(view);
                if (container == null) {
                    return;
                }
                container.setVisibility((int) param.args[0]);
            }
        });
    }
}
