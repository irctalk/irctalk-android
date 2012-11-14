package lk.ircta.model;


public class Server implements Model {
	public static class ServerInfo {
		private String host;
		private int port;
		private boolean ssl;
//		private String password;
		
		protected ServerInfo() {}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}
		
		public void setSsl(boolean ssl) {
			this.ssl = ssl;
		}
		
		public boolean isSsl() {
			return ssl;
		}
	}
	
	public static class UserInfo {
		private String nickname;
		private String realname;
		private String altNickname;
		private String loginName;
		
		protected UserInfo() {}

		public String getNickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
		}

		public String getRealname() {
			return realname;
		}

		public void setRealname(String realname) {
			this.realname = realname;
		}

		public String getAltNickname() {
			return altNickname;
		}

		public void setAltNickname(String altNickname) {
			this.altNickname = altNickname;
		}

		public String getLoginName() {
			return loginName;
		}

		public void setLoginName(String loginName) {
			this.loginName = loginName;
		}
	}
	
	private Long id;
	private String name;
	private ServerInfo server;
	private UserInfo user;
	
	protected Server() {}
	
	public Server(String name, String host, int port, boolean ssl, String nickname, String realname) {
		this.name = name;
		
		ServerInfo serverInfo = new ServerInfo();
		serverInfo.host = host;
		serverInfo.port = port;
		serverInfo.ssl = ssl;
		this.server = serverInfo;
		
		UserInfo userInfo = new UserInfo();
		userInfo.nickname = nickname;
		userInfo.realname = realname;
		this.user = userInfo;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ServerInfo getServer() {
		return server;
	}

	public void setServer(ServerInfo server) {
		this.server = server;
	}

	public UserInfo getUser() {
		return user;
	}

	public void setUser(UserInfo user) {
		this.user = user;
	}
}
