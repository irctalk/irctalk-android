package lk.ircta.local;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.android.gcm.GCMRegistrar;

import lk.ircta.application.Config;
import lk.ircta.application.GlobalApplication;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public enum Local {
	INSTANCE;
	
	private static final Logger logger = Logger.getLogger(Local.class);

	private static abstract class PreferenceEditorHelper {
		protected abstract void apply(SharedPreferences.Editor editor);
	}

	@TargetApi(9)
	private static final class PreferenceEditorHelperGingerBread extends PreferenceEditorHelper {
		@Override
		protected void apply(SharedPreferences.Editor editor) {
			editor.apply();
		}
	}

	private static final class PreferenceEditorHelperCompat extends PreferenceEditorHelper {
		@Override
		protected void apply(SharedPreferences.Editor editor) {
			editor.commit();
		}
	}

	private final PreferenceEditorHelper preferenceEditorHelper = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ? 
			new PreferenceEditorHelperGingerBread() : new PreferenceEditorHelperCompat();

	private final SharedPreferences preferences = GlobalApplication.getInstance().getSharedPreferences(Local.class.getName(), Context.MODE_PRIVATE);

	private String authKey;
	private String email;
	
	private String gcmRegId;

	public boolean isSignedIn() {
		return getAuthKey() != null;
	}
	
	public String getOrInitGCMRegistrationId() {
		if (this.gcmRegId != null)
			return StringUtils.isEmpty(this.gcmRegId) ? null : this.gcmRegId;
		
		Context ctx = GlobalApplication.getInstance();
		String regId = StringUtils.EMPTY;
		
		try {
			GCMRegistrar.checkDevice(ctx);
			GCMRegistrar.checkManifest(ctx);
			regId = GCMRegistrar.getRegistrationId(ctx);
			if (StringUtils.isEmpty(regId)) 
				GCMRegistrar.register(ctx, Config.GCM_SENDER_ID);
			else 
				logger.debug("GCM already registered");
		} catch (UnsupportedOperationException e) {
			// ignore
		}
		
		this.gcmRegId = regId;
		
		return StringUtils.isEmpty(regId) ? null : regId;
	}

	public void signOut() {
		saveAuthKey(null);
		saveEmail(null);
		
		String gcmId = getOrInitGCMRegistrationId();
		if (gcmId != null) {
			Map<String, Object> data = new MapBuilder<String, Object>(3)
					.put("push_type", "gcm")
					.put("push_token", gcmId)
					.put("alert", false)
					.build();
			IrcTalkService.sendRequest("setNotification", data, null);
		}
	}

	public void saveAuthKey(String authKey) {
		this.authKey = authKey;
		putString("auth_key", authKey);
	}

	public String getAuthKey() {
		if (authKey == null)
			authKey = preferences.getString("auth_key", null);
		return authKey;
	}

	public void saveEmail(String email) {
		this.email = email;
		putString("email", email);
	}

	public String getEmail() {
		if (email == null)
			email = preferences.getString("email", null);
		return email;
	}
	
	public void saveGCMRegIdSent(boolean gcmIdSent) {
		putBoolean("gcm_reg_id_sent", gcmIdSent);
	}
	
	public boolean isGCMRegIdSent() {
		return preferences.getBoolean("gcm_reg_id_sent", false);
	}
	
	private void putBoolean(String key, boolean value) {
		preferenceEditorHelper.apply(preferences.edit().putBoolean(key, value));	
	}

	private void putString(String key, String value) {
		if (value == null)
			preferenceEditorHelper.apply(preferences.edit().remove(key));
		preferenceEditorHelper.apply(preferences.edit().putString(key, value));
	}
}
