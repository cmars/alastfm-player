package com.ajaxie.lastfm;

import org.w3c.dom.Element;

public class XSPFParseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2812870114881359524L;

	Element mElement;
	String mName;
	
	public XSPFParseException(Element element, String name) {
		mElement = element;
		mName = name;
	}
	
	public String toString() {
		return "Error parsing " + mName + " at " + mElement.toString();
	}
}
