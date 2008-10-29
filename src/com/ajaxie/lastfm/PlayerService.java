package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

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

	private static final String HOST = "http://ws.audioscrobbler.com";

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
		stopPlaying();
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
	}

	public class StoppedStatus implements Status {
		public String toString() {
			return "stopped";
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
				mCurrentStatus = new PlayingStatus(mPlayerThread.getCurrentPosition(), mPlayerThread.getCurrentTrack());
		return mCurrentStatus;
	}

	public boolean stopPlaying() {
		if (mPlayerThread == null)
			return true;
		Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_STOP)
				.sendToTarget();
		try {
			mPlayerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		mPlayerThread = null;
		mCurrentStatus = new StoppedStatus();
		return true;
	}

	public boolean startPlaying(String url) {
		try {
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
			
			ScrobblerClient scrobbler = new ScrobblerClient();
			scrobbler.handshake(username, password);
			mPlayerThread = new PlayerThread(scrobbler, session, baseHost + basePath);
			mPlayerThread.start();
			mPlayerThread.mInitLock.lock();			
			try {
				while (!mPlayerThread.mInitialized)
					mPlayerThread.mInitializedCondition.await();
				Message m = Message.obtain(mPlayerThread.mHandler,
						PlayerThread.MESSAGE_ADJUST, url);
				m.sendToTarget();
				return true;
			} catch (InterruptedException e) {
				mPlayerThread.mInitLock.unlock();
				e.printStackTrace();
				setCurrentStatus(new ErrorStatus(
						"Auth failed: player thread intialization interrupted"));
				return false;
			} catch (Exception e) {
				mPlayerThread.mInitLock.unlock();
				e.printStackTrace();
				setCurrentStatus(new ErrorStatus("Auth failed: " + e.toString()));
				return false;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			setCurrentStatus(new ErrorStatus("Auth failed: " + e.toString()));
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			setCurrentStatus(new ErrorStatus("Auth failed: " + e.toString()));
			return false;
		} catch (IOException e) {
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

	public void skipCurrentTrack() {
		if (mPlayerThread != null) {
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_SKIP).sendToTarget();
		}
	}
	
	public void loveCurrentTrack() {
		if (mPlayerThread != null) {
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_LOVE).sendToTarget();
		}
	}

	public void banCurrentTrack() {
		if (mPlayerThread != null) {
			Message.obtain(mPlayerThread.mHandler, PlayerThread.MESSAGE_BAN).sendToTarget();
		}
	}

}