package lk.ircta.activity;

import lk.ircta.R;
import lk.ircta.fragment.ChannelChatFragment;
import lk.ircta.fragment.WriteMessageFragment;
import lk.ircta.fragment.WriteMessageFragment.OnSendMessageListener;
import lk.ircta.model.Channel;
import lk.ircta.service.IrcTalkService;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.MenuItem;

public class ChatActivity extends BaseActivity implements OnSendMessageListener {
	private static final Logger logger = Logger.getLogger(ChatActivity.class);
	
	public static final String EXTRA_SERVER_ID = "server_id";
	public static final String EXTRA_CHANNEL = "channel";
	
	private ChannelChatFragment channelChatfragment;
	private WriteMessageFragment writeMessageFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.chat_activity);

		Intent intent = getIntent();
		channelChatfragment = ChannelChatFragment.newInstance(intent.getLongExtra(EXTRA_SERVER_ID, 0), intent.getStringExtra(EXTRA_CHANNEL));
		getSupportFragmentManager().beginTransaction().replace(R.id.content, channelChatfragment).commit();
		
		writeMessageFragment = (WriteMessageFragment) getSupportFragmentManager().findFragmentById(R.id.write_message);
		writeMessageFragment.setOnSendMessageListener(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onBindService(IrcTalkService talkService) {
		super.onBindService(talkService);
		
		Intent intent = getIntent();
		
		Channel channel = talkService.getChannel(intent.getLongExtra(EXTRA_SERVER_ID, 0), intent.getStringExtra(EXTRA_CHANNEL));
		writeMessageFragment.setAutoCompleteNicknames(channel.getMembers().toArray(ArrayUtils.EMPTY_STRING_ARRAY));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSendMessage(String msg) {
		channelChatfragment.sendMessage(msg);
	}
}
