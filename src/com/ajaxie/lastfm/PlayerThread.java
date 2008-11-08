package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ajaxie.lastfm.PlayerService.ErrorStatus;
import com.ajaxie.lastfm.PlayerService.LastFMNotificationListener;
import com.ajaxie.lastfm.Utils.OptionsParser;
import com.ajaxie.lastfm.Utils.ParseException;

public class PlayerThread extends Thread {

	public static final int MESSAGE_ADJUST = 0;
	public static final int MESSAGE_STOP = 1;
	public static final int MESSAGE_UPDATE_PLAYLIST = 2;
	public static final int MESSAGE_SKIP = 3;
	public static final int MESSAGE_CACHE_TRACK_INFO = 4;
	public static final int MESSAGE_SCROBBLE_NOW_PLAYING = 5;
	public static final int MESSAGE_SUBMIT_TRACK = 6;
	public static final int MESSAGE_LOVE = 7;
	public static final int MESSAGE_BAN = 8;
	public static final int MESSAGE_SHARE = 9;
	public static final int MESSAGE_CACHE_FRIENDS_LIST = 10;
	public static final int MESSAGE_LOGIN = 11;

	private static final String TAG = "PlayerThread";
	private static final String XMLRPC_URL = "http://ws.audioscrobbler.com/1.0/rw/xmlrpc.php";
	private static final String WS_URL = "http://ws.audioscrobbler.com/1.0";

	public Handler mHandler;
	public ConditionVariable mInitLock = new ConditionVariable();
	private String mSession;
	private String mBaseURL;

	MediaPlayer mp = null;

	private ArrayList<XSPFTrackInfo> mPlaylist;
	private int mNextPlaylistItem;
	private XSPFTrackInfo mCurrentTrack;

	private long mStartPlaybackTime;
	private String mCurrentTrackRating;

	LastFMError mError = null;

	public static class NotEnoughContentError extends LastFMError {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6594205637078329839L;

		public NotEnoughContentError() {
			super("Not enough content for this station");
		}
	}

	public static class LastFMXmlRpcError extends LastFMError {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String faultString = "";

		public LastFMXmlRpcError(String faultString) {
			super("XmlRPC error: " + faultString);
			this.faultString = faultString;
		}

		public String getFaultString() {
			return faultString;
		}
	}

	public static class BadCredentialsError extends LastFMError {
		String faultString = "";
		public static int BAD_USERNAME = 0;
		public static int BAD_PASSWORD = 1;
		private int badItem;

		public BadCredentialsError(String faultString, int badItem) {
			super(faultString);
			this.faultString = faultString;
			this.badItem = badItem;
		}

		public String getFaultString() {
			return faultString;
		}

		public int getBadItem() {
			return badItem;
		}
	}

	public LastFMError getError() {
		return mError;
	}

	private void setErrorState(LastFMError e) {
		mError = e;
	}

	public int getCurrentPosition() {
		if (mp != null)
			return mp.getCurrentPosition();
		else
			return 0;
	}

	public XSPFTrackInfo getCurrentTrack() {
		return mCurrentTrack;
	}
	
	private LastFMNotificationListener mLastFMNotificationListener = null;

	public void setLastFMNotificationListener(
			LastFMNotificationListener listener) {
		this.mLastFMNotificationListener = listener;
	}

	String mUsername;
	String mPassword;
	protected ArrayList<FriendInfo> mFriendsList;

	public PlayerThread(String username, String password) {
		super();
		mUsername = username;
		mPassword = password;
	}

