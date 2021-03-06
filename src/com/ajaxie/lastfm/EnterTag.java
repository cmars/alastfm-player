package com.ajaxie.lastfm;

import java.util.List;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

public class EnterTag extends EnterNameActivity {

	public EnterTag() {
		super(R.layout.enter_tag, "tag", "Please enter some tag name");
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	SharedPreferences settings = getSharedPreferences(LastFMPlayer.PREFS_NAME, 0);		
        Uri stationUri = LastFMPlayer.getStationUri(settings);
        
        if (stationUri != null)
        {
        	List<String> path = stationUri.getPathSegments();
        	if (stationUri.getScheme().equals("lastfm")) {
    			if (stationUri.getAuthority().equals("globaltags") && path.size() > 0)
        			super.setDefaultName(path.get(0));
        	}
        }
		
		super.onCreate(savedInstanceState);
	}

}
