package lk.ircta.fragment;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lk.ircta.R;
import lk.ircta.activity.AddServerActivity;
import lk.ircta.activity.ChatActivity;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Log;
import lk.ircta.model.Server;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.AddChannelData;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ChannelListFragment extends BaseFragment implements OnChildClickListener {
	private static final Logger logger = Logger.getLogger(ChannelListFragment.class);
	
	private static final int MENU_ADD_SERVER = 100;

	private static final int REQUEST_ADD_SERVER = 0;

	private ExpandableListView listView;
	private ChannelExpandableListAdapter adapter;

	private final BroadcastReceiver updateChannelsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				IrcTalkService talkService = getIrcTalkService();
				if (talkService == null)
					return;
				
				Set<String> channelKeys = JsonResponseHandler.mapper.readValue(intent.getStringExtra(LocalBroadcast.EXTRA_CHANNEL_KEYS), new TypeReference<Set<String>>() {});
				
				Set<Long> serverIds = new HashSet<Long>();
				for (String channelKey : channelKeys) 
					serverIds.add(talkService.getChannel(channelKey).getServerId());
				
				for (Long serverId : serverIds)
					adapter.channels.put(serverId, talkService.getServerChannels(serverId));
			} catch (JsonParseException e) {
				logger.error(null, e);
			} catch (JsonMappingException e) {
				logger.error(null, e);
			} catch (IOException e) {
				logger.error(null, e);
			}
			adapter.notifyDataSetChanged();
		}
	};

	private class ChannelExpandableListAdapter extends BaseExpandableListAdapter {
		private List<Server> servers;
		private Map<Long, List<Channel>> channels;

		private final OnClickListener groupItemClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Server server = (Server) v.getTag(R.id.server);

				switch (v.getId()) {
				case R.id.add_channel: {
					final EditText channelEditText = new EditText(getActivity());
					final AlertDialog alert = new AlertDialog.Builder(getActivity())
							.setTitle("채널 추가")
							.setView(channelEditText)
							.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Map<String, Object> data = new MapBuilder<String, Object>(2)
											.put("server_id", server.getId())
											.put("channel", channelEditText.getText().toString())
											.build();
									IrcTalkService.sendRequest("addChannel", data, new JsonResponseHandler<AddChannelData>(AddChannelData.class) {
										@Override
										public void onReceiveData(final AddChannelData data) {
											runOnUiThread(new Runnable() {
												@Override
												public void run() {
													if (getIrcTalkService() != null)
														getIrcTalkService().addOrUpdateChannel(data.channel);
												}
											});
										}
									});
								}
							}).setNegativeButton(android.R.string.cancel, null)
							.show();
					alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					channelEditText.addTextChangedListener(new TextWatcher() {
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
							if (s.length() == 0 || s.charAt(0) != '#')
								alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
							else
								alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
						}
						
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
						
						@Override
						public void afterTextChanged(Editable s) {}
					});
					
					break;
				}
				default: {
					int groupIdx = servers.indexOf(server);
					if (!listView.expandGroup(groupIdx))
						listView.collapseGroup(groupIdx);
				}
				}
			}
		};

		public ChannelExpandableListAdapter(List<Server> servers, Map<Long, List<Channel>> channels) {
			this.servers = servers;
			this.channels = channels;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return channels.get(servers.get(groupPosition).getId()).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return channels.get(servers.get(groupPosition).getId()).size();
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			Channel channel = (Channel) getChild(groupPosition, childPosition);

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.channel_list_item_child, null);
			}

			TextView channelView = (TextView) convertView.findViewById(R.id.channel);
			channelView.setText(channel.getChannel());

			TextView userCountView = (TextView) convertView.findViewById(R.id.user_count);
			userCountView.setText(String.valueOf(channel.getMembers().size()));

			TextView lastMessageView = (TextView) convertView.findViewById(R.id.last_message);
			Log lastLog = channel.getLastLog();
			if (lastLog != null)
				lastMessageView.setText(lastLog.getFromMessage());
			else
				lastMessageView.setText("");

			return convertView;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return servers.get(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public int getGroupCount() {
			return servers.size();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			final Server server = (Server) getGroup(groupPosition);

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.channel_list_item_group, null);
				convertView.setTag(R.id.server, server);
			}

			convertView.setOnClickListener(groupItemClickListener);

			TextView serverView = (TextView) convertView.findViewById(R.id.server);
			serverView.setText(String.format("%s (%d)", server.getName(), channels.get(servers.get(groupPosition).getId()).size()));

			ImageButton addChannelBtn = (ImageButton) convertView.findViewById(R.id.add_channel);
			addChannelBtn.setOnClickListener(groupItemClickListener);
			addChannelBtn.setTag(R.id.server, server);

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.channel_list_fragment, container, false);

		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		listView.setItemsCanFocus(true);

		return view;
	}

	@Override
	public void onBindService(final IrcTalkService talkService) {
		super.onBindService(talkService);

		adapter = new ChannelExpandableListAdapter(talkService.getServers(), talkService.getChannels());
		listView.setAdapter(adapter);

		// expand all groups
		for (int i = 0; i < adapter.servers.size(); i++)
			listView.expandGroup(i);
		
		localBroadcastManager.registerReceiver(updateChannelsReceiver, new IntentFilter(LocalBroadcast.UPDATE_CHANNELS));
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Intent intent = new Intent(getActivity(), ChatActivity.class);
		intent.putExtra(ChatActivity.EXTRA_SERVER_ID, adapter.servers.get(groupPosition).getId());
		intent.putExtra(ChatActivity.EXTRA_CHANNEL, adapter.channels.get(adapter.servers.get(groupPosition).getId()).get(childPosition).getChannel());
		startActivity(intent);
		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.add(Menu.NONE, MENU_ADD_SERVER, Menu.NONE, "서버 추가").setIcon(R.drawable.ic_ab_new).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD_SERVER:
			Intent intent = new Intent(getActivity(), AddServerActivity.class);
			startActivityForResult(intent, REQUEST_ADD_SERVER);
			return true;
		}

		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ADD_SERVER:
			
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		localBroadcastManager.unregisterReceiver(updateChannelsReceiver);
	}
}
