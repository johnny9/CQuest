package edu.illinois.CS598rhk;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MessageActivity extends Activity {
	protected static final String MSG_TAG = "AdhocClient";
	public static MessageActivity currentInstance = null;
	private AdhocClient application = null;
	private Button buttonSend = null;
	private Button buttonPrev = null;
	private Button buttonNext = null;
	private EditText messageBody = null;
	private TextView messageTitle = null;
	private FriendData curFriend = null; 
	
	private static void setCurrent(MessageActivity current){
		MessageActivity.currentInstance = current;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.messageview);

	  MessageActivity.setCurrent(this);
	  
	  this.application = (AdhocClient)this.getApplication();
	  this.messageTitle=(TextView)findViewById(R.id.MessageTitle);
	  this.messageBody=(EditText)findViewById(R.id.MessageBody);
	  
	  this.buttonSend = (Button)findViewById(R.id.Send);
      this.buttonSend.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "Send pressed ...");
				//application.sendMessage(curFriend, messageBody.getText().toString());
				setResult(RESULT_OK);
				finish();
			}
		});
	  
      this.buttonPrev = (Button)findViewById(R.id.Prev);
      this.buttonPrev.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "Prev pressed ...");
				showIncomingMessage(--application.curMessageIndex);
			}
		});

      this.buttonNext = (Button)findViewById(R.id.Next);
      this.buttonNext.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "Next pressed ...");
				showIncomingMessage(++application.curMessageIndex);
			}
		});
      
      if (this.application.curMessageIndex != -1) {
    	  showIncomingMessage(this.application.curMessageIndex);
      }
      else if (this.application.curFriendIndex != -1) {
    	  showSendMessage(this.application.curFriendIndex);
      }
	}
	
	void showIncomingMessage(int msgIndex) {
		this.messageTitle.setText(this.application.getMessageHeader(msgIndex));
		this.messageBody.setText(this.application.getMessage(msgIndex),
				  TextView.BufferType.NORMAL);   // read-only
		this.messageBody.setEnabled(false);
		this.buttonSend.setVisibility(View.GONE);
		this.buttonPrev.setVisibility(View.VISIBLE);
		this.buttonNext.setVisibility(View.VISIBLE);
		this.buttonPrev.setEnabled(msgIndex > 0);
		this.buttonNext.setEnabled(msgIndex < this.application.getMessageList().size()-1);
	}
	
	void showSendMessage(int friendIndex) {
  	    curFriend=this.application.getActiveFriends().get(friendIndex);
	    this.messageTitle.setText(
			  String.valueOf("Send message to " + curFriend.name + " at " + 
					         curFriend.IPaddress));
	    this.messageBody.setText("", TextView.BufferType.EDITABLE);
	    this.messageBody.setEnabled(true);
	    this.buttonSend.setVisibility(View.VISIBLE);
	    this.buttonPrev.setVisibility(View.GONE);
	    this.buttonNext.setVisibility(View.GONE);
	}
}
