package lk.ircta.model;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Log implements Model {
	public static final Comparator<Log> LOG_COMPARATOR = new Comparator<Log>() {
		@Override
		public int compare(Log lhs, Log rhs) {
			return (int) (lhs.getLogId() - rhs.getLogId());
		}
	};
	
	private long serverId;
	private long logId;
	private String channel;
	private String message;
	private String from;
	private boolean noti;
	private long timestamp;
	
	protected Log() {}

	@JsonIgnore
	public String getChannelKey() {
		return Channel.getChannelKey(serverId, channel);
	}
	
	public long getServerId() {
		return serverId;
	}
	
	public void setServerId(long serverId) {
		this.serverId = serverId;
	}

	public long getLogId() {
		return logId;
	}

	public void setLogId(long logId) {
		this.logId = logId;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}
	
	public boolean isNoti() {
		return noti;
	}
	
	public void setNoti(boolean noti) {
		this.noti = noti;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
