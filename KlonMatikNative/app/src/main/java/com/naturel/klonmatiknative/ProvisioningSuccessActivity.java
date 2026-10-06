package com.naturel.klonmatiknative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ProvisioningSuccessActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdminReceiver.configureManagedProfile(this);
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(open);
        finish();
    }
}
