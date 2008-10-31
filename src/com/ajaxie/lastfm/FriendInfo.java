package com.ajaxie.lastfm;

import org.w3c.dom.Element;

import com.ajaxie.lastfm.Utils.ParseException;

public class FriendInfo {
	private static final String TAG = "FriendInfo";
	
	private String mName;
	private String mProfileUrl;
	private String mAvatarUrl;

	public String getName() {
		return mName;
	}
	
	public String getAvatarUrl() {
		return mAvatarUrl;
	}

	public String getProfileUrl() {
		return mAvatarUrl;
	}
	
	// important for AutoCompleteTextViews
	@Override 
	public String toString() {
		if (mName != null)
			return mName;
		else
			return "<null>";
	}

	public FriendInfo(Element element) throws ParseException{
		mName = element.getAttribute("username");
		mProfileUrl = Utils.getChildElement(element, "url");		
		mAvatarUrl = Utils.getChildElement(element, "image");		
}
}
