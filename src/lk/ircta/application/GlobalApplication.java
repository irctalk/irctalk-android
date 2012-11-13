package lk.ircta.application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;

import android.app.Application;
import android.os.Handler;

@ReportsCrashes(formKey = Config.CRASH_REPORTS_FORM_KEY)
public class GlobalApplication extends Application {
	private static volatile GlobalApplication instance;
	
	private Handler handler;
	
	public static GlobalApplication getInstance() {
		return instance;
	}
	
	@Override
	public void onCreate() {
		instance = this;
		handler = new Handler(getMainLooper());
		ACRA.init(this);
		configure();
	}
	
	private static void configure() {
		final LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setUseFileAppender(false);
//        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "myapp.log");
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public void runOnUiThread(Runnable runnable) {
		handler.post(runnable);
	}
}
