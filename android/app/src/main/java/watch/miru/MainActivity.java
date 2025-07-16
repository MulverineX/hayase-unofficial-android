package watch.miru;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import com.getcapacitor.BridgeActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BridgeActivity {
  private final Map<WebView, Dialog> popupDialogs = new HashMap<>();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(true);
      }
      // Attempt to set command line flags (may not work on all devices/versions, if at all)
      String commandLine = "--enable-blink-features=AudioVideoTracks --enable-experimental-web-platform-features";
      System.setProperty("chromium.command_line", commandLine);
    } catch (Exception e) {
      e.printStackTrace();
    }

    final WebView webView = getBridge().getWebView();

    WebSettings settings = webView.getSettings();
    settings.setSupportMultipleWindows(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    settings.setUseWideViewPort(true);
    settings.setLoadWithOverviewMode(true);

    hideSystemUI();

    // Set user agent to include AndroidTV if device supports Leanback or was launched with LEANBACK_LAUNCHER
    boolean isLeanback = getPackageManager().hasSystemFeature("android.software.leanback")
        || getPackageManager().hasSystemFeature("android.software.leanback_only");
    boolean isLeanbackIntent = false;
    if (getIntent() != null && getIntent().getCategories() != null) {
      isLeanbackIntent = getIntent().getCategories().contains("android.intent.category.LEANBACK_LAUNCHER");
    }
    if (isLeanback || isLeanbackIntent) {
      String ua = settings.getUserAgentString();
      if (!ua.contains("AndroidTV")) {
        settings.setUserAgentString(ua + " AndroidTV");
      }
    }

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        // Only inject JavaScript for allowed URLs.
        super.onPageFinished(view, url);
        if (url != null && (url.startsWith("https://hayase.app") || url.startsWith("http://localhost"))) {
          injectJavaScript(view);
        }
      }

      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse response = super.shouldInterceptRequest(view, request);
        
        // If no custom response, let the default handling occur
        if (response == null) {
          return null;
        }
        
        String url = request.getUrl().toString();
        String method = request.getMethod();
        Map<String, String> headers = response.getResponseHeaders();
        if (headers == null) {
          headers = new HashMap<>();
        }
        
        if (url.startsWith("https://graphql.anilist.co") && "OPTIONS".equals(method)) {
          headers.put("Cache-Control", "public, max-age=86400");
          headers.put("access-control-max-age", "86400");
        }
        
        // MAL CORS fix - doesn't implement CORS properly
        if (url.startsWith("https://myanimelist.net/v1/oauth2") || url.startsWith("https://api.myanimelist.net/v2/")) {
          headers.put("Access-Control-Allow-Origin", "*");
          headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
          headers.put("Access-Control-Allow-Headers", "*");
          headers.put("Access-Control-Allow-Credentials", "true");
          
          if ("OPTIONS".equals(method) && response.getStatusCode() == 405) {
            return new WebResourceResponse(
                response.getMimeType(),
                response.getEncoding(),
                200,
                "OK",
                headers,
                response.getData()
            );
          }
        }
        
        return new WebResourceResponse(
            response.getMimeType(),
            response.getEncoding(),
            response.getStatusCode(),
            response.getReasonPhrase(),
            headers,
            response.getData()
        );
      }
    });
  
    // make js window.open() work just like in a browser
    webView.setWebChromeClient(new WebChromeClient() {
      // these 2 overrides are needed to re-implement fullscreen mode which breaks by overriding chromeClient
      private View mCustomView;
      private WebChromeClient.CustomViewCallback mCustomViewCallback;
      private int mOriginalSystemUiVisibility;

      @Override
      public void onShowCustomView(View view, CustomViewCallback callback) {
        if (mCustomView != null) {
          callback.onCustomViewHidden();
          return;
        }
        mCustomView = view;
        mCustomViewCallback = callback;

        // Add the custom view to the main content view, not decor view, to preserve insets
        ViewGroup contentView = findViewById(android.R.id.content);
        contentView.addView(mCustomView, new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT));
      }

      @Override
      public void onHideCustomView() {
        if (mCustomView == null) {
          return;
        }
        ((ViewGroup) mCustomView.getParent()).removeView(mCustomView);
        mCustomView = null;
        if (mCustomViewCallback != null) {
          mCustomViewCallback.onCustomViewHidden();
        }
      }

      @Override
      public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        WebView popupWebView = new WebView(MainActivity.this);
        WebSettings webSettings = popupWebView.getSettings();
        
        WebSettings mainSettings = view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(mainSettings.getDomStorageEnabled()); // AL requires this LOL
        webSettings.setAllowContentAccess(mainSettings.getAllowContentAccess());
        
        // Set the same WebChromeClient as the main WebView so window.close() works, IMPORTANT
        popupWebView.setWebChromeClient(this);
        popupWebView.setWebViewClient(new WebViewClient());

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(popupWebView);
        dialog.show();

        popupDialogs.put(popupWebView, dialog);

        dialog.setOnDismissListener(dialogInterface -> {
          popupWebView.destroy();
          popupDialogs.remove(popupWebView);
        });

        // The system needs a WebView to be returned for the window creation.
        // We pass our new popup WebView back via the result message.
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(popupWebView);
        resultMsg.sendToTarget();
        // We return true to indicate that we have handled the new window creation.
        return true;
      }
      // Also handle closing the popup window from JavaScript (e.g., window.close()), might not be needed
      @Override
      public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
        Dialog dialog = popupDialogs.get(window);
        if (dialog != null && dialog.isShowing()) {
          dialog.dismiss();
        }
      }
    });
  }

  // Hides the status and navigation bars for immersive fullscreen
  private void hideSystemUI() {
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
      | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
      | View.SYSTEM_UI_FLAG_FULLSCREEN
    );
  }

  private void injectJavaScript(WebView webView) {
    try {
      InputStream inputStream = getAssets().open("public/preload.js");
      byte[] buffer = new byte[inputStream.available()];
      inputStream.read(buffer);
      inputStream.close();

      String jsCode = new String(buffer, StandardCharsets.UTF_8);

      // The 'null' second argument is a callback that we don't need here.
      webView.evaluateJavascript(jsCode, null);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}