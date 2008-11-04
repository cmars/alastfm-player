package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class PlayerService extends Service {
	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		PlayerService getService() {
			return PlayerService.this;
		}

		public boolean startPlaying(String stationUrl) {
			return PlayerService.this
					.startPlaying(stationUrl);
		}
	}
	
	public static class LastFMNotificationListener {
		public void onStartTrack(XSPFTrackInfo track) {}
		public void onBuffer(int percent) {}
		public void onLoved(boolean success) {}
		public void onBanned(boolean success) {}
		public void onShared(boolean success) {}
	}
		
	private static final String HOST = "http://ws.audioscrobbler.com";

	private static final String TAG = "PlayerService";

	private final IBinder mBinder = new LocalBinder();

	private NotificationManager mNM;

	private LastFMNotificationListener mLastFMNotificationListener = null;

	public void setLastFMNotificationListener(LastFMNotificationListener listener) {
		this.mLastFMNotificationListener = listener;
	}

	@Override
	public IBinder onBind(Intent intent) {
//		handleIntent(intent);
		return mBinder;
	}

    private static int PLAYER_NOTIFICATIONS = 1;

	@Override
	public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);		
	}

	@Override
	public void onDestroy() {
		stopPlaying();
        mNM.cancel(PLAYER_NOTIFICATIONS);
	}
	
	private void updateNotification(String text) {
        Notification notification = new Notification(R.drawable.play, null, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LastFMPlayer.class), 0);
        
        notification.setLatestEventInfo(this, "LastFM Player",
                text, contentIntent);
        
        mNM.notify(PLAYER_NOTIFICATIONS, notification);
        
	}
	
	boolean mCurrentTrackLoved = false;
	boolean mCurrentTrackBanned = false;
	
	public boolean isCurrentTrackLoved() {
		return mCurrentTrackLoved;
	}
	
	public boolean isCurrentTrackBanned() {
		return mCurrentTrackBanned;
	}
	
	public class ServiceNotificationListener extends LastFMNotificationListener {

		LastFMNotificationListener mUserListener;
		public ServiceNotificationListener(LastFMNotificationListener userListener) {
			mUserListener = userListener;
		}
		
		@Override
		public void onStartTrack(XSPFTrackInfo track) {
			updateNotification(track.getTitle() + " by " + track.getCreator());
			mCurrentTrackLoved = false;
			mCurrentTrackBanned = false;
			mCurrentStatus = new PlayingStatus(0, track);
			if (mUserListener != null)
				mUserListener.onStartTrack(track);
		}
		
		@Override
		public void onBanned(boolean success) {
			if (mUserListener != null)
				mUserListener.onBanned(success);
		}

		@Override
		public void onLoved(boolean success) {
			if (mUserListener != null)
				mUserListener.onLoved(success);
		}

		@Override
		public void onShared(boolean success) {
			if (mUserListener != null)
				mUserListener.onShared(success);
		}		
	}

	public interface Status {
		public String toString();
	}

	public class PlayingStatus implements Status {
		int position;
		XSPFTrackInfo trackInfo;
		
		public PlayingStatus(int currentPosition, XSPFTrackInfo currentTrack) {
			this.position = currentPosition;
			this.trackInfo = currentTrack;
		}

		public String toString() {
			return "playing";
		}

		public int getCurrentPosition() {
			return position;
		}

		public XSPFTrackInfo getCurrentTrack() {
			return trackInfo;
		}

		public void setCurrentPosition(int currentPosition) {
			position = currentPosition;
		}
	}

	public class StoppedStatus implements Status {
		public String toString() {
			return "stopped";
		}
	}

	public class ConnectingStatus implements Status {
		public String toString() {
			return "connecting..";
		}
	}

	public class ErrorStatus implements Status {
		String mMessage = "";

		public ErrorStatus(String message) {
			mMessage = message;
		}

		public String toString() {
			if (mMessage == null)
				return "Unknown error";
			else
				return "Error: " + mMessage;
		}
	}

	Status mCurrentStatus;

	private PlayerThread mPlayerThread;

	public void setCurrentStatus(Status status) {
		mCurrentStatus = status;
	}

	public Status getCurrentStatus() {
		if (mPlayerThread != null)
			if (mPlayerThread.getError() != null)
				mCurrentStatus = new ErrorStatus(mPlayerThread.getError()
						.toString());
			else
			{
				Status curStatus = mCurrentStatus;
				if (curStatus instanceof PlayingStatus)
					((PlayingStatus)curStatus).setCurrentPosition(mPlayerThread.getCurrentPosition());
			}
		return mCurrentStatus;
	}

	public boolean stopPlaying() {
		if (mPlayerThread == null)
			return true;
		Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_STOP)
				.sendToTarget();
		try {
			mPlayerThread.join(10000);
			if (mPlayerThread.isAlive())
				mPlayerThread.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		mPlayerThread = null;
		mCurrentStatus = new StoppedStatus();
		return true;
	}	
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		handleIntent(intent);
	}
	
	void handleIntent(Intent intent) {
		if (intent.getAction() == null)
			return;
		if (intent.getAction().equals(Intent.ACTION_VIEW))
		{			
			startPlaying(intent.getDataString());
		} else
			Log.e(TAG, "Invalid service intent action: " + intent.getAction());		
	}

	public boolean startPlaying(String url) {
		try {
			if (mPlayerThread != null)
				stopPlaying();

			SharedPreferences settings = getSharedPreferences(
					LastFMPlayer.PREFS_NAME, 0);
			String username = settings.getString("username", null);
			String password = settings.getString("password", null);
			Utils.OptionsParser opts = handshake(username, password);
			String session = opts.get("session");
			String baseHost = opts.get("base_url");
			String basePath = opts.get("base_path");
			if (session == null || session.equals("FAILED") || baseHost == null
					|| basePath == null) {
				String message = opts.get("msg");
				if (message == null)
					message = "";
				setCurrentStatus(new ErrorStatus("Auth failed: " + message));
				return false;
			}
			
			mPlayerThread = new PlayerThread(username, password, session, baseHost + basePath);
			mPlayerThread.setLastFMNotificationListener(new ServiceNotificationListener(mLastFMNotificationListener));
			mPlayerThread.start();				
				mPlayerThread.mInitLock.block();
				Message m = Message.obtain(mPlayerThread.mHandler,
						PlayerThread.MESSAGE_ADJUST, url);
				m.sendToTarget();

				Message.obtain(mPlayerThread.mHandler,
						PlayerThread.MESSAGE_CACHE_FRIENDS_LIST).sendToTarget();
				updateNotification("Starting playback");
				mCurrentStatus = new ConnectingStatus();
				return true;			
		} catch (Exception e) {
			e.printStackTrace();
			setCurrentStatus(new ErrorStatus("Auth failed: " + e.toString()));
			return false;
		} 
	}

	Utils.OptionsParser handshake(String Username, String Pass) throws IOException {
			String passMD5 = Utils.md5String(Pass);
			URL url = new URL(
					HOST
							+ "/radio/handshake.php?version=1.0.0.0&platform=windows&username="
							+ Username + "&passwordmd5=" + passMD5);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			InputStreamReader reader = new InputStreamReader(is);
			BufferedReader stringReader = new BufferedReader(reader);
			Utils.OptionsParser options = new Utils.OptionsParser(stringReader);
			if (!options.parse())
				options = null;
			stringReader.close();
			return options;
	}

	public boolean skipCurrentTrack() {
		if (mPlayerThread != null) {
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_SKIP).sendToTarget();
			return true;
		} else
			return false;
	}
	
	public boolean loveCurrentTrack() {
		if (mPlayerThread != null) {
			mCurrentTrackLoved = true;			
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_LOVE).sendToTarget();
			return true;
		} else
			return false;
	}
	
	public final ArrayList<FriendInfo> getFriendsList() {
		if (mPlayerThread != null)
			return mPlayerThread.getFriendsList();
		else 
			return null;
	}	

	public boolean shareTrack(XSPFTrackInfo track, String recipient, String message) {
		if (mPlayerThread != null) {
			PlayerThread.TrackShareParams msgParams = new PlayerThread.TrackShareParams(track, recipient, message, "en");
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_SHARE, msgParams).sendToTarget();
			return true;
		} else
			return false;
	}

	public boolean banCurrentTrack() {
		if (mPlayerThread != null) {
			mCurrentTrackBanned = true;
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_BAN).sendToTarget();
			return true;
		} else
			return false;
	}

}
