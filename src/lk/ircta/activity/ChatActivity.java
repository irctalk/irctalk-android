package lk.ircta.activity;

import java.io.IOException;

import lk.ircta.R;
import lk.ircta.fragment.ChannelChatFragment;
import lk.ircta.fragment.WriteMessageFragment;
import lk.ircta.fragment.WriteMessageFragment.OnSendMessageListener;
import lk.ircta.model.Channel;
import lk.ircta.network.JsonResponseHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.MenuItem;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ChatActivity extends BaseActivity implements OnSendMessageListener {
	private static final Logger logger = Logger.getLogger(ChatActivity.class);
	
	public static final String EXTRA_SERVER = "server";
	public static final String EXTRA_CHANNEL = "channel";
	
	private ChannelChatFragment channelChatfragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.chat_activity);

		Intent intent = getIntent();
		channelChatfragment = ChannelChatFragment.newInstance(intent.getStringExtra(EXTRA_SERVER), intent.getStringExtra(EXTRA_CHANNEL));
		getSupportFragmentManager().beginTransaction().replace(R.id.content, channelChatfragment).commit();
		
		Channel channel = null;
		try {
			channel = JsonResponseHandler.mapper.readValue(intent.getStringExtra(EXTRA_CHANNEL), Channel.class);
		} catch (JsonParseException e) {
			logger.error(null, e);
		} catch (JsonMappingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}
		
		WriteMessageFragment writeMessageFragment = (WriteMessageFragment) getSupportFragmentManager().findFragmentById(R.id.write_message);
		writeMessageFragment.setOnSendMessageListener(this);
		writeMessageFragment.setAutoCompleteNicknames(channel.getMembers().toArray(ArrayUtils.EMPTY_STRING_ARRAY));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSendMessage(String msg) {
		channelChatfragment.sendMessage(msg);
	}
}
