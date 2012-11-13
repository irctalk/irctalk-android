package lk.ircta.network.handler;

import java.io.IOException;

import lk.ircta.application.GlobalApplication;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Model;
import lk.ircta.network.JsonResponseHandler;

import org.apache.log4j.Logger;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class AddChannelHandler extends JsonResponseHandler<AddChannelHandler.AddChannelData> {
	private static final Logger logger = Logger.getLogger(AddChannelHandler.class);
	
	protected static class AddChannelData implements Model {
		public Channel channel;
		
		protected AddChannelData() {};
	}
	
	public AddChannelHandler() {
		super(AddChannelHandler.AddChannelData.class);
	}
	
	@Override
	public void onReceiveData(AddChannelData data) {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(GlobalApplication.getInstance());
		Intent intent = new Intent(LocalBroadcast.ADD_CHANNEL);
		try {
			intent.putExtra(LocalBroadcast.EXTRA_CHANNEL, mapper.writeValueAsString(data.channel));
		} catch (JsonGenerationException e) {
			logger.error(null, e);
		} catch (JsonMappingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}
		lbm.sendBroadcast(intent);
	}
}
