package com.github739c1ae2.wsapatch.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookMain implements IXposedHookLoadPackage {


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {

        if ("com.microsoft.windows.userapp".equals(lpp.packageName)) {
            new InputMethodPatch(lpp.classLoader);
            new SettingsRedirectionPatch(lpp.classLoader);
        }
        new ContentDescriptionPatch(lpp);
        new OverlayWindowPatch(lpp);
    }
}
