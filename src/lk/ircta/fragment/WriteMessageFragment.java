package lk.ircta.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import lk.ircta.R;

public class WriteMessageFragment extends BaseFragment {
	public interface OnSendMessageListener {
		public void onSendMessage(String msg);
	}
	
	private OnSendMessageListener onSendMessageListener;
	
	private static final MultiAutoCompleteTextView.Tokenizer NickTokenizer = new MultiAutoCompleteTextView.Tokenizer() {
		public int findTokenStart(CharSequence text, int cursor) {
			int i = cursor;

			while (i > 0 && text.charAt(i - 1) != ' ') {
				i--;
			}
			while (i < cursor && text.charAt(i) == ' ') {
				i++;
			}

			return i;
		}

		public int findTokenEnd(CharSequence text, int cursor) {
			int i = cursor;
			int len = text.length();

			while (i < len) {
				if (text.charAt(i) == ' ') {
					return i;
				} else {
					i++;
				}
			}

			return len;
		}

		public CharSequence terminateToken(CharSequence text) {
			int i = text.length();

			while (i > 0 && text.charAt(i - 1) == ' ') {
				i--;
			}

			if (i > 0 && text.charAt(i - 1) == ' ') {
				return text;
			} else {
				if (text instanceof Spanned) {
					SpannableString sp = new SpannableString(text + " ");
					TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
					return sp;
				} else {
					return text + " ";
				}
			}
		}
	};
	
	private MultiAutoCompleteTextView messageEditText;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.write_message_fragment, container, false);
		
		messageEditText = (MultiAutoCompleteTextView) view.findViewById(R.id.message);
		final ImageButton sendBtn = (ImageButton) view.findViewById(R.id.send);
		
		// workaround for multi line + actionSend ime
		messageEditText.setMaxLines(3);
		messageEditText.setHorizontallyScrolling(false);
		messageEditText.setTokenizer(NickTokenizer);
		
		messageEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				sendBtn.setEnabled(s.length() > 0);
			}
			
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override public void afterTextChanged(Editable s) {}
		});
		messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					if (v.getText().length() > 0)
						sendBtn.performClick();
					return true;
				}
				
				return false;
			}
		});
		
		sendBtn.setEnabled(false);
		sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onSendMessageListener != null) {
					onSendMessageListener.onSendMessage(messageEditText.getText().toString());
					
					messageEditText.setText("");
				}
			}
		});
		
		return view;
	}

	public void setOnSendMessageListener(OnSendMessageListener listener) {
		this.onSendMessageListener = listener;
	}
	
	public void setAutoCompleteNicknames(String[] nicknames) {
		messageEditText.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.write_message_dropdown_item, nicknames));
	}
}
