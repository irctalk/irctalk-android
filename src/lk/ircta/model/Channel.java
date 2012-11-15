package lk.ircta.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Channel implements Model {
	public static final Comparator<Channel> NAME_COMPARATOR = new Comparator<Channel>() {
		@Override
		public int compare(Channel lhs, Channel rhs) {
			return lhs.channel.compareTo(rhs.channel);
		}
	};
	private Long serverId;
	private String channel;
	private String topic;
	private Set<String> members;
	@JsonIgnore private Log lastLog;
	
	public Channel(long serverId, String channel) {
		this.serverId = serverId;
		this.channel = channel;
	}
	
	protected Channel() {}
	
	public void mergeUpdate(Channel newChannel) {
		if (newChannel.serverId != null)
			this.serverId = newChannel.serverId;
		if (newChannel.channel != null)
			this.channel = newChannel.channel;
		if (newChannel.topic != null)
			this.topic = newChannel.topic;
		
		for (String member : newChannel.members) {
			String nick = member.substring(1);
			switch (member.charAt(0)) {
			case '+':
				members.add(nick);
				break;
			case '-':
				members.remove(nick);
				break;
			}
		}
	}
	
	@JsonIgnore
	public String getChannelKey() {
		return getChannelKey(serverId, channel);
	}
	
	public Long getServerId() {
		return serverId;
	}
	
	public void setServerId(Long serverId) {
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
	
	public Set<String> getMembers() {
		if (members == null)
			members = new HashSet<String>();
		return members;
	}
	
	public void setMembers(Set<String> memebers) {
		this.members = memebers;
	}

	public Log getLastLog() {
		return lastLog;
	}

	public void setLastLog(Log lastLog) {
		this.lastLog = lastLog;
	}
	
	public static final String getChannelKey(long serverId, String channelStr) {
		return serverId + "|" + channelStr.toLowerCase(Locale.getDefault());
	}
}
