package lk.ircta.activity;

import lk.ircta.R;
import lk.ircta.application.Config;
import lk.ircta.local.Local;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gcm.GCMRegistrar;

public class MainActivity extends BaseActivity {
	private static final Logger logger = Logger.getLogger(MainActivity.class);
	
	private static final int MENU_SETTING = 1090;
	private static final int MENU_SIGN_OUT = 1100;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		getSupportActionBar().setHomeButtonEnabled(false);
		
		try {
			GCMRegistrar.checkDevice(this);
			GCMRegistrar.checkManifest(this);
			final String regId = GCMRegistrar.getRegistrationId(this);
			if (StringUtils.isEmpty(regId)) 
			  GCMRegistrar.register(this, Config.GCM_SENDER_ID);
			else 
				logger.debug("GCM already registered");
		} catch (UnsupportedOperationException e) {
			// ignore
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SETTING, Menu.NONE, "설정").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, MENU_SIGN_OUT, Menu.NONE, "로그아웃").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTING:
			return true;
		case MENU_SIGN_OUT:
			Local.INSTANCE.signOut();
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