	public void run() {
		Looper.prepare();
		mHandler = new Handler() {

			public void handleMessage(Message msg) {
				try {
					switch (msg.what) {
					case PlayerThread.MESSAGE_LOGIN:
						if (!login(mUsername, mPassword))
							getLooper().quit();
						break;
					case PlayerThread.MESSAGE_STOP:
						stopPlaying();
						getLooper().quit();
						break;
					case PlayerThread.MESSAGE_SUBMIT_TRACK:
						TrackSubmissionParams params = (TrackSubmissionParams) msg.obj;
						mScrobbler.submit(params.mTrack,
								params.mPlaybackStartTime, params.mRating);
						break;
					case PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING:
						XSPFTrackInfo currTrack = getCurrentTrack();
						mScrobbler.nowPlaying(currTrack.getCreator(), currTrack
								.getTitle(), currTrack.getAlbum(), currTrack
								.getDuration());
						break;
					case PlayerThread.MESSAGE_SHARE:
						try {
							TrackShareParams msgParams = (TrackShareParams) msg.obj;
							scrobblerRpcCall("recommendItem", new String[] {
									msgParams.mTrack.getCreator(),
									msgParams.mTrack.getTitle(), "track",
									msgParams.mRecipient, msgParams.mMessage,
									msgParams.mLanguage });
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener
										.onShared(true, null);
						} catch (LastFMXmlRpcError e) {
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener.onShared(false,
										e.faultString);
						}

						break;
					case PlayerThread.MESSAGE_LOVE:
						try {
							setCurrentTrackRating("L");
							XSPFTrackInfo currentTrack = getCurrentTrack();
							scrobblerRpcCall("loveTrack", new String[] {
									currentTrack.getCreator(),
									currentTrack.getTitle() });
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener.onLoved(true, null);
						} catch (LastFMXmlRpcError e) {
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener.onLoved(false,
										e.faultString);
						}
						break;
					case PlayerThread.MESSAGE_BAN:
						try {
							setCurrentTrackRating("B");
							XSPFTrackInfo currentTrack2 = getCurrentTrack();
							scrobblerRpcCall("banTrack", new String[] {
									currentTrack2.getCreator(),
									currentTrack2.getTitle() });
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener
										.onBanned(true, null);
						} catch (LastFMXmlRpcError e) {
							if (mLastFMNotificationListener != null)
								mLastFMNotificationListener.onBanned(false,
										e.faultString);
						}
						playNextTrack();
						break;
					case PlayerThread.MESSAGE_SKIP:
						setCurrentTrackRating("S");
						playNextTrack();
						break;
					case PlayerThread.MESSAGE_CACHE_TRACK_INFO:
						getCurrentTrack().downloadImageBitmap();
						break;
					case PlayerThread.MESSAGE_CACHE_FRIENDS_LIST:
						mFriendsList = downloadFriendsList();
						break;
					case PlayerThread.MESSAGE_UPDATE_PLAYLIST:
						mPlaylist = getPlaylist();
						if (mPlaylist != null) {
							mNextPlaylistItem = 0;
						} else
							throw new LastFMError("Playlist fetch failed");
						break;
					case PlayerThread.MESSAGE_ADJUST:
						if (adjust((String) msg.obj)) {
							mPlaylist = getPlaylist();
							if (mPlaylist != null) {
								mNextPlaylistItem = 0;
								startPlaying();
							} else
								throw new LastFMError("Playlist fetch failed");
						} else
							throw new LastFMError("Failed to tune to a station. Please try again or choose a different station.");
						break;
					}
				} catch (LastFMError e) {
					setErrorState(e);
				}
			}

			private void setCurrentTrackRating(String string) {
				mCurrentTrackRating = string;
			}

		};

		Message.obtain(mHandler, PlayerThread.MESSAGE_LOGIN).sendToTarget();

		mInitLock.open();
		Looper.loop();
	}

	ScrobblerClient mScrobbler;

	public boolean stopPlaying() {
		if (mCurrentTrack != null)
			submitCurrentTrackDelayed();
		if (mp != null)
			mp.stop();
		return true;
	}

	public final ArrayList<FriendInfo> getFriendsList() {
		return mFriendsList;
	}

	private XSPFTrackInfo getNextTrack() {
		if (mPlaylist.size() == 0)
			return null;
		if (mNextPlaylistItem >= mPlaylist.size()) {
			mPlaylist = getPlaylist();
			mNextPlaylistItem = 1;
			return mPlaylist.get(0);
		} else {
			mNextPlaylistItem++;
			if (mNextPlaylistItem == mPlaylist.size() - 1)
				updatePlaylistDelayed();
			return mPlaylist.get(mNextPlaylistItem - 1);
		}
	}

	MediaPlayer.OnCompletionListener mOnTrackCompletionListener = new MediaPlayer.OnCompletionListener() {

		public void onCompletion(MediaPlayer mp) {
			try {
				playNextTrack();
			} catch (LastFMError e) {
				setErrorState(e);
			}
		}
	};

