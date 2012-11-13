package lk.ircta.local;

import lk.ircta.application.GlobalApplication;

import android.content.Context;
import android.content.SharedPreferences;

public enum Local {
	INSTANCE;

	private final SharedPreferences preferences = 
			GlobalApplication.getInstance().getSharedPreferences(Local.class.getName(), Context.MODE_PRIVATE);

	private String authKey;
	private String email;
	
	public boolean isSignedIn() {
		return getAuthKey() != null;
	}
	
	public void signOut() {
		saveAuthKey(null);
		saveEmail(null);
	}

	public void saveAuthKey(String authKey) {
		this.authKey = authKey;
		preferences.edit().putString("auth_key", authKey).commit();
	}
	
	public String getAuthKey() {
		if (authKey == null)
			authKey = preferences.getString("auth_key", null);
		return authKey;
	}
	
	public void saveEmail(String email) {
		this.email = email;
		preferences.edit().putString("email", email).commit();
	}
	
	public String getEmail() {
		if (email == null)
			email = preferences.getString("email", null);
		return email;
	}
}
