package lk.ircta.network.handler;

import java.util.Map;

import lk.ircta.model.Log;
import lk.ircta.model.Model;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

public class PushLogHandler extends JsonResponseHandler<PushLogHandler.PushLogData> {
	protected static class PushLogData implements Model {
		public Log log;
		public boolean noti;
		
		protected PushLogData() {};
	}
	
	private final IrcTalkService ircTalkService;
	
	public PushLogHandler(IrcTalkService ircTalkService) {
		super(PushLogHandler.PushLogData.class);
		
		this.ircTalkService = ircTalkService;
	}
	
	@Override
	public void onReceiveData(long msgId, PushLogData data) {
		ircTalkService.pushLog(data.log);
		
		if (data.noti) {
			Map<String, Object> ackData = new MapBuilder<String, Object>(1)
					.put("log_id", data.log.getLogId())
					.build();
			IrcTalkService.sendAck("pushLog", msgId, ackData);
		}
	}
}
