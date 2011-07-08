package com.ajaxie.lastfm;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class EnterNameActivity extends Activity {

	private static class LruList {
		ArrayList<String> items;
		String mName;
		static int LRU_LIST_SIZE = 10;

		public LruList(String name) {
			items = new ArrayList<String>(LRU_LIST_SIZE);
			mName = name;
		}

		public void insertItem(String newItem) {
			for (int i = 0; i < items.size(); i++)
				if (items.get(i).equals(newItem)) {
					for (int j = 1; j <= i; j++)
						items.set(j, items.get(j - 1));
					items.set(0, newItem);
					return;
				}

			items.add(0, newItem);
			if (items.size() > LRU_LIST_SIZE)
				items.remove(items.size() - 1);
		}

		public void save(SharedPreferences prefs) {
			SharedPreferences.Editor ed = prefs.edit();
			for (int i = 0; i < items.size(); i++)
				ed.putString("lru_" + mName + "_" + Integer.toString(i), items
						.get(i));
			boolean res = ed.commit();
		}

		public void load(SharedPreferences prefs) {
			for (int i = 0; i < LRU_LIST_SIZE; i++) {
				String val = prefs.getString("lru_" + mName + "_"
						+ Integer.toString(i), null);
				if (val == null)
					return;
				items.add(val);
			}

		}
		
		public String[] getItems() {
			return items.toArray(new String[] {});
		}
		
		public int getItemCount() {
			return items.size();
		}
	}

	LruList lru;

	int layoutId;
	String listId;
	String hint;
	String defaultName = "";
	
	public EnterNameActivity(int layout, String listId, String hint) {
		layoutId = layout;
		this.hint = hint;
		this.listId = listId;
	}
	
	public void setDefaultName(String name) {
		defaultName = name;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);

		final AutoCompleteTextView nameText = (AutoCompleteTextView) findViewById(R.id.station_name);
		SharedPreferences settings = getSharedPreferences(
				LastFMPlayer.PREFS_NAME, 0);
		lru = new LruList(listId);
		lru.load(settings);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, lru.getItems());
		
		nameText.setAdapter(adapter);
		nameText.setThreshold(1);
		nameText.setText(defaultName);
		
		nameText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && lru.getItemCount() > 0 && nameText.getText().toString().equals(""))
					nameText.showDropDown();
			}			
		});


		final Button okButton = (Button) findViewById(R.id.ok_button);
		
		final View.OnClickListener okOnClickListener = new View.OnClickListener() {
			public void onClick(View v) {
                if (nameText.getText().toString().equals("")) {
                	nameText.setError(hint);
                	nameText.requestFocus();
                } else {
					SharedPreferences settings = getSharedPreferences(
							LastFMPlayer.PREFS_NAME, 0);
					lru.insertItem(nameText.getText().toString());
					lru.save(settings);
					Intent res = new Intent(Intent.ACTION_MAIN);
					res.putExtra("result", nameText.getText().toString());
					setResult(RESULT_OK, res);
					finish();
                }
			}
		};
		
		okButton.setOnClickListener(okOnClickListener);

		nameText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER)
				{
					okOnClickListener.onClick(nameText);
					return true;
				} else
					return false;
			}			
		});
		
		
		final Button cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
}
