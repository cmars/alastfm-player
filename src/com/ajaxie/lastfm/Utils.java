package com.ajaxie.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class Utils {
	static class OptionsParser {
		BufferedReader mReader;
		Map<String, String> mMap = new HashMap<String, String>();
	
		public OptionsParser(BufferedReader reader) {
			mReader = reader;
		}
	
		public String get(String key) {
			return mMap.get(key);
		}
	
		public boolean parse() {
			try {
				for (String s = mReader.readLine(); s != null; s = mReader
						.readLine()) {
					int idx = s.indexOf('=');
					String name = s.substring(0, idx);
					String value = s.substring(idx + 1);
					mMap.put(name, value);
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static String md5String(String str) {
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte[] hash = md.digest();
			final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7',
					'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

			StringBuffer res = new StringBuffer();
			for (int i = 0; i < hash.length; i++) {
				res.append(hexChars[(0xF0 & hash[i]) >> 4]);
				res.append(hexChars[0x0F & hash[i]]);
			}
			return res.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}		
	}

	public static class ParseException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3651212766122533595L;
		/**
		 * 
		 */

		Element mElement;
		String mName;
		
		public ParseException(Element element, String name) {
			mElement = element;
			mName = name;
		}
		
		public String toString() {
			return "Error parsing " + mName + " at " + mElement.toString();
		}
	}
	
	static public String getChildElement(final Element element, final String childName) throws ParseException {
		NodeList nodes = element.getElementsByTagName(childName);
		if (nodes.getLength() != 1)
		{
			Log.i("getChildElement", element.toString());
			throw new ParseException(element, childName);
		}
		if (nodes.item(0).getNodeType() != Node.ELEMENT_NODE)
		{
			Log.i("getChildElement", element.toString());
			throw new ParseException(element, childName);
		}
		
		Element el = (Element)nodes.item(0);
		String res = "";
		for (int i = 0; i < el.getChildNodes().getLength(); i++)
		{
			if (el.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE)
				res = res + el.getChildNodes().item(i).getNodeValue();
		}
		return res;
	}

}
