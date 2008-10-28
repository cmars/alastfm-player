package com.ajaxie.lastfm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class UserInfo extends Activity {
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.userinfo);
	        
	        final EditText usernameText = (EditText)findViewById(R.id.username);
	        final EditText passwordText = (EditText)findViewById(R.id.password);
	        
        	SharedPreferences settings = getSharedPreferences(LastFMPlayer.PREFS_NAME, 0);
	        usernameText.setText(settings.getString("username", ""));
	        passwordText.setText(settings.getString("password", ""));
	        final Button okButton = (Button)findViewById(R.id.ok_button);
	        okButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	    	        	    	        
	            	SharedPreferences settings = getSharedPreferences(LastFMPlayer.PREFS_NAME, 0);
	            	SharedPreferences.Editor ed = settings.edit();
	            	ed.putString("username", usernameText.getText().toString());
	            	ed.putString("password", passwordText.getText().toString());
	            	ed.commit();
	                setResult(RESULT_OK);
	                finish();
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
