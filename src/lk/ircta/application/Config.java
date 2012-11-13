package lk.ircta.application;

public abstract class Config {
	public static final String IRCTALK_SERVICE_URL = "ws://laika.redfeel.net:9001/";

	public static final String GOOGLE_AUTH_CLIENT_ID = "812906460657-6m0d0bsmrtqjt2h9jd29kui3pdg69eb2.apps.googleusercontent.com";
	public static final String GOOGLE_AUTH_SCOPE = "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile";
	public static final String GOOGLE_AUTH_REDIRECT_URI = "http://localhost";
	public static final String GOOGLE_AUTH_RESPONSE_TYPE = "token";
	
	public static final String GCM_SENDER_ID = "812906460657";

	public static final String CRASH_REPORTS_FORM_KEY = "dFU0dUhVWDFZYlRnVkUxNWtuV0czcXc6MQ";
}
