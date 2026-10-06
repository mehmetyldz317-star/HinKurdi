package com.naturel.klonmatiknative;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Collections;

public class AdminReceiver extends android.app.admin.DeviceAdminReceiver {
    public static final String ACTION_INSTALL_CLONE =
            "com.naturel.klonmatiknative.action.INSTALL_CLONE";

    public static ComponentName component(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

    public static void configureManagedProfile(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = component(context);

        try {
            if (!dpm.isProfileOwnerApp(context.getPackageName())) return;

            dpm.setProfileName(admin, "KlonMatik");
            dpm.setProfileEnabled(admin);

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_INSTALL_CLONE);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            dpm.addCrossProfileIntentFilter(
                    admin,
                    filter,
                    DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dpm.setCrossProfilePackages(
                        admin,
                        Collections.singleton(context.getPackageName()));
            }

            context.getPackageManager().setComponentEnabledSetting(
                    new ComponentName(context, InstallProxyActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        configureManagedProfile(context);

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(open);
    }
}
