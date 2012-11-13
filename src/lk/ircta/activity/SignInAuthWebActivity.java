package lk.ircta.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import lk.ircta.application.Config;
import lk.ircta.util.webview.CommonWebChromeClient;

public class SignInAuthWebActivity extends BaseActivity implements AuthActivity {
	public static final String EXTRA_ACCESS_TOKEN = "access_token";
	
	private static final String AUTH_URL = String.format("https://accounts.google.com/o/oauth2/auth?scope=%s&redirect_uri=%s&client_id=%s&response_type=%s", 
			Config.GOOGLE_AUTH_SCOPE, 
			Config.GOOGLE_AUTH_REDIRECT_URI, 
			Config.GOOGLE_AUTH_CLIENT_ID, 
			Config.GOOGLE_AUTH_RESPONSE_TYPE);
	
	private WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		webView = new WebView(this);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.startsWith(Config.GOOGLE_AUTH_REDIRECT_URI)) {
					Uri uri = Uri.parse(url);
					String accessToken = uri.getQueryParameter("access_token");
					
					Intent data = new Intent();
					data.putExtra(EXTRA_ACCESS_TOKEN, accessToken);
					setResult(RESULT_OK, data);
					
					finish();
					return true;
				}
				return false;
			}
		});
		webView.setWebChromeClient(CommonWebChromeClient.DEFAULT_CLIENT);
		
		setContentView(webView);
		
		webView.loadUrl(AUTH_URL);
		
		getSupportActionBar().setHomeButtonEnabled(false);
	}
	
	@Override
	public void onBackPressed() {
		if (webView.canGoBack())
			webView.goBack();
		else 
			super.onBackPressed();
	}
}
