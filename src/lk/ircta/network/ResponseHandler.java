package lk.ircta.network;

import org.apache.log4j.Logger;

public abstract class ResponseHandler {
	private static final Logger logger = Logger.getLogger(ResponseHandler.class);
	
	public abstract void onReceive(String msg);
	
	public void onThrowable(Throwable t) {
		logger.error(null, t);
	};
}
