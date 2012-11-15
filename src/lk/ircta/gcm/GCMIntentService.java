package lk.ircta.gcm;

import java.io.IOException;
import java.util.Map;

import lk.ircta.R;
import lk.ircta.activity.ChatActivity;
import lk.ircta.application.Config;
import lk.ircta.local.Local;
import lk.ircta.model.Log;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.PushLogData;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

import org.apache.log4j.Logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.fasterxml.jackson.core.JsonParseException;
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
		
		String type = intent.getStringExtra("type");
		
		if ("pushLog".equals(type)) {
			try {
				PushLogData data = JsonResponseHandler.mapper.readValue(intent.getStringExtra("data"), PushLogData.class);
				showPushLogNotification(context, data.log);
			} catch (JsonParseException e) {
				logger.error(null, e);
			} catch (JsonMappingException e) {
				logger.error(null, e);
			} catch (IOException e) {
				logger.error(null, e);
			}
		}
	}
	
	public static void showPushLogNotification(Context context, Log log) {
		Intent resultIntent = new Intent(context, ChatActivity.class);
		resultIntent.putExtra(ChatActivity.EXTRA_SERVER_ID, log.getServerId());
		resultIntent.putExtra(ChatActivity.EXTRA_CHANNEL, log.getChannel());
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
				.addParentStack(ChatActivity.class)
				.addNextIntent(resultIntent);
		
		NotificationCompat.Builder notfBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_stat_push)
				.setContentTitle(log.getChannel())
				.setContentText(log.getFromMessage())
				.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
				.setTicker(log.getFromMessage())
				.setDefaults(Notification.DEFAULT_ALL)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setAutoCancel(true);
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(log.getChannelKey(), NOTF_PUSH, notfBuilder.build());
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
