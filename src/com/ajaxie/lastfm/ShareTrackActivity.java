package com.ajaxie.lastfm;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.ajaxie.lastfm.PlayerService.PlayingStatus;
import com.ajaxie.lastfm.PlayerService.Status;

public class ShareTrackActivity extends Activity {
	
	private PlayerService mBoundService;
	private XSPFTrackInfo mTrack;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	
            final TextView titleText = (TextView)findViewById(R.id.share_track_title);
            final TextView authorText = (TextView)findViewById(R.id.share_track_author);
            
            final AutoCompleteTextView recipientText = (AutoCompleteTextView)findViewById(R.id.share_track_recipient);
        	
            mBoundService = ((PlayerService.LocalBinder)service).getService();
            
            ArrayList<FriendInfo> friends = mBoundService.getFriendsList();

    		Status st = mBoundService.getCurrentStatus();
    		if (st instanceof PlayingStatus)
    		{
    			mTrack = ((PlayingStatus)st).getCurrentTrack();
                titleText.setText(mTrack.getTitle());
                authorText.setText("by " + mTrack.getCreator());
                
                ArrayAdapter<FriendInfo> adapter = new ArrayAdapter<FriendInfo>(ShareTrackActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        friends);
                
                recipientText.setAdapter(adapter);       
    		}            
        }
        
        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sharetrack);
        
		if (!bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE))
		{
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
                
        final Button okButton = (Button)findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final AutoCompleteTextView recipientText = (AutoCompleteTextView)findViewById(R.id.share_track_recipient);
                final EditText messageText = (EditText)findViewById(R.id.share_track_message); 
                if (recipientText.getText().toString().equals("")) {
                	recipientText.setError("Please enter recipient nickname");
                	recipientText.requestFocus();
                } else {
                	mBoundService.shareTrack(mTrack, recipientText.getText().toString(), messageText.getText().toString());                
                	setResult(RESULT_OK);
                	finish();
                }                	
            }
        });      	        

        final Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });      	        
   }
}
