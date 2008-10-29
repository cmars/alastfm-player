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

import com.ajaxie.lastfm.Utils.OptionsParser;

public class ScrobblerClient {

	static final String CLIENT_ID = "wmp";
	static final String CLIENT_VER = "1.0";
	private static final String TAG = "ScrobblerClient";
	private String mSessionId;
	private String mNowPlayingUrl;
	private String mSubmissionUrl;
	
	boolean handshake(String username, String password){
		String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		String auth = Utils.md5String(Utils.md5String(password) + timestamp);
		BufferedReader stringReader = null;
		try {					
			String req = "http://post.audioscrobbler.com/?hs=true&p=1.2&" +
				"c=" + CLIENT_ID + "&v=" + CLIENT_VER + "&u=" + URLEncoder.encode(username, "UTF-8") + 
				"&t=" + timestamp + "&a=" + auth;
			URL url = new URL(req);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			InputStreamReader reader = new InputStreamReader(is);
			stringReader = new BufferedReader(reader);
			String res = stringReader.readLine();
			if (res.equals("OK"))
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
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			if (stringReader != null)
				try {
					stringReader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
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
			return false;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
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
			if (res.equals("OK"))
				return true;
			else
			{
				Log.e(TAG, "Now playing returned " + res);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();			
		} finally {
	        try {
				wr.close();			
				if (rd != null)
					rd.close();
	        } catch (IOException e) {
				e.printStackTrace();
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
			"&b[0]=" + URLEncoder.encode(album, "UTF-8");
			url = new URL(mSubmissionUrl);
		} catch (UnsupportedEncodingException e1) {
			return false;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
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
			if (res.equals("OK"))
				return true;
			else
			{
				Log.e(TAG, "Now playing returned " + res);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();			
		} finally {
	        try {
				wr.close();			
				if (rd != null)
					rd.close();
	        } catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}