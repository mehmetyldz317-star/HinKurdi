package com.ilmizan.kitapligim;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE=4301;
    private static final int REQUEST_OPEN=4302;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingFileText;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16,61,52));
        getWindow().setNavigationBarColor(Color.rgb(248,246,240));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        webView=new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=callback;
                try { startActivityForResult(params.createIntent(),REQUEST_OPEN); return true; }
                catch(Exception ex){ fileCallback=null; return false; }
            }
        });
        WebSettings settings=webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        webView.addJavascriptInterface(new Object(){
            @JavascriptInterface public void saveDocument(String name,String mime,String content){
                runOnUiThread(()->openSaveChooser(name,mime,content));
            }
        },"ILMIZAN");
        webView.setBackgroundColor(Color.rgb(248,246,240));
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    private void openSaveChooser(String name,String mime,String content){
        pendingFileText=content;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime.split(";")[0]);
        intent.putExtra(Intent.EXTRA_TITLE,name);
        try{startActivityForResult(intent,REQUEST_SAVE);}
        catch(Exception ex){Toast.makeText(this,"Kaydetme penceresi açılamadı",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_OPEN){
            if(fileCallback!=null){
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));
                fileCallback=null;
            }
        }else if(requestCode==REQUEST_SAVE){
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&pendingFileText!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new Exception("Dosya açılamadı");
                    out.write(pendingFileText.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this,"Yedek kaydedildi",Toast.LENGTH_SHORT).show();
                }catch(Exception ex){Toast.makeText(this,"Dosya kaydedilemedi",Toast.LENGTH_LONG).show();}
            }
            pendingFileText=null;
        }
    }
    @Override public void onBackPressed(){
        if(webView!=null)webView.evaluateJavascript("window.ilmizanBack ? window.ilmizanBack() : null;",null);
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}
        if(webView!=null){webView.destroy();webView=null;}
        super.onDestroy();
    }
}
