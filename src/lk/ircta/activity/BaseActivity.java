package lk.ircta.activity;

import java.util.HashSet;
import java.util.Set;

import lk.ircta.R;
import lk.ircta.local.Local;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.service.IrcTalkService;
import lk.ircta.service.IrcTalkService.IrcTalkServiceBinder;
import lk.ircta.service.OnBindServiceListener;

import org.apache.log4j.Logger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class BaseActivity extends SherlockFragmentActivity {
	private static final Logger logger = Logger.getLogger(BaseActivity.class);
	
	protected static final int REQUEST_SIGNIN = 10000;
	
	protected LocalBroadcastManager localBroadcastManager;
	
	protected IrcTalkService ircTalkService;
	private int bindDispatchLatch = 2;
	private final ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ircTalkService = ((IrcTalkServiceBinder) service).getService();
			if (ircTalkService.isConnectionInitialized())
				connInitReceiver.onReceive(BaseActivity.this, null);

			decrementBindDispatchLatchAndTryDispatch();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			ircTalkService = null;
			
			if (getSupportActionBar() != null)
				getSupportActionBar().setIcon(R.drawable.ic_launcher_gray);
			
			incrementBindDispatchLatch();
		}
	};

	private Set<OnBindServiceListener> onBindServiceListeners = new HashSet<OnBindServiceListener>();

	private final BroadcastReceiver connInitReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (getSupportActionBar() != null)
				getSupportActionBar().setIcon(R.drawable.ic_launcher);
			logger.debug("login received");
			
			decrementBindDispatchLatchAndTryDispatch();
		}
	};
	
	private final BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (getSupportActionBar() != null)
				getSupportActionBar().setIcon(R.drawable.ic_launcher_gray);
			logger.debug("disconnect received");

			incrementBindDispatchLatch();
		}
	};
	
	private void incrementBindDispatchLatch() {
		bindDispatchLatch++;
		if (bindDispatchLatch > 2)
			bindDispatchLatch = 2;
		logger.info("bindDispatchLatch : " + bindDispatchLatch);
	}
	
	private void decrementBindDispatchLatchAndTryDispatch() {
		bindDispatchLatch--;
		if (bindDispatchLatch == 0) {
			for (OnBindServiceListener listener : onBindServiceListeners)
				listener.onBindService(ircTalkService);
			onBindService(ircTalkService);
		}
		
		logger.info("bindDispatchLatch : " + bindDispatchLatch);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);

		if (!(this instanceof AuthActivity) && !Local.INSTANCE.isSignedIn()) {
			Intent signInIntent =new Intent(this, SignInActivity.class);
			signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivityForResult(signInIntent, REQUEST_SIGNIN);
		}

		if (getSupportActionBar() != null) {
			getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setIcon(R.drawable.ic_launcher_gray);
		}

		bindDispatchLatch = 2;
		
		bindService(new Intent(this, IrcTalkService.class), conn, BIND_AUTO_CREATE);

		localBroadcastManager.registerReceiver(connInitReceiver, new IntentFilter(LocalBroadcast.CONNECTION_INITIALIZED));
		localBroadcastManager.registerReceiver(disconnectReceiver, new IntentFilter(LocalBroadcast.DISCONNECT));
	}

	/**
	 * called when {@link IrcTalkService} is bound
	 * @param talkService 
	 */
	protected void onBindService(IrcTalkService talkService) {
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		localBroadcastManager.unregisterReceiver(connInitReceiver);
		localBroadcastManager.unregisterReceiver(disconnectReceiver);

		unbindService(conn);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_SIGNIN:
			if (resultCode != RESULT_OK)
				finish();
			break;
		}
	}

	/**
	 * 
	 * @return {@link IrcTalkService} instance if bound. <b>nullable</b>
	 */
	public IrcTalkService getIrcTalkService() {
		if (ircTalkService != null && ircTalkService.isConnectionInitialized())
			return ircTalkService;
		return null;
	}

	public void addOnBindServiceListener(OnBindServiceListener listener) {
		onBindServiceListeners.add(listener);
	}

	public void removeOnBindServiceListener(OnBindServiceListener listener) {
		onBindServiceListeners.remove(listener);
	}
}
