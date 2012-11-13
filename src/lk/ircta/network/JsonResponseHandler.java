package lk.ircta.network;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;

public abstract class JsonResponseHandler<T> extends ResponseHandler {
	private static final Logger logger = Logger.getLogger(JsonResponseHandler.class);
	
	public static final ObjectMapper mapper = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setPropertyNamingStrategy(LowerCaseWithUnderscoresStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	
	private Class<T> clazz;
	
	public JsonResponseHandler() {};
	
	public JsonResponseHandler(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public void onReceive(String msg) {
		try {
			JsonNode node = mapper.readTree(msg);
			
			int status = node.get("status") != null ? node.get("status").asInt(0) : 0;
			if (status != 0) {
				onReceiveError(status, node.get("msg").asText());
				return;
			}
			
			T data;
			if (clazz == null) {
				data = mapper.readValue(node.get("data").traverse(), new TypeReference<T>() {});
				if (data instanceof Map && ((Map<?, ?>) data).isEmpty()) 
					data = null;
			} else 
				data = mapper.readValue(node.get("data").traverse(), clazz);
			onReceiveData(data);
		} catch (JsonParseException e) {
			onThrowable(e);
		} catch (JsonMappingException e) {
			onThrowable(e);
		} catch (IOException e) {
			onThrowable(e);
		}
	}
	
	public void onReceiveError(int status, String msg) {
		logger.warn(status + " - " + msg);
	}

	public abstract void onReceiveData(T data);
}
