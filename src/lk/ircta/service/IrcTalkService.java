package lk.ircta.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import lk.ircta.application.Config;
import lk.ircta.local.Local;
import lk.ircta.local.LocalBroadcast;
import lk.ircta.model.Channel;
import lk.ircta.model.Log;
import lk.ircta.model.Server;
import lk.ircta.network.ClientHandler;
import lk.ircta.network.JsonRequestModel;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.ResponseHandler;
import lk.ircta.network.datamodel.GetInitLogsData;
import lk.ircta.network.datamodel.GetServersData;
import lk.ircta.network.datamodel.LoginData;
import lk.ircta.network.handler.PushLogHandler;
import lk.ircta.network.handler.UpdateChannelHandler;
import lk.ircta.util.MapBuilder;
import lk.ircta.util.SortedList;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.AbstractChannel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

public class IrcTalkService extends Service {
	private static final Logger logger = Logger.getLogger(IrcTalkService.class);
	
	public class IrcTalkServiceBinder extends Binder {
		public IrcTalkService getService() {
			return IrcTalkService.this;
		}
	}
	
	private final IBinder binder = new IrcTalkServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.debug("Received Start Command");
		return START_STICKY;
	}
	
	private Thread ioWorkerThread;
	
	private static final BlockingQueue<WebSocketFrame> requestQueue = new LinkedBlockingQueue<WebSocketFrame>();
	private volatile AbstractChannel channel;
	
	private ClientHandler clientHandler;
	private CountDownLatch registerLatch, loginLatch;
	private volatile boolean isLoggedIn;
	private volatile boolean isConnectionInitialized;
	private volatile boolean shouldClose;
	private final MutableBoolean isConnectionActive = new MutableBoolean();
	
	private volatile List<Server> servers;
	private volatile Map<Long, List<Channel>> channels;
	private volatile Map<String, Channel> channelKeyMap;
	private volatile ConcurrentHashMap<String, TreeMap<Long, Log>> logs;

	@Override
	public void onCreate() {
		shouldClose = false;
		
		logs = new ConcurrentHashMap<String, TreeMap<Long, Log>>();
		
		logger.debug("Service Created");
		
		ioWorkerThread = new Thread() {
			private static final int RECONNECT_SLEEP_INTERVAL_INITIAL = 5000;
			private static final int RECONNECT_SLEEP_INTERVAL_MAX = RECONNECT_SLEEP_INTERVAL_INITIAL << 5;
			
			private int reconnectSleepInterval = RECONNECT_SLEEP_INTERVAL_INITIAL;
			
			public void run() {
				while (!shouldClose) {
					logger.info(shouldClose);
					registerLatch = new CountDownLatch(1);
					loginLatch = new CountDownLatch(1);
					isLoggedIn = false;
					isConnectionInitialized = false;
					isConnectionActive.setValue(false);
					
					ClientBootstrap bootstrap = new ClientBootstrap(new OioClientSocketChannelFactory(Executors.newCachedThreadPool()));
					
					URI uri = null;
					try {
						uri = new URI(Config.IRCTALK_SERVICE_URL);
					} catch (URISyntaxException e) {
						logger.error(null, e);
					}
					
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("CLIENT_ID", "android");
					try {
						headers.put("CLIENT_VERSION", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
					} catch (NameNotFoundException e) {
						// impossible
					}
					
					try {
						isConnectionActive.setValue(true);
						final WebSocketClientHandshaker handshaker = new WebSocketClientHandshakerFactory().newHandshaker(uri, WebSocketVersion.V13, null, false, headers);
						
						clientHandler = new ClientHandler(handshaker, IrcTalkService.this);
						clientHandler.putPersistResponseHandler("pushLog", new PushLogHandler(IrcTalkService.this));
						clientHandler.putPersistResponseHandler("updateChannel", new UpdateChannelHandler(IrcTalkService.this));
						
						bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
							@Override
							public ChannelPipeline getPipeline() throws Exception {
								ChannelPipeline pipeline = Channels.pipeline();
								
								pipeline.addLast("decoder", new HttpResponseDecoder());
								pipeline.addLast("encoder", new HttpRequestEncoder());
								pipeline.addLast("ws-handler", clientHandler);
								return pipeline;
							}
						});
						
						logger.debug("connecting");
						ChannelFuture future = bootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
						future.syncUninterruptibly();
						logger.debug("connected");
						
						logger.debug("handshaking");
						channel = (AbstractChannel) future.getChannel();
						handshaker.handshake(channel).syncUninterruptibly();
						logger.debug("handshaked");
						
						// reset reconnect sleep interval to initial value
						reconnectSleepInterval = RECONNECT_SLEEP_INTERVAL_INITIAL;
						
						String gcmRegId = Local.INSTANCE.getOrInitGCMRegistrationId();
						
						if (Local.INSTANCE.isSignedIn()) {
							registerLatch.countDown();
							
							MapBuilder<String, Object> dataBuilder = new MapBuilder<String, Object>(3);
							dataBuilder.put("auth_key", Local.INSTANCE.getAuthKey());
							if (gcmRegId != null) 
								dataBuilder.put("push_type", "gcm").put("push_token", gcmRegId);
							sendRequestSync("login", dataBuilder.build(), new JsonResponseHandler<LoginData>(LoginData.class) {
								@Override
								public void onReceiveData(LoginData data) {
									isLoggedIn = true;
									loginLatch.countDown();
									
									// TODO do something with data.alert
								}
								
								@Override
								public void onReceiveError(int status, String msg) {
									loginLatch.countDown();
								}
							});
						}
						
						registerLatch.await();
						logger.debug("register latch passed");
						
						loginLatch.await();
						logger.debug("login latch passed");
						
						if (!isLoggedIn) 
							close(false);
						
						sendRequestSync("getServers", Collections.emptyMap(), new JsonResponseHandler<GetServersData>(GetServersData.class) {
							@Override
							public void onReceiveData(GetServersData data) {
								servers = data.servers;
								channels = new HashMap<Long, List<Channel>>();
								channelKeyMap = new HashMap<String, Channel>(data.channels.size());
								
								for (Server server : servers) 
									channels.put(server.getId(), new SortedList<Channel>(Channel.NAME_COMPARATOR, true));
								
								for (Channel channel : data.channels) {
									channels.get(channel.getServerId()).add(channel);
									channelKeyMap.put(channel.getChannelKey(), channel);
								}
								
								isConnectionInitialized = true;
								LocalBroadcastManager.getInstance(IrcTalkService.this).sendBroadcast(new Intent(LocalBroadcast.CONNECTION_INITIALIZED));
							}
						}).syncUninterruptibly();
						
						Map<String, Object> reqData = new MapBuilder<String, Object>(2)
								.put("last_log_id", -1)
								.put("log_count", 30)
								.build();
						
						sendRequestSync("getInitLogs", reqData, new JsonResponseHandler<GetInitLogsData>(GetInitLogsData.class) {
							@Override
							public void onReceiveData(GetInitLogsData data) {
								pushLogs(data.logs);
							}
						}).syncUninterruptibly();
						
						// gcm registration id
						if (gcmRegId != null && !Local.INSTANCE.isGCMRegIdSent()) {
							Map<String, Object> data = new MapBuilder<String, Object>(3)
									.put("push_type", "gcm")
									.put("push_token", gcmRegId)
									.put("alert", true)
									.build();
							sendRequest("setNotification", data, new JsonResponseHandler<Void>() {
								@Override
								public void onReceiveData(Void data) {
									Local.INSTANCE.saveGCMRegIdSent(true);
								}
							});
						}
						
						// request queue
						
						WebSocketFrame frame;
						while ((frame = requestQueue.take()) != null) {
							logger.debug("request taken : " + frame.toString());
							if (!channel.isConnected())
								break;
							
							channel.write(frame);
							
							if (frame instanceof CloseWebSocketFrame)
								break;
						}
						
						channel.getCloseFuture().syncUninterruptibly();
					} catch (Exception e) {
						logger.error(null, e);
					} finally {
						if (channel != null)
							channel.close();
						
						isLoggedIn = false;
						isConnectionInitialized = false;
						isConnectionActive.setValue(false);
						requestQueue.clear();
						
						bootstrap.releaseExternalResources();
						
						LocalBroadcastManager.getInstance(IrcTalkService.this).sendBroadcast(new Intent(LocalBroadcast.DISCONNECT));
					}
					
					logger.debug("service main worker looped");
					
					if (!shouldClose && !Thread.currentThread().isInterrupted() ) {
						try {
							logger.info("sleep for " + reconnectSleepInterval + "ms");
							Thread.sleep(reconnectSleepInterval);
							reconnectSleepInterval <<= 1;
							if (reconnectSleepInterval >= RECONNECT_SLEEP_INTERVAL_MAX)
							logger.info("woke up");
						} catch (InterruptedException e) {
							logger.info("main worker thread interrupted while sleeping for reconnecting");
							break;
						}
					}
				}
				
				logger.debug("service main worker thread ended");
			}
		};
		
		ioWorkerThread.start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		logger.debug("service destroyed");
		
		close(true);
	}
	
	/**
	 * type 이 login, register일 경우 이 메소드로 send
	 */
	public <T> ChannelFuture sendRequestSync(String type, T data, final ResponseHandler rawHandler) {
		JsonRequestModel<T> packetModel = new JsonRequestModel<T>(type, data);
		
		ResponseHandler handler = rawHandler;
		if (type.equals("register")) {
			handler = new ResponseHandler() {
				@Override
				public void onReceive(String msg) {
					if (rawHandler != null)
						rawHandler.onReceive(msg);
					registerLatch.countDown();
				}
			};
		} else if (type.equals("login")) {
			handler = new ResponseHandler() {
				@Override
				public void onReceive(String msg) {
					if (rawHandler != null)
						rawHandler.onReceive(msg);
					
					try {
						JsonNode node = JsonResponseHandler.mapper.readTree(msg);
						int status = node.get("status").asInt();
						if (status == 0) 
							isLoggedIn = true;
					} catch (Exception e) {
						logger.error(null, e);
					}
					loginLatch.countDown();
				}
			};
		}
		
		if (handler != null)
			ClientHandler.putResponseHandler(type, packetModel.getMsgId(), handler);
		return channel.write(packetModel.asFrame());
	}
	
	public static <T> void sendRequest(String type, T data, ResponseHandler handler) {
		JsonRequestModel<T> packetModel = new JsonRequestModel<T>(type, data);
		if (handler != null)
			ClientHandler.putResponseHandler(type, packetModel.getMsgId(), handler);
		requestQueue.add(packetModel.asFrame());
	}
	
	public static <T> void sendAck(String type, long msgId, T data) {
		JsonRequestModel<T> packetModel = new JsonRequestModel<T>(type, msgId, data);
		requestQueue.add(packetModel.asFrame());
	}
	
	public void close(boolean shouldClose) {
		if (shouldClose)
			this.shouldClose = shouldClose;
		
		registerLatch.countDown();
		loginLatch.countDown();
		
		synchronized (isConnectionActive) {
			logger.debug("close - isConnectionActive: " + isConnectionActive);
			
			if (isConnectionActive.isTrue()) {
				requestQueue.clear();
				requestQueue.add(new CloseWebSocketFrame());
			}
		}
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
	
	public boolean isConnectionInitialized() {
		return isConnectionInitialized;
	}
	
	
	public List<Server> getServers() {
		return new ArrayList<Server>(servers);
	}
	
	public Server getServer(long serverId) {
		for (Server server : servers)
			if (server.getId().equals(serverId))
				return server;
		return null;
	}
	
	public Map<Long, List<Channel>> getChannels() {
		return new HashMap<Long, List<Channel>>(channels);
	}
	
	public List<Channel> getServerChannels(Server server) {
		return getServerChannels(server.getId());
	}
	
	public List<Channel> getServerChannels(long serverId) {
		return new ArrayList<Channel>(channels.get(serverId));
	}
	
	public Channel getChannel(long serverId, String channelStr) {
		return channelKeyMap.get(Channel.getChannelKey(serverId, channelStr));
	}
	
	public Channel getChannel(String channelKey) {
		return channelKeyMap.get(channelKey);
	}
	
	public void addOrUpdateChannel(Channel newChannel) {
		synchronized (channels) {
			Channel channel = getChannel(newChannel.getServerId(), newChannel.getChannelKey());
			if (channel == null) {
				channels.get(newChannel.getServerId()).add(newChannel);
				channelKeyMap.put(newChannel.getChannelKey(), newChannel);
			} else 
				channel.mergeUpdate(newChannel);
		}
	}
	
	/** 
	 * do not call this method outside (except for persist handlers)
	 * @param channel
	 */
	public void putChannel(Channel channel) {
		channels.get(channel.getServerId()).add(channel);
		channelKeyMap.put(channel.getChannelKey(), channel);
	}
	
	/**
	 * 
	 * @param channel
	 * @return defensive copied {@link Log}s list
	 */
	public Collection<Log> getChannelLogs(Channel channel) {
		logs.putIfAbsent(channel.getChannelKey(), new TreeMap<Long, Log>());
		return logs.get(channel.getChannelKey()).values();
	}
	
	public void pushLog(Log log) {
		putLog(log);
		broadcastLogs(log, false);
	}
	
	public void pushLogSync(Log log) {
		putLog(log);
		broadcastLogs(log, true);
	}
	
	public void pushLogs(List<Log> logs) {
		for (Log log : logs)
			putLog(log);
		broadcastLogs(logs, false);
	}
	
	public void pushLogsSync(List<Log> logs) {
		for (Log log : logs)
			putLog(log);
		broadcastLogs(logs, true);
	}
	
	private boolean putLog(Log log) {
		logs.putIfAbsent(log.getChannelKey(), new TreeMap<Long, Log>());
		TreeMap<Long, Log> channelLogs = logs.get(log.getChannelKey());
		
		synchronized(channelLogs) {
			if (channelLogs.containsKey(log.getLogId()))
				return false;
			
			channelLogs.put(log.getLogId(), log);
		}
		return true;
	}
	
	private void broadcastLogs(Log log, boolean sync) {
		List<Log> logs = new ArrayList<Log>(1);
		logs.add(log);
		broadcastLogs(logs, sync);
	}
	
	private void broadcastLogs(List<Log> logs, boolean sync) {
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

		Set<String> updatedChannelKeys = new HashSet<String>();
		for (Log log : logs) {
			Channel channel = setLastLogIfLast(log);
			updatedChannelKeys.add(channel.getChannelKey());
		}
		
		String logsJson = null;
		String updatedChannelKeysJson = null;
		try {
			logsJson = JsonResponseHandler.mapper.writeValueAsString(logs);
			updatedChannelKeysJson = JsonResponseHandler.mapper.writeValueAsString(updatedChannelKeys);
		} catch (JsonGenerationException e) {
			logger.error(null, e);
		} catch (JsonMappingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}

		// PUSH_LOG
		Intent intent = new Intent(LocalBroadcast.PUSH_LOGS);
		intent.putExtra(LocalBroadcast.EXTRA_LOGS, logsJson);
		
		if (sync)
			localBroadcastManager.sendBroadcastSync(intent);
		else
			localBroadcastManager.sendBroadcast(intent);
		
		intent = new Intent(LocalBroadcast.UPDATE_CHANNELS);
		intent.putExtra(LocalBroadcast.EXTRA_CHANNEL_KEYS, updatedChannelKeysJson);
		localBroadcastManager.sendBroadcast(intent);
	}
	
	private Channel setLastLogIfLast(Log log) {
		Channel channel = channelKeyMap.get(log.getChannelKey());
		if (channel == null)
			return null;
		
		if (channel.getLastLog() == null || channel.getLastLog().getLogId() < log.getLogId())
			channel.setLastLog(log);
		
		return channel;
	}
}
