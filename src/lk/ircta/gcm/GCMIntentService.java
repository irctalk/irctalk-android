package lk.ircta.gcm;

import java.io.IOException;
import java.util.Map;

import lk.ircta.R;
import lk.ircta.activity.ChatActivity;
import lk.ircta.activity.MainActivity;
import lk.ircta.application.Config;
import lk.ircta.local.Local;
import lk.ircta.model.Log;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

import org.apache.log4j.Logger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	private static final Logger logger = Logger.getLogger(GCMIntentService.class);
	
	private static final int NOTF_PUSH = 0;
	
	public GCMIntentService() {
		super(Config.GCM_SENDER_ID);
	}
	
	@Override
	protected void onError(Context context, String errorId) {
		logger.debug("onError - " + errorId);
	}

	@Override
	protected void onMessage(Context context, final Intent intent) {
		logger.debug("onMessage - " + intent);
		
		String logJson = intent.getStringExtra("log");
		
		Log log = null;
		String channelJson = null;
		try {
			log = JsonResponseHandler.mapper.readValue(logJson, Log.class);
			channelJson = JsonResponseHandler.mapper.writeValueAsString(log.getChannel());
		} catch (JsonGenerationException e) {
			logger.error(null, e);
		} catch (JsonMappingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}

		Intent resultIntent = new Intent(this, ChatActivity.class);
		resultIntent.putExtra(ChatActivity.EXTRA_SERVER_ID, log.getServerId());
		resultIntent.putExtra(ChatActivity.EXTRA_CHANNEL, channelJson);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
				.addParentStack(MainActivity.class)
				.addNextIntent(resultIntent);
		
		NotificationCompat.Builder notfBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat_push)
				.setContentTitle(log.getFrom())
				.setContentText(log.getMessage())
				.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTF_PUSH, notfBuilder.build());
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		logger.debug("onRegistered - " + regId);
		
		if (!Local.INSTANCE.isSignedIn()) // should not happen
			return;
		
		Map<String, Object> data = new MapBuilder<String, Object>(3)
				.put("push_type", "gcm")
				.put("push_token", regId)
				.put("alert", true)
				.build();
		IrcTalkService.sendRequest("setNotification", data, new JsonResponseHandler<Void>() {
			@Override
			public void onReceiveData(Void data) {
				Local.INSTANCE.saveGCMRegIdSent(true);
			}
		});
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		logger.debug("onUnregistered - " + regId);
		
		if (!Local.INSTANCE.isSignedIn()) // should not happen
			return;
		
		Map<String, Object> data = new MapBuilder<String, Object>(3)
				.put("push_type", "gcm")
				.put("push_token", regId)
				.put("alert", false)
				.build();
		IrcTalkService.sendRequest("setNotification", data, null);
	}
}
