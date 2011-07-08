package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.util.Log;

public class ScrobblerClient {

	static final String CLIENT_ID = "ala";
	private static final String TAG = "ScrobblerClient";
	private String mSessionId;
	private String mNowPlayingUrl;
	private String mSubmissionUrl;
	private String mClientVersion = "1.0";
	
	void setClientVersionString(String ver) {
		mClientVersion = ver;
	}
	
	boolean handshake(String username, String password){
		String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		String auth = Utils.md5String(Utils.md5String(password) + timestamp);
		BufferedReader stringReader = null;
		try {					
			String req = "http://post.audioscrobbler.com/?hs=true&p=1.2&" +
				"c=" + CLIENT_ID + "&v=" + URLEncoder.encode(mClientVersion, "UTF-8") + "&u=" + URLEncoder.encode(username, "UTF-8") + 
				"&t=" + timestamp + "&a=" + auth;
			URL url = new URL(req);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			InputStreamReader reader = new InputStreamReader(is);
			stringReader = new BufferedReader(reader);
			String res = stringReader.readLine();
			if (res != null && res.equals("OK"))
			{
				mSessionId = stringReader.readLine();
				mNowPlayingUrl = stringReader.readLine();
				mSubmissionUrl = stringReader.readLine();
				stringReader.close();
				return true;
			}
			stringReader.close();
			Log.e(TAG, "Handshake failed: " + res);
			return false;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "in scrobbler handshake", e);
			return false;
		} catch (IOException e) {
			Log.e(TAG, "in scrobbler handshake", e);
			if (stringReader != null)
				try {
					stringReader.close();
				} catch (IOException e1) {
					Log.e(TAG, "in scrobbler handshake", e1);
				}
			return false;
		}
	}
	
	boolean nowPlaying(String artist, String track, String album, int len) {
		String req;
		URL url;
		try {
			req = "s=" + mSessionId + "&a=" + URLEncoder.encode(artist, "UTF-8") +
			"&t=" + URLEncoder.encode(track, "UTF-8") + "&b=" + URLEncoder.encode(album, "UTF-8") +
			"&l=" + Integer.toString(len) + "&n=&m=";
			url = new URL(mNowPlayingUrl);
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "while scrobbling 'now playing'", e1);
			return false;
		} catch (MalformedURLException e1) {
			Log.e(TAG, "while scrobbling 'now playing'", e1);
			return false;
		}
		
		URLConnection conn;
		OutputStreamWriter wr = null;
        BufferedReader rd = null;
		try {
			conn = url.openConnection();
	        conn.setDoOutput(true);
	        wr = new OutputStreamWriter(conn.getOutputStream());
	        wr.write(req);
	        wr.flush();
		
        String res;
        	rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			res = rd.readLine();
			if (res != null && res.equals("OK"))
				return true;
			else
			{
				if (res == null)
					Log.e(TAG, "Now playing returned null" );
				else
					Log.e(TAG, "Now playing returned " + res);
				return false;
			}
		} catch (IOException e) {
			Log.e(TAG, "while scrobbling 'now playing'", e);
		} finally {
	        try {
				wr.close();			
				if (rd != null)
					rd.close();
	        } catch (IOException e) {
				Log.e(TAG, "while scrobbling 'now playing'", e);
			}
		}
		return false;
	}

	boolean submit(XSPFTrackInfo trackInfo, long startTime, String rating) {
		return submit(trackInfo.getCreator(), trackInfo.getTitle(), trackInfo.getAlbum(), trackInfo.getDuration(), startTime, trackInfo.getAuth(), rating);
	}
	
	boolean submit(String artist, String track, String album, int len, long startTime, String auth, String rating) {
		String req;
		URL url;
		try {
			req = "s=" + mSessionId + 
			"&a[0]=" + URLEncoder.encode(artist, "UTF-8") +
			"&t[0]=" + URLEncoder.encode(track, "UTF-8") + 
			"&i[0]=" + Long.toString(startTime) + 
			"&o[0]=L" + auth +
			"&r[0]=" + rating +
			"&l[0]=" + Integer.toString(len) + 
			"&b[0]=" + URLEncoder.encode(album, "UTF-8") +
			"&n[0]=&m[0]=";
			url = new URL(mSubmissionUrl);
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "while scrobbling", e1);
			return false;
		} catch (MalformedURLException e1) {
			Log.e(TAG, "while scrobbling", e1);
			return false;
		}
		
		URLConnection conn;
		OutputStreamWriter wr = null;
        BufferedReader rd = null;
		try {
			conn = url.openConnection();
	        conn.setDoOutput(true);
	        wr = new OutputStreamWriter(conn.getOutputStream());
	        wr.write(req);
	        wr.flush();
		
        String res;
        	rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			res = rd.readLine();
			if (res != null && res.equals("OK"))
				return true;
			else
			{
				Log.e(TAG, "Now playing returned " + res);
				return false;
			}
		} catch (IOException e) {
			Log.e(TAG, "while scrobbling", e);
		} finally {
	        try {
				wr.close();			
				if (rd != null)
					rd.close();
	        } catch (IOException e) {
				Log.e(TAG, "while scrobbling", e);
			}
		}
		return false;
	}
}
