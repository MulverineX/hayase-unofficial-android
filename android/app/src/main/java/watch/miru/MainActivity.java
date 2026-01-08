package app.hayase;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSArray;

import net.hampoelz.capacitor.nodejs.CapacitorNodeJSPlugin;
import net.hampoelz.capacitor.nodejs.CapacitorNodeJS;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BridgeActivity {
  private static final String TAG = "HAYASE-NATIVE";
  private final Map<WebView, Dialog> popupDialogs = new HashMap<>();
  protected RequestQueue queue = null;

  private void startNodeEngineWithCustomArgs() {
    Log.d(TAG, "startNodeEngineWithCustomArgs() called");
    try {
      Log.d(TAG, "Getting CapacitorNodeJS plugin instance...");
      CapacitorNodeJSPlugin capacitorNodeJS = (CapacitorNodeJSPlugin) getBridge().getPlugin("CapacitorNodeJS")
          .getInstance();
      Log.d(TAG, "CapacitorNodeJS plugin instance obtained");

      Field f = CapacitorNodeJSPlugin.class.getDeclaredField("implementation");
      f.setAccessible(true);
      CapacitorNodeJS implementation = (CapacitorNodeJS) f.get(capacitorNodeJS);
      Log.d(TAG, "Got CapacitorNodeJS implementation via reflection");

      String[] nodeArgs = new String[] {
        "--disallow-code-generation-from-strings",
        "--disable-proto=throw",
        "--frozen-intrinsics"
      };
      Log.d(TAG, "Node args: " + String.join(", ", nodeArgs));

      Method m = CapacitorNodeJS.class.getDeclaredMethod(
          "startEngine",
          com.getcapacitor.PluginCall.class,
          String.class,
          String.class,
          String[].class,
          Map.class);
      m.setAccessible(true);
      Log.d(TAG, "Invoking startEngine method...");
      m.invoke(implementation, null, "nodejs", null, nodeArgs, new HashMap<>());
      Log.i(TAG, "NodeJS engine started successfully with custom args");
    } catch (Exception e) {
      Log.e(TAG, "Failed to start NodeJS engine with custom args", e);
    }
  }

  @Override
  public void onDestroy() {
    try {
      CapacitorNodeJSPlugin capacitorNodeJS = (CapacitorNodeJSPlugin) getBridge().getPlugin("CapacitorNodeJS")
          .getInstance();
      Field f = CapacitorNodeJSPlugin.class.getDeclaredField("implementation");
      f.setAccessible(true);
      CapacitorNodeJS implementation = (CapacitorNodeJS) f.get(capacitorNodeJS);
      Method m = CapacitorNodeJS.class.getDeclaredMethod("sendMessage", String.class, String.class, JSArray.class);
      m.setAccessible(true);
      m.invoke(implementation, "EVENT_CHANNEL", "destroy", new JSArray());
      Log.i("Destroy", "Sent destroy message to NodeJS");
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      Log.e("Destroy", "Failed to send destroy message to NodeJS", e);
    }

    super.onDestroy();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate() started");
    Log.d(TAG, "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
    Log.d(TAG, "Android version: " + android.os.Build.VERSION.RELEASE + " (SDK " + android.os.Build.VERSION.SDK_INT + ")");
    Log.d(TAG, "Supported ABIs: " + String.join(", ", android.os.Build.SUPPORTED_ABIS));

    this.queue = Volley.newRequestQueue(this);
    Log.d(TAG, "Registering plugins...");
    registerPlugin(MediaNotificationPlugin.class);
    Log.d(TAG, "Registered MediaNotificationPlugin");
    registerPlugin(EngagePlugin.class);
    Log.d(TAG, "Registered EngagePlugin");
    registerPlugin(FilesystemPlugin.class);
    Log.d(TAG, "Registered FilesystemPlugin");

    Log.d(TAG, "Calling super.onCreate()...");
    super.onCreate(savedInstanceState);
    Log.d(TAG, "super.onCreate() completed");

    Log.d(TAG, "Starting NodeJS engine...");
    startNodeEngineWithCustomArgs();

    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(true);
        Log.d(TAG, "WebView debugging enabled");
      }
      // Attempt to set command line flags (may not work on all devices/versions, if
      // at all)
      String commandLine = "--enable-blink-features=AudioVideoTracks --enable-experimental-web-platform-features";
      System.setProperty("chromium.command_line", commandLine);
    } catch (Exception e) {
      Log.e(TAG, "Failed to set WebView flags", e);
      e.printStackTrace();
    }

    final WebView webView = getBridge().getWebView();
    Log.d(TAG, "Got WebView from bridge");

    AppUpdater.downloadAndInstallApk(this, "https://api.hayase.watch/latest");

    WebSettings settings = webView.getSettings();
    settings.setSupportMultipleWindows(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    settings.setUseWideViewPort(true);
    settings.setLoadWithOverviewMode(true);
    Log.d(TAG, "WebView settings configured");

    hideSystemUI();

    // Set user agent to include AndroidTV if device supports Leanback or was
    // launched with LEANBACK_LAUNCHER
    boolean isLeanback = getPackageManager().hasSystemFeature("android.software.leanback")
        || getPackageManager().hasSystemFeature("android.software.leanback_only");
    boolean isLeanbackIntent = false;
    if (getIntent() != null && getIntent().getCategories() != null) {
      isLeanbackIntent = getIntent().getCategories().contains("android.intent.category.LEANBACK_LAUNCHER");
    }
    Log.d(TAG, "Leanback feature: " + isLeanback + ", Leanback intent: " + isLeanbackIntent);
    if (isLeanback || isLeanbackIntent) {
      String ua = settings.getUserAgentString();
      if (!ua.contains("AndroidTV")) {
        settings.setUserAgentString(ua + " AndroidTV");
        Log.d(TAG, "Added AndroidTV to user agent");
      }
    }

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        // Inject Worker override EARLY - before page scripts run
        // This must happen before JASSUB creates its worker
        if (url != null && (url.startsWith("https://hayase.app") || url.startsWith("http://localhost"))) {
          injectEarlyPatches(view);
        }
      }

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
        if (request == null || request.getUrl() == null) {
          return super.shouldInterceptRequest(view, request);
        }

        String urlString = request.getUrl().toString();

        // Log requests for debugging JASSUB interception
        if (urlString.contains("jassub") || urlString.contains("__jassub") || urlString.contains(".wasm")) {
          Log.d(TAG, "WebView request: " + urlString);
        }

        // Intercept JASSUB asset requests to serve local 1.8.8-compatible versions
        // The Worker constructor override redirects JASSUB worker to our marker URL
        boolean isJassubWorkerJs = urlString.contains("__jassub_worker_intercept__.js");
        // WASM files requested by the worker (relative to worker location or absolute)
        boolean isJassubWasmModern = urlString.contains("jassub-worker-modern.wasm");
        boolean isJassubWasm = urlString.contains("jassub-worker.wasm") && !isJassubWasmModern;
        // Default fallback font
        boolean isDefaultFont = urlString.contains("default.woff2") && urlString.contains("jassub");

        if (isJassubWorkerJs || isJassubWasm || isJassubWasmModern || isDefaultFont) {
          Log.i(TAG, "Intercepting JASSUB asset: workerJs=" + isJassubWorkerJs + " wasm=" + isJassubWasm +
              " wasmModern=" + isJassubWasmModern + " font=" + isDefaultFont + " url=" + urlString);

          String assetPath = "jassub/";
          String mimeType;

          if (isJassubWorkerJs) {
            assetPath += "jassub-worker.js";
            mimeType = "application/javascript";
          } else if (isJassubWasmModern) {
            assetPath += "jassub-worker-modern.wasm";
            mimeType = "application/wasm";
          } else if (isJassubWasm) {
            assetPath += "jassub-worker.wasm";
            mimeType = "application/wasm";
          } else {
            assetPath += "default.woff2";
            mimeType = "font/woff2";
          }

          try {
            InputStream stream = getAssets().open(assetPath);
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Cache-Control", "public, max-age=31536000");
            Log.d(TAG, "Intercepted JASSUB asset: " + urlString + " -> " + assetPath);
            return new WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, stream);
          } catch (IOException e) {
            Log.e(TAG, "Failed to load JASSUB asset: " + assetPath, e);
          }
        }

        boolean isMAL = urlString.startsWith("https://myanimelist.net/v1/oauth2")
            || urlString.startsWith("https://api.myanimelist.net/v2/");

        boolean isAL = urlString.startsWith("https://graphql.anilist.co");

        if (!isMAL && !isAL) {
          return super.shouldInterceptRequest(view, request);
        }

        boolean isOptions = "OPTIONS".equals(request.getMethod());

        if (isAL && !isOptions) {
          return super.shouldInterceptRequest(view, request);
        }

        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", "*");
        responseHeaders.put("Cache-Control", "public, max-age=86400");
        responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        responseHeaders.put("access-control-max-age", "86400");
        responseHeaders.put("Access-Control-Allow-Headers", "*");
        responseHeaders.put("Access-Control-Allow-Credentials", "true");

        // Override MAL and AL CORS preflight requests
        if (isOptions) {
          return new WebResourceResponse("application/json", "UTF-8", 200, "OK", responseHeaders, null);
        }

        // Rewrite only MAL responses
        HttpURLConnection connection = null;
        try {
          String originalUrl = urlString;
          URL url;
          String requestBody = null;

          // Extract query parameters and convert them to request body for POST/PATCH
          // requests, because Android requests don't contain the body in the
          // request object... bruh
          if ("POST".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) {
            java.net.URI uri = new java.net.URI(originalUrl);
            String query = uri.getQuery();

            if (query != null && !query.isEmpty()) {
              // Remove query parameters from URL for the actual request
              String baseUrl = originalUrl.substring(0, originalUrl.indexOf('?'));
              url = new URL(baseUrl);

              // Use the query string as the request body
              requestBody = query;
            } else {
              url = new URL(originalUrl);
            }
          } else {
            url = new URL(originalUrl);
          }

          connection = (HttpURLConnection) url.openConnection();

          Map<String, String> requestHeaders = request.getRequestHeaders();
          for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
          }

          // Set the request method
          connection.setRequestMethod(request.getMethod());

          // Send the request body for POST/PATCH requests
          if (requestBody != null && ("POST".equals(request.getMethod()) || "PATCH".equals(request.getMethod()))) {
            connection.setDoOutput(true);

            // Write the request body
            try (java.io.OutputStream os = connection.getOutputStream()) {
              byte[] input = requestBody.getBytes("UTF-8");
              os.write(input, 0, input.length);
            }
          }

          int statusCode = connection.getResponseCode();
          String reasonPhrase = connection.getResponseMessage();
          String mimeType = connection.getContentType();
          String encoding = connection.getContentEncoding();

          // Use getErrorStream() for errors, getInputStream() for success
          InputStream responseStream;
          if (statusCode >= 200 && statusCode < 300) {
            responseStream = connection.getInputStream();
          } else {
            responseStream = connection.getErrorStream();
            // If errorStream is also null, create an empty stream
            if (responseStream == null) {
              responseStream = new java.io.ByteArrayInputStream(new byte[0]);
            }
          }

          Map<String, List<String>> headerFields = connection.getHeaderFields();
          for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() != null) {
              responseHeaders.put(entry.getKey(), String.join(", ", entry.getValue()));
            }
          }

          return new WebResourceResponse(
              mimeType != null ? mimeType : "application/json",
              encoding != null ? encoding : "UTF-8",
              statusCode,
              reasonPhrase,
              responseHeaders,
              responseStream);

        } catch (Exception e) {
          Log.e("WebViewClient", "Error occurred while intercepting request", e);
          if (connection != null) {
            connection.disconnect();
          }
          return super.shouldInterceptRequest(view, request);
        }
      }

      @Override
      public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          Log.e("WebView", "Render process gone for " + view.getUrl() + ". Did it crash? " + detail.didCrash());
        }

        if (view != null) {
          view.reload();
        }

        return true; // Prevent the app from crashing
      }
    });

    Log.d(TAG, "Setting up WebChromeClient...");
    // make js window.open() work just like in a browser
    webView.setWebChromeClient(new WebChromeClient() {
      // these 2 overrides are needed to re-implement fullscreen mode which breaks by
      // overriding chromeClient
      private View mCustomView;
      private WebChromeClient.CustomViewCallback mCustomViewCallback;
      private int mOriginalSystemUiVisibility;

      // Forward WebView console messages to logcat for debugging
      @Override
      public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String msg = consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId();
        switch (consoleMessage.messageLevel()) {
          case ERROR:
            Log.e("HAYASE-WEBVIEW", msg);
            break;
          case WARNING:
            Log.w("HAYASE-WEBVIEW", msg);
            break;
          default:
            Log.d("HAYASE-WEBVIEW", msg);
            break;
        }
        return true;
      }

      @Override
      public void onShowCustomView(View view, CustomViewCallback callback) {
        if (mCustomView != null) {
          callback.onCustomViewHidden();
          return;
        }
        mCustomView = view;
        mCustomViewCallback = callback;

        // Add the custom view to the main content view, not decor view, to preserve
        // insets
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

        // Set the same WebChromeClient as the main WebView so window.close() works,
        // IMPORTANT
        popupWebView.setWebChromeClient(this);
        popupWebView.setWebViewClient(new WebViewClient() {
          @Override
          public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
              Log.e("WebViewPopup", "Render process gone. Did it crash? " + detail.didCrash());
            }

            Dialog dialog = popupDialogs.get(view);
            if (dialog != null) {
              dialog.dismiss();
            }

            return true; // Prevent the app from crashing
          }
        });

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(popupWebView);
        dialog.show();

        android.view.Window window = dialog.getWindow();
        if (window != null) {
          android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
          int width = (int) (metrics.widthPixels - 30);
          int height = (int) (metrics.heightPixels - 30);
          window.setLayout(width, height);
        }

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

      // Also handle closing the popup window from JavaScript (e.g., window.close()),
      // might not be needed
      @Override
      public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
        Dialog dialog = popupDialogs.get(window);
        if (dialog != null && dialog.isShowing()) {
          dialog.dismiss();
        }
      }
    });

    Log.d(TAG, "onCreate() completed - native layer initialization finished");
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
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
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

      // Skip port forwarding check - Android doesn't support UPnP and the 60s scan wastes time
      String portPatch = "(function() {" +
          "  function patchNative(obj) {" +
          "    obj.checkIncomingConnections = function() { return Promise.resolve(false); };" +
          "  }" +
          "  if (window.native) { patchNative(window.native); }" +
          "  else {" +
          "    var desc = Object.getOwnPropertyDescriptor(window, 'native');" +
          "    Object.defineProperty(window, 'native', {" +
          "      configurable: true," +
          "      set: function(val) {" +
          "        patchNative(val);" +
          "        Object.defineProperty(window, 'native', desc || { value: val, configurable: true, writable: true });" +
          "      }" +
          "    });" +
          "  }" +
          "})();";
      webView.evaluateJavascript(portPatch, null);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Inject patches that must run BEFORE page scripts execute
  private void injectEarlyPatches(WebView webView) {
    // Intercept JASSUB worker creation to swap WebGPU-dependent 2.x worker with Canvas2D-based 1.8.8 worker
    // JASSUB creates its worker with: new Worker(url, { name: 'jassub-worker', type: 'module' })
    // We override the Worker constructor to redirect only JASSUB's worker to our interceptable marker URL
    String jassubPatch = "(function() {" +
        "  if (window.__hayaseWorkerPatched) return;" +
        "  window.__hayaseWorkerPatched = true;" +
        "  var OriginalWorker = window.Worker;" +
        "  window.Worker = function(url, options) {" +
        "    if (options && options.name === 'jassub-worker') {" +
        "      console.log('[HAYASE] Intercepting JASSUB worker creation, redirecting to local jassub-compat');" +
        "      url = 'https://hayase.app/__jassub_worker_intercept__.js';" +
        "    }" +
        "    return new OriginalWorker(url, options);" +
        "  };" +
        "  window.Worker.prototype = OriginalWorker.prototype;" +
        "  console.log('[HAYASE] Worker constructor patched for JASSUB interception');" +
        "})();";
    webView.evaluateJavascript(jassubPatch, null);
  }
}