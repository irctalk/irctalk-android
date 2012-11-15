package lk.ircta.network.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lk.ircta.application.GlobalApplication;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Model;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.service.IrcTalkService;

import org.apache.log4j.Logger;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class UpdateChannelHandler extends JsonResponseHandler<UpdateChannelHandler.UpdateChannelData> {
	private static final Logger logger = Logger.getLogger(UpdateChannelHandler.class);
	
	protected static class UpdateChannelData implements Model {
		public List<Channel> channels;
		
		protected UpdateChannelData() {};
	}
	
	private final IrcTalkService talkService;
	
	public UpdateChannelHandler(IrcTalkService talkService) {
		super(UpdateChannelHandler.UpdateChannelData.class);
		
		this.talkService = talkService;
	}
	
	@Override
	public void onReceiveData(UpdateChannelData data) {
		Set<String> updatedChannelKeys = new HashSet<String>(data.channels.size());
		
		for (Channel newChannel : data.channels) {
			Channel channel = talkService.getChannel(newChannel.getServerId(), newChannel.getChannel());
			if (channel == null) {
				talkService.putChannel(newChannel);
				channel = newChannel;
			} else 
				channel.mergeUpdate(newChannel);
			
			updatedChannelKeys.add(channel.getChannelKey());
		}
		
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(GlobalApplication.getInstance());
		Intent intent = new Intent(LocalBroadcast.UPDATE_CHANNELS);
		try {
			intent.putExtra(LocalBroadcast.EXTRA_CHANNEL_KEYS, mapper.writeValueAsString(updatedChannelKeys));
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
