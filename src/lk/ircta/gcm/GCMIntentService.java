package lk.ircta.gcm;

import lk.ircta.application.Config;

import org.apache.log4j.Logger;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	private static final Logger logger = Logger.getLogger(GCMIntentService.class);
	
	public GCMIntentService() {
		super(Config.GCM_SENDER_ID);
	}
	
	@Override
	protected void onError(Context context, String errorId) {
		logger.debug("onError - " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		logger.debug("onMessage - " + intent);
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		logger.debug("onRegistered - " + regId);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		logger.debug("onUnregistered - " + regId);
	}
}
