package lk.ircta.local;

import lk.ircta.model.Channel;
import lk.ircta.model.Log;

public abstract class LocalBroadcast {
	public static final String CONNECTION_INITIALIZED = "connection_initialized";
	public static final String DISCONNECT = "disconnect";
	
	/**
	 * {@link #EXTRA_LOGS}: [{@link Log}]
	 */
	public static final String PUSH_LOGS = "push_logs";
	
	/**
	 * {@link #EXTRA_CHANNEL}: {@link Channel}
	 */
	public static final String ADD_CHANNEL = "add_channel";
	
	public static final String EXTRA_LOGS = "log";
	public static final String EXTRA_CHANNEL = "channel";
}
