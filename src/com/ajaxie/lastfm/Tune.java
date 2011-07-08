package com.ajaxie.lastfm;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Tune extends ListActivity {
	MyListAdapter mAdapter;
	protected static final int ENTER_ARTIST = 0;
	protected static final int ENTER_TAG = 1;
	protected static final int CHOOSE_FRIEND = 2;
	String mUsername;	

	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);	        	       
	        mAdapter = new MyListAdapter();

        	SharedPreferences settings = getSharedPreferences(LastFMPlayer.PREFS_NAME, 0);
	        mUsername = settings.getString("username", "empty");

	        setListAdapter(new ArrayAdapter<String>(this,
	                android.R.layout.simple_list_item_single_choice, STATION_TYPES));

		    getListView().setItemsCanFocus(false);
		    getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		    
	        Uri stationUri = LastFMPlayer.getStationUri(settings);
	        if (stationUri != null)
	        {
	        	List<String> path = stationUri.getPathSegments();
	        	if (stationUri.getScheme().equals("lastfm")) {
	        		if (stationUri.getAuthority().equals("artist"))
	        			getListView().setItemChecked(STATION_TYPE_ARTIST, true);
	        		else
	        			if (stationUri.getAuthority().equals("globaltags"))
		        			getListView().setItemChecked(STATION_TYPE_TAG, true);
	        			else
	        				if (stationUri.getAuthority().equals("user")) {
	        					if (path.size() >= 2 && path.get(1).equals("neighbours"))
	        						getListView().setItemChecked(STATION_TYPE_NEIGHBOR, true);
	        					if (path.size() >= 2 && path.get(1).equals("recommended"))
	        						getListView().setItemChecked(STATION_TYPE_RECOMMENDED, true);
	        					if (path.size() >= 2 && path.get(1).equals("personal"))
	        					{
	        						if (path.get(0).equals(mUsername))
	        							getListView().setItemChecked(STATION_TYPE_PERSONAL, true);
	        						else
		        						getListView().setItemChecked(STATION_TYPE_FRIENDS, true);
	        					}
	        					if (path.size() >= 2 && path.get(1).equals("playlist"))
	        						getListView().setItemChecked(STATION_TYPE_PLAYLIST, true);
	        				}
	        	}
	        }

		    getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Uri.Builder builder = new Uri.Builder();
				builder.scheme("lastfm");
				builder.fragment("");
				builder.query("");
				switch (arg2) {
				case STATION_TYPE_NEIGHBOR:
					builder.authority("user");
					builder.appendPath(mUsername);
					builder.appendPath("neighbours");					
					setResult(RESULT_OK, new Intent("play", builder.build(),
							Tune.this, LastFMPlayer.class));
					finish();										
					break;				
				case STATION_TYPE_PLAYLIST:
					builder.authority("user");
					builder.appendPath(mUsername);
					builder.appendPath("playlist");					
					setResult(RESULT_OK, new Intent("play", builder.build(),
							Tune.this, LastFMPlayer.class));
					finish();										
					break;				
				case STATION_TYPE_RECOMMENDED:
					builder.authority("user");
					builder.appendPath(mUsername);
					builder.appendPath("recommended");					
					builder.appendPath("100");					
					setResult(RESULT_OK, new Intent("play", builder.build(),
							Tune.this, LastFMPlayer.class));
					finish();										
					break;				
				case STATION_TYPE_PERSONAL:
					builder.authority("user");
					builder.appendPath(mUsername);
					builder.appendPath("personal");					
					setResult(RESULT_OK, new Intent("play", builder.build(),
							Tune.this, LastFMPlayer.class));
					finish();										
					break;									
				case STATION_TYPE_ARTIST:
					startActivityForResult(new Intent(Tune.this,
							EnterArtistName.class), ENTER_ARTIST);
					break;
				case STATION_TYPE_TAG:
					startActivityForResult(new Intent(Tune.this,
							EnterTag.class), ENTER_TAG);
					break;
				case STATION_TYPE_FRIENDS:
					startActivityForResult(new Intent(Tune.this,
							ChooseFriendActivity.class), CHOOSE_FRIEND);
					break;
				}
			}
	    	   
	       });

