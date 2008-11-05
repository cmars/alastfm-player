package com.ajaxie.lastfm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import com.ajaxie.lastfm.PlayerService.PlayingStatus;
import com.ajaxie.lastfm.PlayerService.Status;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ViewSwitcher;
import android.widget.Gallery.LayoutParams;

public class LastFMPlayer extends Activity {

	private static final int DIALOG_ERROR = 1;

	public static final String PREFS_NAME = "LastFMSettings";

	protected static final int SET_USER_INFO = 0;
	protected static final int SHARE_TRACK = 1;

	public static final int MENU_SETTINGS_ID = Menu.FIRST;
	public static final int MENU_ABOUT_ID = Menu.FIRST + 1;

	protected static final String TAG = "LastFMPlayer";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);		
		menu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_help);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS_ID:
			startActivityForResult(
					new Intent(LastFMPlayer.this, UserInfo.class),
					SET_USER_INFO);
			return true;
		case MENU_ABOUT_ID:
			startActivity(new Intent(LastFMPlayer.this, AboutActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_ERROR)
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(
					R.string.alert_dialog_single_choice).setPositiveButton(
					R.string.alert_dialog_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							/* User clicked Yes so do some stuff */
						}
					}).setMessage("Error").create();

		return null;
	}

	private PlayerService mBoundService;

	public class LastFMServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((PlayerService.LocalBinder) service).getService();

			onServiceStarted();
			// Tell the user about this for our demo.
			Toast.makeText(LastFMPlayer.this, R.string.local_service_connected,
					Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(LastFMPlayer.this,
					R.string.local_service_disconnected, Toast.LENGTH_SHORT)
					.show();
		}
	};

	class LastFMNotificationListener extends
			PlayerService.LastFMNotificationListener {

		@Override
		public void onLoved(boolean success) {
			LastFMPlayer.this.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(LastFMPlayer.this,
							R.string.track_loved, Toast.LENGTH_SHORT)
							.show();
				}
			});
		}

		@Override
		public void onShared(boolean success) {
			LastFMPlayer.this.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(LastFMPlayer.this,
							R.string.track_shared, Toast.LENGTH_SHORT)
							.show();
				}
			});
		}

		@Override
		public void onBanned(boolean success) {
			LastFMPlayer.this.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(LastFMPlayer.this,
							R.string.track_banned, Toast.LENGTH_SHORT)
							.show();
				}
			});
		}
		
	}

	void resetSongInfoDisplay() {
		TextView statusText = (TextView) LastFMPlayer.this
				.findViewById(R.id.status_text);
		TextView timeText = (TextView) LastFMPlayer.this
				.findViewById(R.id.time_counter);
		TextView creatorText = (TextView) LastFMPlayer.this
				.findViewById(R.id.creator_name_text);
		TextView albumText = (TextView) LastFMPlayer.this
				.findViewById(R.id.album_name_text);
		TextView trackText = (TextView) LastFMPlayer.this
				.findViewById(R.id.track_name_text);
		ImageSwitcher albumView = (ImageSwitcher) LastFMPlayer.this
				.findViewById(R.id.album_view);

		statusText.setText("Disconnected");
		timeText.setText("--:--");
		creatorText.setText("");
		albumText.setText("");
		trackText.setText("");
		albumView.setImageDrawable(null);
	}

	class StatusRefreshTask extends TimerTask {

		Bitmap prevBitmap;

		@Override
		public void run() {
			if (mBoundService != null) {
				final PlayerService.Status status = mBoundService
						.getCurrentStatus();

				if (status != null) {
					final String statusString = status.toString();
					LastFMPlayer.this.runOnUiThread(new Runnable() {

						public void run() {
							if (mBoundService == null)
								return;
							TextView errorText = (TextView) LastFMPlayer.this
									.findViewById(R.id.error_text);
							TextView statusText = (TextView) LastFMPlayer.this
									.findViewById(R.id.status_text);
							TextView timeText = (TextView) LastFMPlayer.this
									.findViewById(R.id.time_counter);
							TextView creatorText = (TextView) LastFMPlayer.this
									.findViewById(R.id.creator_name_text);
							TextView albumText = (TextView) LastFMPlayer.this
									.findViewById(R.id.album_name_text);
							TextView trackText = (TextView) LastFMPlayer.this
									.findViewById(R.id.track_name_text);
							ImageSwitcher albumView = (ImageSwitcher) LastFMPlayer.this
									.findViewById(R.id.album_view);

							if (status instanceof PlayerService.ErrorStatus) {
								statusText.setText("Error");
								errorText.setText(statusString);
								errorText.setVisibility(View.VISIBLE);
							} else {
								final ImageButton loveButton = (ImageButton) findViewById(R.id.love_button);
								final ImageButton banButton = (ImageButton) findViewById(R.id.ban_button);
								final ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);

								loveButton.setEnabled(!mBoundService
										.isCurrentTrackLoved());
								banButton.setEnabled(!mBoundService
										.isCurrentTrackBanned());
								shareButton.setEnabled(true);

								statusText.setText(statusString);
								errorText.setVisibility(View.INVISIBLE);
							}

							if (status instanceof PlayerService.PlayingStatus) {
								int pos = ((PlayerService.PlayingStatus) status)
										.getCurrentPosition();
								final XSPFTrackInfo track = ((PlayerService.PlayingStatus) status)
										.getCurrentTrack();
								if (track != null) {
									int minutes = (track.getDuration() - pos)
											/ (1000 * 60);
									int seconds = ((track.getDuration() - pos) / 1000) % 60;

									timeText.setText(String.format(
											"%1$02d:%2$02d", minutes, seconds));
									creatorText.setText(track.getCreator());
									albumText.setText(track.getAlbum());
									if (prevBitmap != track.getBitmap()) {
										albumView
												.setImageDrawable(new BitmapDrawable(
														track.getBitmap()));
										prevBitmap = track.getBitmap();
									}
									trackText.setText(track.getTitle());
								}
							}
						}
					});
				}
			}
		}
	}

	void bindToPlayerService() {
		if (!LastFMPlayer.this.bindService(new Intent(LastFMPlayer.this,
				PlayerService.class), new LastFMServiceConnection(),
				Context.BIND_AUTO_CREATE))
			LastFMPlayer.this.showDialog(DIALOG_ERROR);
	}

	Timer refreshTimer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		refreshTimer = new Timer();
		refreshTimer.scheduleAtFixedRate(new StatusRefreshTask(), 1000, 1000);

		TextView statusText = (TextView) LastFMPlayer.this
				.findViewById(R.id.status_text);
		try {
			statusText.setText("version "
					+ getPackageManager().getPackageInfo(this.getPackageName(),
							0).versionName);
		} catch (NameNotFoundException e) {
			statusText.setText("Invalid version -- please check for update");
		}

		final Spinner stationSpinner = (Spinner) findViewById(R.id.station_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.station_types,
				android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stationSpinner.setAdapter(adapter);

		final EditText stationName = (EditText) findViewById(R.id.station_name);

		bindToPlayerService();

		final ImageButton playButton = (ImageButton) findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (stationName.getText().toString().equals("")) {
					stationName
							.setError("Please enter the station name to play");
					return;
				}

				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				String username = settings.getString("username", null);
				String password = settings.getString("password", null);
				if ((username == null || password == null)
						|| (username.length() == 0 || password.length() == 0))
					startActivityForResult(new Intent(LastFMPlayer.this,
							UserInfo.class), SET_USER_INFO);
				else {
					Uri stationUri = getStationUri();

					SharedPreferences.Editor ed = settings.edit();
					ed.putInt("last_station_type", stationSpinner
							.getSelectedItemPosition());
					ed.putString("last_station_name", stationName.getText()
							.toString());
					ed.commit();

					Intent serviceIntent = new Intent();
					serviceIntent.setAction(Intent.ACTION_VIEW);
					serviceIntent.setClass(LastFMPlayer.this,
							PlayerService.class);
					serviceIntent.setData(stationUri);

					/*
					 * bindToPlayerService();
					 * LastFMPlayer.this.startService(serviceIntent);
					 */
					if (mBoundService == null) {
						if (!LastFMPlayer.this.bindService(serviceIntent,
								new LastFMServiceConnection(),
								Context.BIND_AUTO_CREATE))
							LastFMPlayer.this.showDialog(DIALOG_ERROR);
						LastFMPlayer.this.startService(serviceIntent);
					} else {
						LastFMPlayer.this.startService(serviceIntent);
						// mBoundService.startPlaying(stationUri.toString());
					}

					/*
					 * if (mBoundService != null)
					 * mBoundService.startPlaying(stationUrl);
					 */
				}
			}
		});

		final ImageButton stopButton = (ImageButton) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null) {
					mBoundService.stopPlaying();
					// LastFMPlayer.this.unbindService(mConnection);
					mBoundService = null;
					Intent serviceIntent = new Intent(LastFMPlayer.this,
							PlayerService.class);
					LastFMPlayer.this.stopService(serviceIntent);
					resetSongInfoDisplay();
				}
			}
		});

		final ImageButton skipButton = (ImageButton) findViewById(R.id.skip_button);
		skipButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null)					
					mBoundService.skipCurrentTrack();
			}
		});

		final ImageButton banButton = (ImageButton) findViewById(R.id.ban_button);
		banButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null) {
					if (mBoundService.banCurrentTrack())
						banButton.setEnabled(false);
				}
			}
		});

		final ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null)
					if (mBoundService.getCurrentStatus() instanceof PlayerService.PlayingStatus)
						startActivity(new Intent(LastFMPlayer.this,
								ShareTrackActivity.class));
			}
		});

		final ImageButton loveButton = (ImageButton) findViewById(R.id.love_button);
		loveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null) {
					if (mBoundService.loveCurrentTrack())
						loveButton.setEnabled(false);
				}
			}
		});

		ImageSwitcher albumView = (ImageSwitcher) LastFMPlayer.this
				.findViewById(R.id.album_view);
		albumView.setFactory(new ViewSwitcher.ViewFactory() {

			public View makeView() {
				ImageView i = new ImageView(LastFMPlayer.this);
				i.setBackgroundColor(0xFF000000);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				i.setLayoutParams(new ImageSwitcher.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				return i;
			}
		});
		albumView.setInAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));
		albumView.setOutAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out));

		if (savedInstanceState != null) {
			loveButton.setEnabled(savedInstanceState.getBoolean(
					"loveButton_enabled", true));
			banButton.setEnabled(savedInstanceState.getBoolean(
					"banButton_enabled", true));
			shareButton.setEnabled(savedInstanceState.getBoolean(
					"shareButton_enabled", true));
		}
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		stationSpinner.setSelection(settings.getInt("last_station_type", 0));
		stationName.setText(settings.getString("last_station_name", ""));

	}

	protected void shareTrack(XSPFTrackInfo track) {
		startActivityForResult(new Intent(LastFMPlayer.this,
				ShareTrackActivity.class), SHARE_TRACK);
	}

	public void onServiceStarted() {
		if (mBoundService != null) {
			mBoundService
					.setLastFMNotificationListener(new LastFMNotificationListener());
		}
	}

	private Uri getStationUri() {
		final Spinner stationSpinner = (Spinner) findViewById(R.id.station_spinner);
		final EditText stationName = (EditText) findViewById(R.id.station_name);

		Uri.Builder builder = new Uri.Builder();
		builder.scheme("lastfm");
		switch (stationSpinner.getSelectedItemPosition()) {
		case 0:
			builder.authority("artist");
			builder.appendPath(stationName.getText().toString());
			builder.appendPath("similarartists");
			builder.fragment("");
			builder.query("");
			return builder.build();
		case 1:
			builder.authority("globaltags");
			builder.appendPath(stationName.getText().toString());
			builder.fragment("");
			builder.query("");
			return builder.build();
		default:
			Log.e(TAG, "While composing station url: invalid station type");
			return null;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SET_USER_INFO) {
			if (resultCode == RESULT_OK) {

				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				String username = settings.getString("username", null);
				String password = settings.getString("password", null);
				if (username != null && password != null
						&& username.length() != 0 && password.length() != 0) {

					Uri stationUri = getStationUri();
					Intent serviceIntent = new Intent("play", stationUri,
							LastFMPlayer.this, PlayerService.class);
					LastFMPlayer.this.startService(serviceIntent);
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		final ImageButton loveButton = (ImageButton) findViewById(R.id.love_button);
		final ImageButton banButton = (ImageButton) findViewById(R.id.ban_button);
		final ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);

		outState.putBoolean("loveButton_enabled", loveButton.isEnabled());
		outState.putBoolean("banButton_enabled", banButton.isEnabled());
		outState.putBoolean("shareButton_enabled", shareButton.isEnabled());
	}

}