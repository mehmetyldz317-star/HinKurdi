package com.naturel.ayrialan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
