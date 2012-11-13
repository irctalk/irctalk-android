package lk.ircta.network.handler;

import lk.ircta.model.Log;
import lk.ircta.model.Model;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.service.IrcTalkService;

public class PushLogHandler extends JsonResponseHandler<PushLogHandler.PushLogData> {
	protected static class PushLogData implements Model {
		public Log log;
		
		protected PushLogData() {};
	}
	
	private final IrcTalkService ircTalkService;
	
	public PushLogHandler(IrcTalkService ircTalkService) {
		super(PushLogHandler.PushLogData.class);
		
		this.ircTalkService = ircTalkService;
	}
	
	@Override
	public void onReceiveData(PushLogData data) {
		ircTalkService.pushLog(data.log);
		
//		ircTalkService.sendRequest("pushLog", data, null);
	}
}
