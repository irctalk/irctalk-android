package lk.ircta.fragment;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lk.ircta.R;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Log;
import lk.ircta.model.Server;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.GetPastLogsData;
import lk.ircta.network.datamodel.SendLogData;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;
import lk.ircta.util.SortedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ChannelChatFragment extends BaseFragment implements OnItemClickListener {
	private static final Logger logger = Logger.getLogger(ChannelChatFragment.class);
	
	private static final String ARG_SERVER_ID = "server_id";
	private static final String ARG_CHANNEL = "channel";
	
	private static final int CONTEXT_MENU_COPY = 100;
	private static final int CONTEXT_MENU_SHARE = 101;
	
	private final BroadcastReceiver pushLogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				List<Log> logs = JsonResponseHandler.mapper.readValue(intent.getStringExtra(LocalBroadcast.EXTRA_LOGS), new TypeReference<List<Log>>(){});
				for (Log log : logs) 
					if (log.getChannelKey().equals(channel.getChannelKey()))
						chatListAdapter.add(log);
			} catch (JsonParseException e) {
				logger.error(null, e);
			} catch (JsonMappingException e) {
				logger.error(null, e);
			} catch (IOException e) {
				logger.error(null, e);
			}
			chatListAdapter.notifyDataSetChanged();
		}
	};
	
	private Server server;
	private Channel channel;
	
	private ListView chatListView;
	private ViewGroup chatListHeaderView;
	private ArrayAdapter<Log> chatListAdapter;
	
	private AtomicBoolean gettingPastLog;
	private long rootLogId;
	
	public static ChannelChatFragment newInstance(long serverId, String channel) {
		ChannelChatFragment fragment = new ChannelChatFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_SERVER_ID, serverId);
		args.putString(ARG_CHANNEL, channel);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static ChannelChatFragment newInstance(Server server, Channel channel) {
		ChannelChatFragment fragment = new ChannelChatFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_SERVER_ID, server.getId());
		args.putString(ARG_CHANNEL, channel.getChannel());
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		gettingPastLog = new AtomicBoolean();
		rootLogId = -1;
		
		chatListAdapter = new LogAdapter(getActivity(), R.id.message);
		localBroadcastManager.registerReceiver(pushLogReceiver, new IntentFilter(LocalBroadcast.PUSH_LOGS));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.channel_chat_fragment, container, false);
		
		chatListView = (ListView) view.findViewById(android.R.id.list);
		
		chatListHeaderView = (ViewGroup) ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_list_header, null);
		chatListHeaderView.getChildAt(0).setVisibility(View.GONE);
		
		chatListView.addHeaderView(chatListHeaderView);
		chatListView.setHeaderDividersEnabled(false);
		registerForContextMenu(chatListView);
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		chatListView.setAdapter(chatListAdapter);
	}

	@Override
	public void onBindService(IrcTalkService talkService) {
		super.onBindService(talkService);
		
		Bundle args = getArguments();
		if (args == null)
			return;
		
		server = talkService.getServer(args.getLong(ARG_SERVER_ID));
		channel = talkService.getChannel(server.getId(), args.getString(ARG_CHANNEL));
		
		getSherlockActivity().getSupportActionBar().setTitle(channel.getChannel());
		if (!StringUtils.isEmpty(channel.getTopic()))
			getSherlockActivity().getSupportActionBar().setSubtitle(channel.getTopic());
		
		for (Log log : talkService.getChannelLogs(channel))
			chatListAdapter.add(log);
		
		chatListView.setSelection(chatListView.getCount() > 0 ? chatListView.getCount() - 1 : 0);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(ContextMenu.NONE, CONTEXT_MENU_COPY, ContextMenu.NONE, android.R.string.copy);
		menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE, ContextMenu.NONE, "Share");
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
		case CONTEXT_MENU_COPY:
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(chatListAdapter.getItem((int) info.id).getMessage());
			return true;
		case CONTEXT_MENU_SHARE:
			Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, chatListAdapter.getItem((int) info.id).getMessage());
			startActivity(Intent.createChooser(sharingIntent, "Share"));
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void requestGetPastLog() {
		if (gettingPastLog.getAndSet(true) || rootLogId != -1) 
			return; // request already sent
		
		chatListHeaderView.getChildAt(0).setVisibility(View.VISIBLE);
		
		Map<String, Object> data = new MapBuilder<String, Object>(4)
				.put("server_id", server.getId())
				.put("channel", channel.getChannel())
				.put("last_log_id", chatListAdapter.isEmpty() ? -1 : chatListAdapter.getItem(0).getLogId())
				.put("log_count", 30)
				.build();
		
		IrcTalkService.sendRequest("getPastLogs", data, new JsonResponseHandler<GetPastLogsData>(GetPastLogsData.class) {
			@Override
			public void onReceiveData(final GetPastLogsData data) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int position = chatListView.getFirstVisiblePosition();
						
						getIrcTalkService().pushLogsSync(data.logs);
						
						position += data.logs.size();
						chatListView.setSelection(position);
						
						if (data.logs.isEmpty() && !chatListAdapter.isEmpty())
							rootLogId = chatListAdapter.getItem(0).getLogId();
						
						chatListHeaderView.getChildAt(0).setVisibility(View.GONE);
						gettingPastLog.set(false);
					}
				});
			}
		});
	}
	
	public void sendMessage(String message) {
		Map<String, Object> data = new MapBuilder<String, Object>()
				.put("server_id", server.getId())
				.put("channel", channel.getChannel())
				.put("message", message)
				.build();
		IrcTalkService.sendRequest("sendLog", data, new JsonResponseHandler<SendLogData>(SendLogData.class) {
			@Override
			public void onReceiveData(SendLogData data) {
				getIrcTalkService().pushLog(data.log);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						chatListView.setSelection(chatListView.getCount() - 1);
					}
				});
			}
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		localBroadcastManager.unregisterReceiver(pushLogReceiver);
	}
	
	private class LogAdapter extends ArrayAdapter<Log> {
		private class ViewHolder {
			private TextView nameView;
			private TextView dateView;
			private TextView messageView;
		}
		
		private static final int GET_PAST_LOG_AREA = 3;
		private final LayoutInflater inflater = getActivity().getLayoutInflater();
//		private final DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
		private final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
		
		public LogAdapter(Context context, int textViewResourceId, List<Log> logs) {
			super(context, textViewResourceId, new SortedList<Log>(logs, Log.LOG_COMPARATOR, true));
		}
		
		public LogAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId, new SortedList<Log>(Log.LOG_COMPARATOR, true));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.chat_list_log, null);
				holder = new ViewHolder();
				holder.nameView = (TextView) convertView.findViewById(R.id.name);
				holder.dateView = (TextView) convertView.findViewById(R.id.date);
				holder.messageView = (TextView) convertView.findViewById(R.id.message);
				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();
			
			if (position < GET_PAST_LOG_AREA)
				requestGetPastLog();
			
			Log log = getItem(position);
			
			if (log.getFrom() != null) {
				holder.nameView.setText(log.getFrom());
				holder.nameView.setVisibility(View.VISIBLE);
			} else 
				holder.nameView.setVisibility(View.GONE);
			
			Date date = new Date(log.getTimestamp());
			holder.dateView.setText(timeFormat.format(date));
			
			holder.messageView.setText(log.getMessage());
			((RelativeLayout.LayoutParams) holder.messageView.getLayoutParams()).addRule(RelativeLayout.LEFT_OF, log.getFrom() == null ? R.id.date : 0);
			
			return convertView;
		}
	}
}
