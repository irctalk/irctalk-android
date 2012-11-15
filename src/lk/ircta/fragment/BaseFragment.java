package lk.ircta.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.actionbarsherlock.app.SherlockFragment;
import lk.ircta.activity.BaseActivity;
import lk.ircta.service.IrcTalkService;
import lk.ircta.service.OnBindServiceListener;

public class BaseFragment extends SherlockFragment implements OnBindServiceListener {
	protected LocalBroadcastManager localBroadcastManager;

	private boolean isServiceBoundOnAttach;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (activity instanceof BaseActivity) {
			if (((BaseActivity) activity).getIrcTalkService() == null) 
				((BaseActivity) activity).addOnBindServiceListener(this);
			else 
				isServiceBoundOnAttach = true;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (isServiceBoundOnAttach && getActivity() != null && getIrcTalkService().isConnectionInitialized())
			onBindService(getIrcTalkService());
	}
	
	/**
	 * called when {@link IrcTalkService} is bound.<br>
	 * @param talkService 
	 */
	public void onBindService(IrcTalkService talkService) {};
	
	public void runOnUiThread(final Runnable r) {
		Activity activity = getActivity();
		if (activity == null) 
			return;
		
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (getActivity() == null)
					return;
				
				r.run();
			}
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Activity activity = getActivity();
		if (activity != null && activity instanceof BaseActivity) 
			((BaseActivity) activity).removeOnBindServiceListener(this);
	}
	
	/**
	 * 
	 * @return {@link IrcTalkService} nullable
	 */
	protected IrcTalkService getIrcTalkService() {
		if (getActivity() instanceof BaseActivity)
			return ((BaseActivity) getActivity()).getIrcTalkService();
		return null;
	}
}
