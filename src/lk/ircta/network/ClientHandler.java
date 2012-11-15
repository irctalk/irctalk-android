package lk.ircta.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lk.ircta.service.IrcTalkService;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler extends SimpleChannelUpstreamHandler {
	private static final Logger logger = Logger.getLogger(ClientHandler.class);
	
	private final WebSocketClientHandshaker handshaker;
	private final IrcTalkService ircTalkService;
	private final Map<String, ResponseHandler> persistResponseHandlers = new HashMap<String, ResponseHandler>();
	private static final Map<String, ResponseHandler> responseHandlers = new HashMap<String, ResponseHandler>();

	public ClientHandler(WebSocketClientHandshaker handshaker, IrcTalkService ircTalkService) {
		this.handshaker = handshaker;
		this.ircTalkService = ircTalkService;
	}
	
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		logger.debug("handle upstream: " + e.toString());
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Channel ch = ctx.getChannel();
		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
			logger.debug("websocket client connected - " + e);
			return;
		}

		if (e.getMessage() instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) e.getMessage();
			throw new Exception("Unexpected HttpResponse (status=" + response.getStatus() + ", content="
					+ response.getContent().toString(CharsetUtil.UTF_8) + ")");
		}

		WebSocketFrame frame = (WebSocketFrame) e.getMessage();
		if (frame instanceof TextWebSocketFrame) {
			TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
			handleResponse(textFrame.getText());
		} else if (frame instanceof PongWebSocketFrame) {
			// pong
		} else if (frame instanceof CloseWebSocketFrame) {
			ch.close();
		}
	}
	
	private void handleResponse(String msg) {
		try {
			JsonNode root = new ObjectMapper().readTree(msg);
			
			String type = root.get("type").asText();
			int msgId = 0;
			if (root.has("msg_id"))
				msgId = root.get("msg_id").asInt();
			
			String handlerKey = getHandlerKey(type, msgId);
			
			ResponseHandler handler = responseHandlers.get(handlerKey);
			if (handler != null) {
				responseHandlers.remove(handlerKey);
				handler.onReceive(msg);
			} else {
				handler = persistResponseHandlers.get(type);
				if (handler != null) 
					handler.onReceive(msg);
			}
		} catch (JsonProcessingException e) {
			logger.error(null, e);
		} catch (IOException e) {
			logger.error(null, e);
		}
	}
	
	@Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		ircTalkService.close(false);
    }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
		logger.warn(null, e.getCause());
	}
	
	private static String getHandlerKey(String type, long msgId) {
		return msgId == 0 ? type : type + "|" + msgId;
	}
	
	public static void putResponseHandler(String key, ResponseHandler handler) {
		responseHandlers.put(key, handler);
	}
	
	public static void putResponseHandler(String type, long msgId, ResponseHandler handler) {
		responseHandlers.put(getHandlerKey(type, msgId), handler);
	}
	
	public void putPersistResponseHandler(String key, ResponseHandler handler) {
		persistResponseHandlers.put(key, handler);
	}
}
