package com.naturel.klonmatiknative;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class InstallProxyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserManager um = (UserManager) getSystemService(USER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && um != null
                && !um.isManagedProfile()) {
            finish();
            return;
        }

        AdminReceiver.configureManagedProfile(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(
                    this,
                    "Bir kez 'Bu kaynaktan izin ver' seçeneğini aç. Sonra ana profilde tekrar Klonla'ya bas.",
                    Toast.LENGTH_LONG).show();
            Intent settings = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            finish();
            return;
        }

        ClipData clip = getIntent().getClipData();
        String packageName = getIntent().getStringExtra("clone_package");
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "APK verisi alınamadı.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if (packageName != null) params.setAppPackageName(packageName);

            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri == null) continue;

                String entry = i == 0 ? "base.apk" : "split_" + i + ".apk";
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = session.openWrite(entry, 0, -1)) {
                    if (in == null) throw new IllegalStateException("APK okunamadı");
                    byte[] buffer = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    session.fsync(out);
                }
            }

            Intent callback = new Intent(this, InstallResultReceiver.class);
            callback.putExtra("clone_package", packageName);
            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    sessionId,
                    callback,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            IntentSender sender = pi.getIntentSender();
            session.commit(sender);
            session.close();

            Toast.makeText(
                    this,
                    "Android kurulum ekranı açılırsa onayla.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Klon aktarımı başarısız: " + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " / " + e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
