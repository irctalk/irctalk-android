package lk.ircta.activity;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import lk.ircta.R;
import lk.ircta.local.Local;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.util.MapBuilder;

public class SignInActivity extends BaseActivity implements AuthActivity, OnClickListener {
	private static final Logger logger = Logger.getLogger(SignInActivity.class);
	
	private static final int REQUEST_AUTH_WEB = 100;
	
	private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
	private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

	private ProgressDialog progressDialog;
	private Button signInButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signin_activity);

		signInButton = (Button) findViewById(R.id.btn_signin);
		signInButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_signin:
			if (!ArrayUtils.isEmpty(AccountManager.get(this).getAccountsByType(ACCOUNT_TYPE_GOOGLE)))
				AccountManager.get(this).getAuthTokenByFeatures(ACCOUNT_TYPE_GOOGLE, SCOPE, null, this, null, null,
						new AccountManagerCallback<Bundle>() {
							@Override
							public void run(AccountManagerFuture<Bundle> future) {
								Bundle bundle;
								try {
									bundle = future.getResult();
//									String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
									String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
									
									progressDialog = ProgressDialog.show(SignInActivity.this, null, "잠시만 기다려주세요..", true);
									
									proceedRegister(authToken);
								} catch (OperationCanceledException e) {
									logger.warn("user canceled", e);
								} catch (AuthenticatorException e) {
									logger.error(null, e);
								} catch (IOException e) {
									logger.error(null, e);
								}
							}
						}, null);
			else
				startActivityForResult(new Intent(this, SignInAuthWebActivity.class), REQUEST_AUTH_WEB);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_AUTH_WEB:
			if (resultCode == RESULT_OK) {
				String accessToken = data.getStringExtra(SignInAuthWebActivity.EXTRA_ACCESS_TOKEN);
				
				proceedRegister(accessToken);
			}
			break;
		}
	}
	
	private void proceedRegister(String accessToken) {
		Map<String, String> dataMap = new MapBuilder<String, String>(1)
				.put("access_token", accessToken)
				.build();
		
		ircTalkService.sendRequestSync("register", dataMap, new JsonResponseHandler<Map<String, Object>>() {
			@Override
			public void onReceiveData(final Map<String, Object> data) {
				Local.INSTANCE.saveAuthKey((String) data.get("auth_key"));
				
				ircTalkService.sendRequestSync("login", data, new JsonResponseHandler<Void>() {
					@Override
					public void onReceiveData(Void data) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
								
								setResult(RESULT_OK);
								finish();
							}
						});
					}
				});
			}
			
			@Override
			public void onReceiveError(int status, String msg) {
				super.onReceiveError(status, msg);
				
				progressDialog.dismiss();
				AlertDialog.Builder alert = new AlertDialog.Builder(SignInActivity.this)
				.setMessage("로그인에 실패하였습니다.");
				alert.show();
			}
			
			@Override
			public void onThrowable(Throwable t) {
				super.onThrowable(t);
				
				progressDialog.dismiss();
				AlertDialog.Builder alert = new AlertDialog.Builder(SignInActivity.this)
				.setMessage("로그인에 실패하였습니다.");
				alert.show();
			}
		});
	}
}
