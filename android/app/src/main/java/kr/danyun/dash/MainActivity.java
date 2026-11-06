package kr.danyun.dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.EditText;

/** 대시보드 주소(비밀 열쇠 포함)는 앱 안에 넣지 않고, 처음 실행할 때 한 번 붙여넣어 저장한다. */
public class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> fileCb;
  private SharedPreferences prefs;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    prefs = getSharedPreferences("dash", MODE_PRIVATE);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setSupportMultipleWindows(false);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        String host = r.getUrl().getHost();
        if (host != null && (host.endsWith("google.com") || host.endsWith("googleusercontent.com"))) return false;
        startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        fileCb = cb;
        try { startActivityForResult(p.createIntent(), 7); } catch (Exception e) { fileCb = null; return false; }
        return true;
      }
    });
    String url = prefs.getString("url", "");
    if (url.isEmpty()) askUrl(); else web.loadUrl(url);
  }

  private void askUrl() {
    final EditText in = new EditText(this);
    in.setHint("https://script.google.com/macros/s/.../exec?k=...");
    in.setText(prefs.getString("url", ""));
    new AlertDialog.Builder(this)
      .setTitle("대시보드 주소 붙여넣기")
      .setMessage("Claude가 보내 드린 링크(끝에 ?k= 가 붙은 주소)를 붙여넣어 주세요.")
      .setView(in).setCancelable(false)
      .setPositiveButton("저장", (d, w) -> {
        String u = in.getText().toString().trim();
        if (!u.startsWith("https://")) { askUrl(); return; }
        prefs.edit().putString("url", u).apply();
        web.loadUrl(u);
      }).show();
  }

  @Override protected void onActivityResult(int req, int res, Intent data) {
    if (req == 7 && fileCb != null) {
      fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
      fileCb = null;
    } else super.onActivityResult(req, res, data);
  }

  @Override public void onBackPressed() {
    if (web.canGoBack()) web.goBack(); else super.onBackPressed();
  }

  @Override public boolean onKeyLongPress(int keyCode, android.view.KeyEvent e) {
    if (keyCode == android.view.KeyEvent.KEYCODE_BACK) { askUrl(); return true; }
    return super.onKeyLongPress(keyCode, e);
  }
}
