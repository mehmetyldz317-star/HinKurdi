package com.naturel.ayrialan;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends android.app.admin.DeviceAdminReceiver {

    public static ComponentName component(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = component(context);

        try {
            if (dpm.isProfileOwnerApp(context.getPackageName())) {
                dpm.setProfileName(admin, "Ayrı Alan");
                dpm.setProfileEnabled(admin);
            }
        } catch (Exception ignored) {
        }

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(open);
    }
}
