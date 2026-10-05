package com.naturel.ayrialan;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_PROVISION = 1001;
    private LinearLayout root;
    private DevicePolicyManager dpm;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        userManager = (UserManager) getSystemService(USER_SERVICE);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        root.setBackgroundColor(Color.rgb(248, 250, 248));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addTitle("Ayrı Alan");
        addText("Reklamsız • sade • Android'in ayrı profil özelliğini kullanır", 14, Color.DKGRAY);

        if (isManagedProfile()) {
            renderManagedProfile();
        } else {
            renderPersonalProfile();
        }

        setContentView(scroll);
    }

    private boolean isManagedProfile() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && userManager != null
                && userManager.isManagedProfile();
    }

    private void renderPersonalProfile() {
        addCardTitle("Ana telefon");
        addText("Buradaki uygulamalar ve girişler aynen kalır. Ayrı Alan kurulduğunda ikinci bir izole profil oluşur.", 16, Color.rgb(55, 55, 55));

        boolean allowed = false;
        try {
            allowed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        } catch (Exception ignored) {
        }

        Button setup = addPrimaryButton("Ayrı Alanı Kur");
        setup.setEnabled(allowed);
        setup.setAlpha(allowed ? 1f : 0.45f);
        setup.setOnClickListener(v -> startProvisioning());

        if (!allowed) {
            addText("Bu telefonda yeni iş profili oluşturulmasına şu anda izin verilmiyor. Cihazda mevcut bir iş profili olabilir veya üretici bu özelliği kapatmış olabilir.", 14, Color.rgb(150, 60, 40));
            Button settings = addSecondaryButton("Android Ayarlarını Aç");
            settings.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "Ayarlar açılamadı.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        addDivider();
        addCardTitle("Nasıl kullanılır?");
        addStep("1", "Ayrı Alanı Kur'a bas.");
        addStep("2", "Android'in oluşturduğu iş profilini tamamla.");
        addStep("3", "Çantalı rozetli Ayrı Alan uygulamasını aç.");
        addStep("4", "Play Store'dan istediğin uygulamayı o profile kur ve ayrı hesapla giriş yap.");

        addDivider();
        addText("Not: Bu sürüm Android'in resmi profil sistemini kullanır. Kampanya/hesap sınırlarını aşmak için tasarlanmamıştır.", 13, Color.GRAY);
    }

    private void renderManagedProfile() {
        addStatus("Ayrı alan aktif");
        addText("Bu profilin uygulama verileri ana telefondan ayrıdır. Burada kurduğun uygulamalar kendi oturumunu tutar.", 16, Color.rgb(45, 70, 50));

        Button store = addPrimaryButton("Play Store'u Aç");
        store.setOnClickListener(v -> openAppMarket());

        Button refresh = addSecondaryButton("Uygulamaları Yenile");
        refresh.setOnClickListener(v -> render());

        addDivider();
        addCardTitle("Bu alandaki uygulamalar");
        showLaunchableApps();
    }

    private void startProvisioning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && !dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
                Toast.makeText(this, "Telefon yeni ayrı profile izin vermiyor.", Toast.LENGTH_LONG).show();
                return;
            }

            ComponentName admin = AdminReceiver.component(this);
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin);
            startActivityForResult(intent, REQ_PROVISION);
        } catch (Exception e) {
            Toast.makeText(this, "Profil kurulumu başlatılamadı: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void openAppMarket() {
        try {
            Intent market = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MARKET);
            startActivity(market);
        } catch (Exception first) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                if (launch != null) {
                    startActivity(launch);
                } else {
                    Toast.makeText(this, "Play Store bulunamadı.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception second) {
                Toast.makeText(this, "Uygulama mağazası açılamadı.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> infos = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL);
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo info : infos) {
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) {
                filtered.add(info);
            }
        }

        Collections.sort(filtered, Comparator.comparing(
                r -> r.loadLabel(pm).toString().toLowerCase()));

        if (filtered.isEmpty()) {
            addText("Henüz başka uygulama yok. Play Store'u açıp istediğin uygulamayı bu profile kur.", 15, Color.GRAY);
            return;
        }

        for (ResolveInfo info : filtered) {
            String label = info.loadLabel(pm).toString();
            String pkg = info.activityInfo.packageName;

            Button app = addAppButton(label);
            app.setOnClickListener(v -> {
                try {
                    Intent launch = pm.getLaunchIntentForPackage(pkg);
                    if (launch != null) {
                        startActivity(launch);
                    } else {
                        Toast.makeText(this, "Uygulama açılamadı.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Uygulama açılamadı.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(30);
        t.setTextColor(Color.rgb(30, 95, 50));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, 0, 0, dp(4));
        root.addView(t);
    }

    private void addCardTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(20);
        t.setTextColor(Color.rgb(35, 35, 35));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(22), 0, dp(8));
        root.addView(t);
    }

    private void addStatus(String s) {
        TextView t = new TextView(this);
        t.setText("✓  " + s);
        t.setTextSize(18);
        t.setTextColor(Color.rgb(30, 120, 55));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.setBackgroundColor(Color.rgb(228, 245, 232));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(20), 0, dp(10));
        root.addView(t, lp);
    }

    private void addText(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.15f);
        t.setPadding(0, dp(4), 0, dp(8));
        root.addView(t);
    }

    private Button addPrimaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(46, 125, 65));
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(14), 0, dp(6));
        root.addView(b, lp);
        return b;
    }

    private Button addSecondaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(38, 82, 48));
        b.setBackgroundColor(Color.rgb(232, 239, 233));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(8), 0, dp(4));
        root.addView(b, lp);
        return b;
    }

    private Button addAppButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(16), 0, dp(16), 0);
        b.setTextColor(Color.rgb(35, 35, 35));
        b.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(5), 0, dp(5));
        root.addView(b, lp);
        return b;
    }

    private void addStep(String number, String text) {
        TextView t = new TextView(this);
        t.setText(number + ".  " + text);
        t.setTextSize(15);
        t.setTextColor(Color.rgb(55, 55, 55));
        t.setPadding(dp(6), dp(7), 0, dp(7));
        root.addView(t);
    }

    private void addDivider() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(220, 225, 220));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, dp(22), 0, dp(4));
        root.addView(v, lp);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
