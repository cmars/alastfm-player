package com.ajaxie.lastfm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class XSPFTrackInfo {
	private static final String TAG = "XSPFTrackInfo";

	private String mLocation;
	private String mAlbum;
	private String mCreator;
	private int mDuration;
	private String mImage;
	private String mTitle;
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getLocation() {
		return mLocation;
	}
	
	public String getAlbum() {
		return mAlbum;
	}
	
	public String getCreator() {
		return mCreator;
	}
	
	public int getDuration() {
		return mDuration;
	}
	
	public String getImage() {
		return mImage;
	}
	
    Bitmap mBitmap = null;
    boolean mBitmapLoadFailed = false;
	public Bitmap downloadImageBitmap() { 
		if (mBitmap == null && !mBitmapLoadFailed) {
	        try { 
	            URL aURL = new URL(mImage); 
	            URLConnection conn = aURL.openConnection(); 
	            conn.connect(); 
	            InputStream is = conn.getInputStream(); 
	            BufferedInputStream bis = new BufferedInputStream(is); 
	            mBitmap = BitmapFactory.decodeStream(bis); 
	            bis.close(); 
	            is.close(); 
	       } catch (IOException e) { 
	           Log.e(TAG, "Error getting bitmap", e);
	           mBitmapLoadFailed = true;
	       }
	   }
       return mBitmap; 
    }
	
	public Bitmap getBitmap() {
		return mBitmap;
	}
	
	private String mAuth;

	public XSPFTrackInfo(Element element) throws Utils.ParseException{
			mLocation = Utils.getChildElement(element, "location");		
			mAlbum = Utils.getChildElement(element, "album");		
			mCreator = Utils.getChildElement(element, "creator");
			try {
				mDuration = Integer.parseInt(Utils.getChildElement(element, "duration"));
			} catch (NumberFormatException ex) {
				throw new Utils.ParseException(element, "duration");
			}
			mImage =  Utils.getChildElement(element, "image");
			mTitle =  Utils.getChildElement(element, "title");
			mAuth =  Utils.getChildElement(element, "lastfm:trackauth");
	}

	public String getAuth() {
		return mAuth;
	}
}
