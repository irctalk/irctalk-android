package lk.ircta.util.webview;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class CommonWebChromeClient extends WebChromeClient {
	public static final CommonWebChromeClient DEFAULT_CLIENT = new CommonWebChromeClient();
	
	@Override
	public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {
		new AlertDialog.Builder(view.getContext())
		.setMessage(message)
		.setCancelable(false)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				result.confirm();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				result.cancel();
			}
		}).show();

		return true;
	};

	@Override
	public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
		new AlertDialog.Builder(view.getContext())
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				result.confirm();
			}
		}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				result.cancel();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				result.cancel();
			}
		}).show();

		return true;
	}
}
