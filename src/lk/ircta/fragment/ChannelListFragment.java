package lk.ircta.fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.ircta.R;
import lk.ircta.activity.AddServerActivity;
import lk.ircta.activity.ChatActivity;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Log;
import lk.ircta.model.Server;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.GetInitLogsData;
import lk.ircta.network.datamodel.GetServersData;
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
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ChannelListFragment extends BaseFragment implements OnChildClickListener {
	private static final Logger logger = Logger.getLogger(ChannelListFragment.class);
	
	private static final int MENU_ADD_SERVER = 100;

	private static final int REQUEST_ADD_SERVER = 0;

	private ExpandableListView listView;
	private ChannelExpandableListAdapter adapter;
	private List<Server> servers;
	private Map<Server, List<Channel>> channels;

	private final BroadcastReceiver pushLogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				List<Log> logs = JsonResponseHandler.mapper.readValue(intent.getStringExtra(LocalBroadcast.EXTRA_LOGS), new TypeReference<List<Log>>(){});
				for (Log log : logs)
					adapter.setLastLogIfLast(log);
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

	private final BroadcastReceiver addChannelReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				Channel channel = JsonResponseHandler.mapper.readValue(intent.getStringExtra(LocalBroadcast.EXTRA_CHANNEL), Channel.class);
				for (Server server : servers) {
					if (server.getId() == channel.getServerId()) {
						channels.get(server).add(channel);
						break;
					}
				}
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

		private final OnClickListener groupItemClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Server server = (Server) v.getTag(R.id.server);

				switch (v.getId()) {
				case R.id.add_channel: {
					final EditText channelEditText = new EditText(getActivity());
					AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle("채널 추가").setView(channelEditText)
							.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Map<String, Object> data = new MapBuilder<String, Object>(2)
											.put("server_id", server.getId())
											.put("channel", channelEditText.getText().toString())
											.build();
									IrcTalkService.sendRequest("addChannel", data, new JsonResponseHandler<Void>() {
										@Override
										public void onReceiveData(Void data) {
											Toast.makeText(getActivity(), "response", Toast.LENGTH_SHORT).show();
											requestGetServers();
										}
									});
								}
							}).setNegativeButton(android.R.string.cancel, null);
					alert.show();

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

		public ChannelExpandableListAdapter(List<Server> servers) {
			this.servers = servers;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return channels.get(servers.get(groupPosition)).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return channels.get(servers.get(groupPosition)).size();
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
				lastMessageView.setText(lastLog.getFrom() != null ? lastLog.getFrom() + ": " + lastLog.getMessage() : lastLog.getMessage());
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
			serverView.setText(String.format("%s (%d)", server.getName(), channels.get(servers.get(groupPosition)).size()));

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

		public void setLastLogIfLast(Log log) {
			for (int i = 0; i < getGroupCount(); i++) {
				if (((Server) getGroup(i)).getId() == log.getServerId()) {
					Server server = ((Server) getGroup(i));
					for (Channel channel : channels.get(server)) {
						if (channel.getChannelKey().equals(log.getChannelKey())) {
							if (channel.getLastLog() == null || log.getLogId() > channel.getLastLog().getLogId())
								channel.setLastLog(log);
							break;
						}
					}
					break;
				}
			}
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
	public void onBindService(IrcTalkService talkService) {
		super.onBindService(talkService);

		requestGetServers();
	}

	private void requestGetServers() {
		IrcTalkService.sendRequest("getServers", Collections.EMPTY_MAP, new JsonResponseHandler<GetServersData>(GetServersData.class) {
			@Override
			public void onReceiveData(GetServersData data) {
				servers = data.servers;
				channels = new HashMap<Server, List<Channel>>();

				Map<Long, Server> serverIdMap = new HashMap<Long, Server>();
				for (Server server : servers) {
					serverIdMap.put(server.getId(), server);
					if (!channels.containsKey(server))
						channels.put(server, new ArrayList<Channel>());
				}

				for (Channel channel : data.channels) {
					Server server = serverIdMap.get(channel.getServerId());
					channels.get(server).add(channel);
				}
				
				logger.info(channels);
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						boolean isFirst = adapter == null;

						adapter = new ChannelExpandableListAdapter(servers);
						listView.setAdapter(adapter);

						// expand all groups
						for (int i = 0; i < servers.size(); i++)
							listView.expandGroup(i);

						if (isFirst) {
							localBroadcastManager.registerReceiver(pushLogReceiver, new IntentFilter(LocalBroadcast.PUSH_LOGS));
							localBroadcastManager.registerReceiver(addChannelReceiver, new IntentFilter(LocalBroadcast.ADD_CHANNEL));
						}
					}
				});
				
				Map<String, Object> reqData = new MapBuilder<String, Object>(2)
						.put("last_log_id", -1)
						.put("log_count", 30)
						.build();

				IrcTalkService.sendRequest("getInitLogs", reqData, new JsonResponseHandler<GetInitLogsData>(GetInitLogsData.class) {
					@Override
					public void onReceiveData(GetInitLogsData data) {
						IrcTalkService talkService = getIrcTalkService();
						if (talkService == null)
							return;
						talkService.pushLogs(data.logs);
					}
				});
			}
		});
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Intent intent = new Intent(getActivity(), ChatActivity.class);
		try {
			intent.putExtra(ChatActivity.EXTRA_SERVER, JsonResponseHandler.mapper.writeValueAsString(servers.get(groupPosition)));
			intent.putExtra(ChatActivity.EXTRA_CHANNEL,
					JsonResponseHandler.mapper.writeValueAsString(channels.get(servers.get(groupPosition)).get(childPosition)));
		} catch (JsonGenerationException e) {
			logger.error(null, e);
		} catch (JsonMappingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}
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
			requestGetServers();
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		localBroadcastManager.unregisterReceiver(pushLogReceiver);
		localBroadcastManager.unregisterReceiver(addChannelReceiver);
	}
}
