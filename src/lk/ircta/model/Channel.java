package lk.ircta.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Channel implements Model {
	private long serverId;
	private String channel;
	private String topic;
	private List<String> members;
	private Log lastLog;
	
	public Channel(long serverId, String channel) {
		this.serverId = serverId;
		this.channel = channel;
	}
	
	protected Channel() {}
	
	@JsonIgnore
	public String getChannelKey() {
		return serverId + "|" + channel.toLowerCase();
	}
	
	public long getServerId() {
		return serverId;
	}
	
	public void setServerId(long serverId) {
		this.serverId = serverId;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public List<String> getMembers() {
		return members;
	}
	
	public void setMembers(List<String> memebers) {
		this.members = memebers;
	}

	public Log getLastLog() {
		return lastLog;
	}

	public void setLastLog(Log lastLog) {
		this.lastLog = lastLog;
	}
}
