package lk.ircta.network;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;

public class JsonRequestModel<T> {
	private static final ObjectMapper mapper = new ObjectMapper()
			.setPropertyNamingStrategy(LowerCaseWithUnderscoresStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	private static final AtomicLong msgIdCounter = new AtomicLong(1);

	protected String type;
	protected long msgId;
	protected T data;

	protected JsonRequestModel() {
	}

	public JsonRequestModel(String type, T data) {
		this.type = type;
		this.msgId = msgIdCounter.getAndIncrement();
		this.data = data;
	}

	public TextWebSocketFrame asFrame() {
		try {
			return new TextWebSocketFrame(mapper.writeValueAsString(this));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String getType() {
		return type;
	}

	public long getMsgId() {
		return msgId;
	}

	public T getData() {
		return data;
	}
}
