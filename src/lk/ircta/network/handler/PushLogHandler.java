package lk.ircta.network.handler;

import java.util.Map;

import lk.ircta.gcm.GCMIntentService;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.PushLogData;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

public class PushLogHandler extends JsonResponseHandler<PushLogData> {
	private final IrcTalkService talkService;
	
	public PushLogHandler(IrcTalkService ircTalkService) {
		super(PushLogData.class);
		
		this.talkService = ircTalkService;
	}
	
	@Override
	public void onReceiveData(long msgId, PushLogData data) {
		talkService.pushLog(data.log);
		
		if (data.log.isNoti()) {
			GCMIntentService.showPushLogNotification(talkService, data.log);
			
			Map<String, Object> ackData = new MapBuilder<String, Object>(1)
					.put("log_id", data.log.getLogId())
					.build();
			IrcTalkService.sendAck("pushLog", msgId, ackData);
		}
	}
}
