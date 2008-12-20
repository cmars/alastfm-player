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

			try {
				versionText.setText("version " + getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
				versionText.setText("Invalid version -- please check for update");
			}
	   }
}
