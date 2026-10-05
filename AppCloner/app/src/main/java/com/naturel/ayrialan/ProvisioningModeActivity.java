package com.naturel.ayrialan;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

public class ProvisioningModeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<Integer> allowed =
                getIntent().getIntegerArrayListExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES);

        int mode = DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE;
        if (allowed != null
                && !allowed.isEmpty()
                && !allowed.contains(DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE)
                && allowed.contains(
                        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE)) {
            mode = DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE;
        }

        Intent result = new Intent();
        result.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, mode);
        setResult(RESULT_OK, result);
        finish();
    }
}
