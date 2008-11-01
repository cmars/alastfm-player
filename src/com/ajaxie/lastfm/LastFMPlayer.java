package com.ajaxie.lastfm;

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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SETTINGS_ID, 0, R.string.menu_settings);
		menu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about);
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
			startActivity(
					new Intent(LastFMPlayer.this, AboutActivity.class)
					);
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

	private ServiceConnection mConnection = new ServiceConnection() {
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
						@Override
						public void run() {
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
							statusText.setText(statusString);

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

	Timer refreshTimer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		refreshTimer = new Timer();
		refreshTimer.scheduleAtFixedRate(new StatusRefreshTask(), 1000, 1000);

		if (!LastFMPlayer.this.bindService(new Intent(LastFMPlayer.this,
				PlayerService.class), mConnection, Context.BIND_AUTO_CREATE))
			LastFMPlayer.this.showDialog(DIALOG_ERROR);
		Intent serviceIntent = new Intent(LastFMPlayer.this,
				PlayerService.class);
		LastFMPlayer.this.startService(serviceIntent);

		final ImageButton playButton = (ImageButton) findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				String username = settings.getString("username", null);
				String password = settings.getString("password", null);
				if ((username == null || password == null)
						|| (username.length() == 0 || password.length() == 0))
					startActivityForResult(new Intent(LastFMPlayer.this,
							UserInfo.class), SET_USER_INFO);
				else {
					final EditText stationText = (EditText) findViewById(R.id.station_url);
					if (mBoundService != null)
						mBoundService.startPlaying(stationText.getText()
								.toString());
				}
			}
		});

		final ImageButton stopButton = (ImageButton) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null) {
					mBoundService.stopPlaying();
					LastFMPlayer.this.unbindService(mConnection);
					Intent serviceIntent = new Intent(LastFMPlayer.this,
							PlayerService.class);
					LastFMPlayer.this.stopService(serviceIntent);
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
				if (mBoundService != null)
					mBoundService.banCurrentTrack();
			}
		});

		final ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(LastFMPlayer.this, ShareTrackActivity.class));
			}
		});

		final ImageButton loveButton = (ImageButton) findViewById(R.id.love_button);
		loveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mBoundService != null)
					mBoundService.loveCurrentTrack();
			}
		});

		ImageSwitcher albumView = (ImageSwitcher) LastFMPlayer.this
				.findViewById(R.id.album_view);
		albumView.setFactory(new ViewSwitcher.ViewFactory() {
			@Override
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
	}

	protected void shareTrack(XSPFTrackInfo track) {
		startActivityForResult(new Intent(LastFMPlayer.this,
				ShareTrackActivity.class), SHARE_TRACK);
	}

	public void onServiceStarted() {
		if (mBoundService != null) {
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
					final EditText stationText = (EditText) findViewById(R.id.station_url);
					if (mBoundService != null)
						mBoundService.startPlaying(stationText.getText()
								.toString());
				}
			}
		}
	}
}