//		   setListAdapter(mAdapter);	        
	        
	   }
	   
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (requestCode == ENTER_ARTIST) {
				if (resultCode == RESULT_OK) {
						Uri.Builder builder = new Uri.Builder();
						builder.scheme("lastfm");
						builder.fragment("");
						builder.query("");
						builder.authority("artist");
						builder.appendPath(data.getStringExtra("result"));
						builder.appendPath("similarartists");		
						Uri stationUri = builder.build();
						setResult(RESULT_OK, new Intent("play", stationUri,
								this, LastFMPlayer.class));
						finish();
					}
				}
			else
				if (requestCode == ENTER_TAG && resultCode == RESULT_OK) {
					Uri.Builder builder = new Uri.Builder();
					builder.scheme("lastfm");
					builder.fragment("");
					builder.query("");
					builder.authority("globaltags");
					builder.appendPath(data.getStringExtra("result"));
					Uri stationUri = builder.build();
					setResult(RESULT_OK, new Intent("play", stationUri,
							this, LastFMPlayer.class));
					finish();					
				}
				else 
					if (requestCode == CHOOSE_FRIEND && resultCode == RESULT_OK) {
						Uri.Builder builder = new Uri.Builder();
						builder.scheme("lastfm");
						builder.fragment("");
						builder.query("");
						builder.authority("user");
						builder.appendPath(data.getStringExtra("result"));
						builder.appendPath("personal");
						Uri stationUri = builder.build();
						setResult(RESULT_OK, new Intent("play", stationUri,
								this, LastFMPlayer.class));
						finish();											
					}
					
		}	   
	   
	   private final static String[] STATION_TYPES = new String[] {
		   		"Artist", "Tag", "My Recommendations",
		        "My Radio Station",		        
		        "My Neighbour Radio", "Friends Radio", "My Playlist"} ;
	   
	   private final int STATION_TYPE_ARTIST = 0;
	   private final int STATION_TYPE_TAG = 1;
	   private final int STATION_TYPE_RECOMMENDED = 2;
	   private final int STATION_TYPE_PERSONAL = 3;
	   private final int STATION_TYPE_NEIGHBOR = 4;
	   private final int STATION_TYPE_FRIENDS = 5;
	   private final int STATION_TYPE_PLAYLIST = 6;
//	   private final int STATION_TYPE_GROUP = 6;
//	   private final int STATION_TYPE_LOVED = 7;
	   
	   
	    /**
	     * A simple adapter which maintains an ArrayList of photo resource Ids. 
	     * Each photo is displayed as an image. This adapter supports clearing the
	     * list of photos and adding a new photo.
	     *
	     */
	    public class MyListAdapter extends BaseAdapter {
	        // Sample data set.  children[i] contains the children (String[]) for groups[i].
	    		        
	        public TextView getTextView(String text) {
	            // Layout parameters for the ExpandableListView
	            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	                    ViewGroup.LayoutParams.FILL_PARENT, 64);

	            TextView textView = new TextView(Tune.this);
	            textView.setText(text);
	            textView.setLayoutParams(lp);
	            // Center the text vertically
	            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	            // Set the text starting position
	            textView.setPadding(36, 0, 0, 0);
	            return textView;
	        }	   

	        public String getItemText(int position) {
	            return STATION_TYPES[position];
	        }

	        public int getCount() {
	            return STATION_TYPES.length;
	        }
	        
	        
	        public View getView(int position, View convertView, ViewGroup parent)
	        {	        	
        			return getTextView(getItemText(position));
	        }
	        
	        public boolean hasStableIds() {
	            return true;
	        }

			@Override
			public Object getItem(int position) {
				return getItemText(position);
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

	    }
	   
}
