package com.naturel.klonmatiknative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdminReceiver.configureManagedProfile(this);
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
