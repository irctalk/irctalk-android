package lk.ircta.activity;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import lk.ircta.R;
import lk.ircta.model.Server;
import lk.ircta.network.JsonResponseHandler;
import lk.ircta.network.datamodel.AddServerData;
import lk.ircta.service.IrcTalkService;
import lk.ircta.util.MapBuilder;

public class AddServerActivity extends BaseActivity {
	private static final int MENU_ADD_SERVER = 100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_server_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ADD_SERVER, Menu.NONE, "Done").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		case MENU_ADD_SERVER:
			String name = ((EditText) findViewById(R.id.name)).getText().toString();
			String host = ((EditText) findViewById(R.id.host)).getText().toString();
			int port = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
			boolean ssl = ((CheckBox) findViewById(R.id.ssl)).isChecked();
			String nickname = ((EditText) findViewById(R.id.nickname)).getText().toString();
			String realname = ((EditText) findViewById(R.id.realname)).getText().toString();
			
			if (StringUtils.isBlank(name)) 
				name = host;
			
			if (StringUtils.isBlank(host) || port == 0 || StringUtils.isBlank(nickname)) {
				// TODO alert
			}
			
			final ProgressDialog progressDialog = ProgressDialog.show(this, null, "잠시만 기다려주세요..", true);
			
			Map<String, Object> data = new MapBuilder<String, Object>(1)
					.put("server", new Server(name, host, port, ssl, nickname, realname))
					.build();
			IrcTalkService.sendRequest("addServer", data, new JsonResponseHandler<AddServerData>(AddServerData.class) {
				@Override
				public void onReceiveData(AddServerData data) {
					progressDialog.dismiss();
					setResult(RESULT_OK);
					finish();
				}
				
				@Override
				public void onReceiveError(int status, String msg) {
					super.onReceiveError(status, msg);
					progressDialog.dismiss();
				}
			});
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
