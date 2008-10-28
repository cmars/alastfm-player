package com.ajaxie.lastfm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class XSPFTrackInfo {
	private static final String TAG = "XSPFTrackInfo";

	private static String getChildElement(final Element element, final String childName) throws XSPFParseException {
		NodeList nodes = element.getElementsByTagName(childName);
		if (nodes.getLength() != 1)
			throw new XSPFParseException(element, childName);
		if (nodes.item(0).getNodeType() != Node.ELEMENT_NODE)
			throw new XSPFParseException(element, childName);
		
		Element el = (Element)nodes.item(0);
		String res = "";
		for (int i = 0; i < el.getChildNodes().getLength(); i++)
		{
			if (el.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE)
				res = res + el.getChildNodes().item(i).getNodeValue();
		}
		return res;
	}

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

	public XSPFTrackInfo(Element element) throws XSPFParseException{
			mLocation = getChildElement(element, "location");		
			mAlbum = getChildElement(element, "album");		
			mCreator = getChildElement(element, "creator");
			try {
				mDuration = Integer.parseInt(getChildElement(element, "duration"));
			} catch (NumberFormatException ex) {
				throw new XSPFParseException(element, "duration");
			}
			mImage =  getChildElement(element, "image");
			mTitle =  getChildElement(element, "title");
			mAuth =  getChildElement(element, "lastfm:trackauth");
	}

	public String getAuth() {
		return mAuth;
	}
}
