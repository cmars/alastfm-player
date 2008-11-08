package com.ajaxie.lastfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity {
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.about);
	        
			TextView versionText = (TextView)findViewById(R.id.version);
			Button resetButton = (Button)findViewById(R.id.reset_button);

			resetButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
		            new AlertDialog.Builder(AboutActivity.this)
	                .setIcon(R.drawable.alert_dialog_icon)
	                .setTitle("Are you really want to reset all settings, including your saved last.fm username and password?")
	                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	    					SharedPreferences settings = getSharedPreferences(LastFMPlayer.PREFS_NAME, 0);
	    					SharedPreferences.Editor ed = settings.edit();
	    					ed.clear();
	    					ed.commit();
	                    }
	                })
	                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {

	                        /* User clicked Cancel so do some stuff */
	                    }
	                })
	                .show();									
				}
				
			});
			try {
				versionText.setText("version " + getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
				versionText.setText("Invalid version -- please check for update");
			}
	   }
}
