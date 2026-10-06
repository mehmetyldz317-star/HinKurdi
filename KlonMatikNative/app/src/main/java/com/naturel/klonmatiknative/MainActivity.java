package com.naturel.klonmatiknative;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout root;
    private UserManager userManager;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = (UserManager) getSystemService(USER_SERVICE);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    private boolean isManagedProfile() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && userManager != null
                && userManager.isManagedProfile();
    }

    private boolean hasNativeProfile() {
        try {
            CrossProfileApps cross = getSystemService(CrossProfileApps.class);
            return cross != null && !cross.getTargetUserProfiles().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        root.setBackgroundColor(Color.rgb(248, 250, 248));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addTitle("KlonMatik Native");
        addText("Sanal motor yok • gerçek Android profili • ayrı uygulama verisi",
                14, Color.DKGRAY);

        if (isManagedProfile()) {
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, InstallProxyActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            AdminReceiver.configureManagedProfile(this);
            renderManaged();
        } else {
            // Local copy must not consume clone-install intents; they should cross
            // into the managed profile where this component is enabled.
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, InstallProxyActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            renderPersonal();
        }

        setContentView(scroll);
    }

    private void renderPersonal() {
        if (!hasNativeProfile()) {
            addStatus("Yerel klon alanı henüz kurulmadı", false);
            addText(
                    "Bir kez Android iş profili oluşturulur. Sonra telefonda zaten yüklü uygulamaları seçip ikinci profile aktarabilirsin.",
                    16, Color.rgb(55, 55, 55));

            Button setup = addPrimaryButton("Yerel Klon Alanını Kur");
            setup.setOnClickListener(v -> provisionProfile());

            addText(
                    "Kurulum Android'in kendi profil ekranıdır. Play Store hesabı gerekmez.",
                    13, Color.GRAY);
            return;
        }

        addStatus("Yerel klon alanı hazır", true);
        addText(
                "Aşağıdan telefonda kurulu uygulamayı seç. APK yeniden indirilmeden orijinal imzasıyla ikinci profile aktarılır.",
                15, Color.rgb(50, 65, 50));

        addSection("Yüklü uygulamalar");
        showPersonalApps();
    }

    private void renderManaged() {
        addStatus("Klon profili aktif", true);
        addText(
                "Burası ana telefondan ayrı Android veri alanıdır. Buradaki uygulamaların oturumları ana kopyadan bağımsızdır.",
                15, Color.rgb(50, 65, 50));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Button permission = addPrimaryButton("Klon Kurulum İznini Aç");
            permission.setOnClickListener(v -> {
                Intent i = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            });
            addText(
                    "Bunu yalnızca bir kez açman gerekir. Sonra ana profilde Klonla'ya bastığında sistem kurulumunu onaylayabilirsin.",
                    13, Color.GRAY);
        } else {
            addStatus("Klon kurulum izni hazır", true);
        }

        addSection("Bu profildeki uygulamalar");
        showManagedApps();
    }

    private void provisionProfile() {
        try {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)) {
                Toast.makeText(
                        this,
                        "Bu telefon Android iş profilini desteklemiyor.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!dpm.isProvisioningAllowed(
                    DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
                Toast.makeText(
                        this,
                        "Yeni iş profiline izin verilmiyor. Telefonda başka bir iş profili varsa önce onu kaldırmak gerekebilir.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    AdminReceiver.component(this));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.putExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_ALLOW_OFFLINE,
                        true);
            }

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Profil kurulumu başlatılamadı: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPersonalApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> infos =
                pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL);
        List<ResolveInfo> apps = new ArrayList<>();

        for (ResolveInfo r : infos) {
            if (r.activityInfo == null) continue;
            String pkg = r.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName())) continue;

            ApplicationInfo ai = r.activityInfo.applicationInfo;
            if (ai == null) continue;

            // Hide core system launchers/settings; updated system apps are still
            // allowed if the user explicitly sees them as normal launchable apps.
            boolean pureSystem =
                    (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
            if (pureSystem) continue;

            apps.add(r);
        }

        Collections.sort(
                apps,
                Comparator.comparing(
                        x -> x.loadLabel(pm).toString().toLowerCase()));

        if (apps.isEmpty()) {
            addText("Klonlanabilir uygulama bulunamadı.", 14, Color.GRAY);
            return;
        }

        String lastPkg = "";
        for (ResolveInfo r : apps) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(lastPkg)) continue;
            lastPkg = pkg;

            String label = r.loadLabel(pm).toString();
            Button b = addAppButton(label + "   •   Klonla");
            b.setOnClickListener(v -> exportAndSend(pkg, label));
        }
    }

    private void showManagedApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos =
                pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL);

        int count = 0;
        for (ResolveInfo r : infos) {
            if (r.activityInfo == null) continue;
            String pkg = r.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName())) continue;

            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) continue;

            String label = r.loadLabel(pm).toString();
            Button b = addAppButton(label + "   •   Aç");
            b.setOnClickListener(v -> {
                try {
                    startActivity(pm.getLaunchIntentForPackage(pkg));
                } catch (Exception e) {
                    Toast.makeText(
                            this, "Uygulama açılamadı.", Toast.LENGTH_SHORT).show();
                }
            });
            count++;
        }

        if (count == 0) {
            addText(
                    "Henüz klon yok. Ana profilde KlonMatik Native'i açıp bir uygulama seç.",
                    14, Color.GRAY);
        }
    }

    private void exportAndSend(String packageName, String label) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);

            File shareRoot = new File(getCacheDir(), "share/" + packageName);
            deleteRecursive(shareRoot);
            if (!shareRoot.mkdirs() && !shareRoot.isDirectory()) {
                throw new IllegalStateException("Geçici klasör oluşturulamadı");
            }

            List<File> files = new ArrayList<>();
            File base = new File(shareRoot, "base.apk");
            copyFile(new File(ai.sourceDir), base);
            files.add(base);

            if (ai.splitSourceDirs != null) {
                for (int i = 0; i < ai.splitSourceDirs.length; i++) {
                    File split = new File(shareRoot, "split_" + i + ".apk");
                    copyFile(new File(ai.splitSourceDirs[i]), split);
                    files.add(split);
                }
            }

            String authority = getPackageName() + ".files";
            ClipData clip = null;
            for (File f : files) {
                Uri uri = FileProvider.getUriForFile(this, authority, f);
                if (clip == null) {
                    clip = ClipData.newUri(
                            getContentResolver(), "Klon APK", uri);
                } else {
                    clip.addItem(new ClipData.Item(uri));
                }
            }

            Intent send = new Intent(AdminReceiver.ACTION_INSTALL_CLONE);
            send.addCategory(Intent.CATEGORY_DEFAULT);
            send.setPackage(getPackageName());
            send.putExtra("clone_package", packageName);
            send.putExtra("clone_label", label);
            send.setClipData(clip);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(send);

            Toast.makeText(
                    this,
                    label + " klon profiline aktarılıyor.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Klon hazırlanamadı: " + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " / " + e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void copyFile(File source, File target) throws Exception {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[128 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            out.getFD().sync();
        }
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }

    private void addTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(29);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Color.rgb(28, 100, 55));
        root.addView(t);
    }

    private void addSection(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(20);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Color.rgb(35, 35, 35));
        t.setPadding(0, dp(24), 0, dp(8));
        root.addView(t);
    }

    private void addStatus(String s, boolean ok) {
        TextView t = new TextView(this);
        t.setText((ok ? "✓  " : "•  ") + s);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(ok ? Color.rgb(35, 125, 60) : Color.rgb(120, 90, 30));
        t.setPadding(dp(12), dp(13), dp(12), dp(13));
        t.setBackgroundColor(
                ok ? Color.rgb(230, 246, 234) : Color.rgb(250, 244, 224));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(18), 0, dp(8));
        root.addView(t, lp);
    }

    private void addText(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.15f);
        t.setPadding(0, dp(6), 0, dp(8));
        root.addView(t);
    }

    private Button addPrimaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(45, 125, 67));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(12), 0, dp(5));
        root.addView(b, lp);
        return b;
    }

    private Button addAppButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setTextColor(Color.rgb(35, 35, 35));
        b.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.setMargins(0, dp(4), 0, dp(4));
        root.addView(b, lp);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
