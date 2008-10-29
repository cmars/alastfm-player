package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ajaxie.lastfm.Utils.OptionsParser;

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

	public Handler mHandler;
	public Lock mInitLock = new ReentrantLock();
	public boolean mInitialized = false;
	public Condition mInitializedCondition = mInitLock.newCondition();
	private String mSession;
	private String mBaseURL;

	MediaPlayer mp = null;
	
    private ArrayList<XSPFTrackInfo> mPlaylist;
	private int mNextPlaylistItem;
	private XSPFTrackInfo mCurrentTrack;
	
	private long mStartPlaybackTime;
	private String mCurrentTrackRating;

	LastFMError mError = null;
	
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
		
	public PlayerThread(ScrobblerClient scrobbler, String session, String baseURL) {
		super();
		mSession = session;
		mBaseURL = baseURL;
		mScrobbler = scrobbler;
	}
	
	public void run()                       
    {
		Looper.prepare();
		mHandler = new Handler() {

			public void handleMessage(Message msg) {
				try {
                switch (msg.what){ 
                case PlayerThread.MESSAGE_STOP:
                	stopPlaying();
                	getLooper().quit();
                	break;
                case PlayerThread.MESSAGE_SUBMIT_TRACK:
                	TrackSubmissionParams params = (TrackSubmissionParams)msg.obj;
                	mScrobbler.submit(params.mTrack, params.mPlaybackStartTime, params.mRating);
                case PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING:
                	XSPFTrackInfo currTrack = getCurrentTrack();
                	mScrobbler.nowPlaying(currTrack.getCreator(), currTrack.getTitle(), currTrack.getAlbum(), currTrack.getDuration());
                	break;
                case PlayerThread.MESSAGE_LOVE:
                	setCurrentTrackRating("L");
                	break;
                case PlayerThread.MESSAGE_BAN:
                	setCurrentTrackRating("B");
                	playNextTrack();
                	break;
                case PlayerThread.MESSAGE_SKIP:
                	setCurrentTrackRating("S");
                	playNextTrack();
                	break;
                case PlayerThread.MESSAGE_CACHE_TRACK_INFO:
                	getCurrentTrack().downloadImageBitmap();
                	break;
                case PlayerThread.MESSAGE_UPDATE_PLAYLIST:
            		mPlaylist = getPlaylist();
            		if (mPlaylist != null)
            		{
            			mNextPlaylistItem = 0;
            		} else 
            			throw new LastFMError("Playlist fetch failed");
                	break;
                case PlayerThread.MESSAGE_ADJUST:
                	if (adjust((String)msg.obj)) {
                		mPlaylist = getPlaylist();
                		if (mPlaylist != null)
                		{
                			mNextPlaylistItem = 0;
                			startPlaying();
                		} else 
                			throw new LastFMError("Playlist fetch failed");
                	} else 
            			throw new LastFMError("Adjust call failed");
                	break;
                } }
				catch (LastFMError e) {
					setErrorState(e);
				}
            }

			private void setCurrentTrackRating(String string) {
				mCurrentTrackRating = string;
			}

        };

        mInitialized = true;
        mInitLock.lock();
        mInitializedCondition.signalAll();
        mInitLock.unlock();
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
	
	private XSPFTrackInfo getNextTrack() {
		if (mNextPlaylistItem >= mPlaylist.size())
		{
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
		@Override
		public void onCompletion(MediaPlayer mp) {
			try {
				playNextTrack();
			} catch (LastFMError e) {
				setErrorState(e);
			}
		}		
	};

	private static class TrackSubmissionParams {
		public XSPFTrackInfo mTrack;
		public long mPlaybackStartTime;
		public String mRating;
		
		public TrackSubmissionParams(XSPFTrackInfo track, long playbackStartTime, String rating) {
			mTrack = track;
			mPlaybackStartTime = playbackStartTime;
			mRating = rating;
		}
	}
	
	private void submitCurrentTrackDelayed() {
		XSPFTrackInfo curTrack = getCurrentTrack();
		if (curTrack.getDuration() > 30)			
			if (mp.getCurrentPosition() > 240 || mp.getCurrentPosition() > curTrack.getDuration() || mCurrentTrackRating.equals("L") || mCurrentTrackRating.equals("B"))				
			{
				TrackSubmissionParams params = new TrackSubmissionParams(curTrack, mStartPlaybackTime, mCurrentTrackRating);
				Message.obtain(mHandler, PlayerThread.MESSAGE_SUBMIT_TRACK, params).sendToTarget();					
			}			
	}

	private void startPlaying() throws LastFMError {
		playNextTrack();
	}
	
	private void playNextTrack() throws LastFMError {
		if (mCurrentTrack != null)
			submitCurrentTrackDelayed();
		mCurrentTrack = getNextTrack();
		String streamUrl = mCurrentTrack.getLocation();
		try {
			mp = new MediaPlayer();
			mp.setDataSource(streamUrl);
			mp.setOnCompletionListener(mOnTrackCompletionListener);
			mp.prepare();
			mp.start();
			mStartPlaybackTime = System.currentTimeMillis() / 1000;		
			mCurrentTrackRating = "";
			Message.obtain(mHandler, PlayerThread.MESSAGE_CACHE_TRACK_INFO).sendToTarget();			
			Message.obtain(mHandler, PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING).sendToTarget();			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new LastFMError(e.toString());
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new LastFMError(e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			playNextTrack();
		}
	}
	
	private void updatePlaylistDelayed() {
		Message.obtain(mHandler, PlayerThread.MESSAGE_UPDATE_PLAYLIST).sendToTarget();
	}
	
	private ArrayList<XSPFTrackInfo> getPlaylist() {
		try {
			URL url;
			url = new URL("http://" + mBaseURL + "/xspf.php?sk=" + mSession + "&discovery=0&desktop=1.4.1.57486");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			
			DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbFac.newDocumentBuilder();
			Document doc = db.parse(is);
			
			NodeList tracks = doc.getElementsByTagName("track");
			ArrayList<XSPFTrackInfo> result = new ArrayList<XSPFTrackInfo>();
			for (int i = 0; i < tracks.getLength(); i++)
				try {
					result.add(new XSPFTrackInfo((Element)tracks.item(i)));
				} catch (XSPFParseException e) {
					e.printStackTrace();
					return null;
				}
			
			return result;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return null;
			} catch (SAXException e) {
				e.printStackTrace();
				return null;
			} 
	}

	private boolean adjust(String stationUrl) throws LastFMError {
		try {
		URL url;
		url = new URL("http://" + mBaseURL + "/adjust.php?session=" + mSession + "&url=" + URLEncoder.encode(stationUrl, "UTF-8"));
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.connect();
		InputStream is = conn.getInputStream();
		InputStreamReader reader = new InputStreamReader (is); 
		BufferedReader stringReader = new BufferedReader (reader);
		Utils.OptionsParser options = new Utils.OptionsParser(stringReader);
		if (!options.parse())
			options = null;
		stringReader.close();
		if ("OK".equals(options.get("response")))					
			return true;
		else
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new LastFMError("Adjust failed:" + e.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new LastFMError("Adjust failed:" + e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw new LastFMError("Station not found:" + stationUrl);
		} 
	}
	
}