	OnBufferingUpdateListener mOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			if (mLastFMNotificationListener != null)
				mLastFMNotificationListener.onBuffer(percent);
		}
	};

	private static class TrackSubmissionParams {
		public XSPFTrackInfo mTrack;
		public long mPlaybackStartTime;
		public String mRating;

		public TrackSubmissionParams(XSPFTrackInfo track,
				long playbackStartTime, String rating) {
			mTrack = track;
			mPlaybackStartTime = playbackStartTime;
			mRating = rating;
		}
	}

	public static class TrackShareParams {
		public XSPFTrackInfo mTrack;
		public String mRecipient;
		public String mMessage;
		public String mLanguage;

		public TrackShareParams(XSPFTrackInfo track, String recipient,
				String message, String language) {
			mTrack = track;
			mMessage = message;
			mRecipient = recipient;
			mLanguage = language;
		}
	}

	private void submitCurrentTrackDelayed() {
		XSPFTrackInfo curTrack = getCurrentTrack();
		if (curTrack.getDuration() > 30)
			if (mp != null && mp.getCurrentPosition() > 240
					|| mp.getCurrentPosition() > curTrack.getDuration()					
					|| (mCurrentTrackRating != null && mCurrentTrackRating.equals("L"))
					|| (mCurrentTrackRating != null && mCurrentTrackRating.equals("B"))) {
				TrackSubmissionParams params = new TrackSubmissionParams(
						curTrack, mStartPlaybackTime, mCurrentTrackRating);
				Message.obtain(mHandler, PlayerThread.MESSAGE_SUBMIT_TRACK,
						params).sendToTarget();
			}
	}

	private void startPlaying() throws LastFMError {
		playNextTrack();
	}

	private void playNextTrack() throws LastFMError {
		if (mCurrentTrack != null)
			submitCurrentTrackDelayed();
		mCurrentTrack = getNextTrack();
		if (mCurrentTrack == null)
			throw new NotEnoughContentError();

		String streamUrl = mCurrentTrack.getLocation();
		try {
			if (mp != null)
			{
				mp.stop();
				mp = null;
			}
			
			MediaPlayer mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(streamUrl);
			mediaPlayer.setOnCompletionListener(mOnTrackCompletionListener);
			mediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
			mediaPlayer.prepare();
			mediaPlayer.start();
			mp = mediaPlayer;
			mStartPlaybackTime = System.currentTimeMillis() / 1000;
			mCurrentTrackRating = "";
			Message.obtain(mHandler, PlayerThread.MESSAGE_CACHE_TRACK_INFO)
					.sendToTarget();
			Message.obtain(mHandler, PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING)
					.sendToTarget();
			if (mLastFMNotificationListener != null)
				mLastFMNotificationListener.onStartTrack(mCurrentTrack);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "in playNextTrack", e);
			throw new LastFMError(e.toString());
		} catch (IllegalStateException e) {
			Log.e(TAG, "in playNextTrack", e);
			throw new LastFMError(e.toString());
		} catch (IOException e) {
			Log.e(TAG, "in playNextTrack", e);
			playNextTrack();
		}
	}

	private void updatePlaylistDelayed() {
		Message.obtain(mHandler, PlayerThread.MESSAGE_UPDATE_PLAYLIST)
				.sendToTarget();
	}

	private ArrayList<FriendInfo> downloadFriendsList() {
		try {
			URL url;
			url = new URL(WS_URL + "/user/"
					+ URLEncoder.encode(mUsername, "UTF-8") + "/friends.xml");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();

			DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbFac.newDocumentBuilder();
			Document doc = db.parse(is);

			NodeList friends = doc.getElementsByTagName("user");
			ArrayList<FriendInfo> result = new ArrayList<FriendInfo>();
			for (int i = 0; i < friends.getLength(); i++)
				try {
					result.add(new FriendInfo((Element) friends.item(i)));
				} catch (Utils.ParseException e) {
					Log.e(TAG, "in downloadFriendsList", e);
					return null;
				}

			return result;
		} catch (Exception e) {
			Log.e(TAG, "in downloadFriendsList", e);
			return null;
		}
	}

	private ArrayList<XSPFTrackInfo> getPlaylist() {
		try {
			URL url;
			url = new URL("http://" + mBaseURL + "/xspf.php?sk=" + mSession
					+ "&discovery=0&desktop=1.4.1.57486");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();

			DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbFac.newDocumentBuilder();
			Document doc = db.parse(is);

			Element root = doc.getDocumentElement();
			NodeList titleNs = root.getElementsByTagName("title");
			String stationName = "<unknown station>"; 
			if (titleNs.getLength() > 0)
			{
				Element titleElement = (Element)titleNs.item(0);
				String res = "";
				for (int i = 0; i < titleElement.getChildNodes().getLength(); i++)
				{
					Node item = titleElement.getChildNodes().item(i);
					if (item.getNodeType() == Node.TEXT_NODE)
						res += item.getNodeValue(); 
				}
				stationName = URLDecoder.decode(res, "UTF-8");
			}
			NodeList tracks = doc.getElementsByTagName("track");
			ArrayList<XSPFTrackInfo> result = new ArrayList<XSPFTrackInfo>();
			for (int i = 0; i < tracks.getLength(); i++)
				try {
					result.add(new XSPFTrackInfo(stationName, (Element) tracks.item(i)));
				} catch (Utils.ParseException e) {
					Log.e(TAG, "in getPlaylist", e);
					return null;
				}

			return result;
		} catch (Exception e) {
			Log.e(TAG, "in getPlaylist", e);
			return null;
		}
	}

	
	private boolean adjust(String stationUrl) throws LastFMError {
		try {
			URL url = new URL("http://" + mBaseURL + "/adjust.php?session="
					+ mSession + "&url="
					+ URLEncoder.encode(stationUrl, "UTF-8"));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			InputStreamReader reader = new InputStreamReader(is);
			BufferedReader stringReader = new BufferedReader(reader);
			Utils.OptionsParser options = new Utils.OptionsParser(stringReader);
			if (!options.parse())
				options = null;
			stringReader.close();
			if ("OK".equals(options.get("response")))
			{
				return true;
			}
			else {
				Log
						.e(TAG, "Adjust failed: \"" + options.get("response")
								+ "\"");
				return false;
			}
		} catch (MalformedURLException e) {
			Log.e(TAG, "in adjust", e);
			throw new LastFMError("Adjust failed:" + e.toString());
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "in adjust", e);
			throw new LastFMError("Adjust failed:" + e.toString());
		} catch (IOException e) {
			Log.e(TAG, "in adjust", e);
			throw new LastFMError("Station not found:" + stationUrl);
		}
	}

	void scrobblerRpcCall(String method, String[] params) throws LastFMError {
		String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		String auth = Utils.md5String(Utils.md5String(mPassword) + timestamp);

		String[] authParams = new String[3 + params.length];

		authParams[0] = mUsername;
		authParams[1] = timestamp;
		authParams[2] = auth;
		for (int i = 0; i < params.length; i++)
			authParams[i + 3] = params[i];

		xmlRpcCall(method, authParams);
	}

	private static final String HOST = "http://ws.audioscrobbler.com";

	boolean checkIfUserExists(String username) 
		throws IOException {
		try {
			URL url = new URL(WS_URL + "/user/" + URLEncoder.encode(username, "UTF-8") + "/profile.xml");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			
			InputStream is = conn.getInputStream();
			is.close();
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}
	
	boolean login(String username, String password) {
		try {
			Utils.OptionsParser opts;
			opts = handshake(username, password);
			String session = opts.get("session");
			String baseHost = opts.get("base_url");
			String basePath = opts.get("base_path");
			if (session == null || session.equals("FAILED") || baseHost == null
					|| basePath == null) {
				String message = opts.get("msg");
				if (message == null)
					message = "";
				if (message.equals("no such user"))
				{
					if (!checkIfUserExists(username))
						setErrorState(new BadCredentialsError(message, BadCredentialsError.BAD_USERNAME));
					else
						setErrorState(new BadCredentialsError(message, BadCredentialsError.BAD_PASSWORD));
				}
				else
					setErrorState(new LastFMError("Auth failed: " + message));
				return false;
			}
			mSession = session;
			mBaseURL = baseHost + basePath;
			
			mScrobbler = new ScrobblerClient();
			mScrobbler.handshake(username, password);			
			return true;
		} catch (IOException e) {
			setErrorState(new LastFMError("Login failed: please check your internet connection"));
			return false;
		}
	}

	Utils.OptionsParser handshake(String Username, String Pass)
			throws IOException {
		String passMD5 = Utils.md5String(Pass);
		URL url = new URL(
				HOST
						+ "/radio/handshake.php?version=1.0.0.0&platform=windows&username="
						+ URLEncoder.encode(Username, "UTF_8") + "&passwordmd5=" + passMD5);
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

	static void xmlRpcCall(String method, String[] params) throws LastFMError {
		try {
			XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
			XmlSerializer serializer = fac.newSerializer();
			URL url;
			url = new URL(XMLRPC_URL);

			URLConnection conn;
			conn = url.openConnection();
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setDoOutput(true);

			serializer.setOutput(conn.getOutputStream(), "UTF-8");
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "methodCall");
			serializer.startTag(null, "methodName");
			serializer.text(method);
			serializer.endTag(null, "methodName");
			serializer.startTag(null, "params");
			for (String s : params) {
				serializer.startTag(null, "param");
				serializer.startTag(null, "value");
				serializer.startTag(null, "string");
				serializer.text(s);
				serializer.endTag(null, "string");
				serializer.endTag(null, "value");
				serializer.endTag(null, "param");
			}
			serializer.endTag(null, "params");
			serializer.endTag(null, "methodCall");
			serializer.flush();

			InputStream is = conn.getInputStream();

			DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbFac.newDocumentBuilder();
			Document doc = db.parse(is);

			try {
				String res = Utils.getChildElement(doc.getDocumentElement(),
						new String[] { "params", "param", "value", "string" });
				if (!res.equals("OK")) {
					Log.e(TAG, "while xmlrpc got " + res);
					throw new LastFMXmlRpcError("XMLRPC Call failed: " + res);
				}
			} catch (ParseException e) {
				String faultString = Utils.getChildElement(doc
						.getDocumentElement(), new String[] { "params",
						"param", "value", "struct", "member[1]", "value",
						"string" });
				throw new LastFMXmlRpcError(faultString);
			}
		} catch (LastFMXmlRpcError e) {
			throw e;
		} catch (Exception e) {
			Log.e(TAG, "while xmlrpc", e);
			throw new LastFMError(e.toString());
		}
	}
}
