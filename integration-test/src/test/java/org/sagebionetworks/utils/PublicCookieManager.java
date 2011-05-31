package org.sagebionetworks.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.gdevelop.gwt.syncrpc.CookieManager;

public class PublicCookieManager extends CookieManager {

	// private Map store;
	private Map<String, String> cookieMap;

	private static final String SET_COOKIE_SEPARATOR = "; ";
	private static final String COOKIE = "Cookie";

	private static final char DOT = '.';

	public PublicCookieManager() {
		super();
		cookieMap = new HashMap<String, String>();
	}

	/**
	 * Retrieves and stores cookies returned by the host on the other side of
	 * the the open java.net.URLConnection.
	 * 
	 * The connection MUST have been opened using the connect() method or a
	 * IOException will be thrown.
	 * 
	 * @param conn
	 *            a java.net.URLConnection - must be open, or IOException will
	 *            be thrown
	 * @throws java.io.IOException
	 *             Thrown if conn is not open.
	 */
	public void storeCookies(URLConnection conn) throws IOException {

	}

	public void clearCookies(URLConnection conn) throws IOException {
		// let's determine the domain from where these cookies are being sent
		cookieMap.clear();
	}

	/**
	 * Prior to opening a URLConnection, calling this method will set all
	 * unexpired cookies that match the path or subpaths for thi underlying URL
	 * 
	 * The connection MUST NOT have been opened method or an IOException will be
	 * thrown.
	 * 
	 * @param conn
	 *            a java.net.URLConnection - must NOT be open, or IOException
	 *            will be thrown
	 * @throws java.io.IOException
	 *             Thrown if conn has already been opened.
	 */
	public void setCookies(URLConnection conn) throws IOException {

		// let's determine the domain and path to retrieve the appropriate
		// cookies
		URL url = conn.getURL();
		String domain = getDomainFromHost(url.getHost());
		String path = url.getPath();

		StringBuffer cookieStringBuffer = new StringBuffer();

		Iterator<String> cookieNames = cookieMap.keySet().iterator();
		while (cookieNames.hasNext()) {
			String cookieName = cookieNames.next();
			cookieStringBuffer.append(cookieName);
			cookieStringBuffer.append("=");
			cookieStringBuffer.append((String) cookieMap.get(cookieName));
			if (cookieNames.hasNext())
				cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
		}
		try {
			conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
		} catch (java.lang.IllegalStateException ise) {
			IOException ioe = new IOException(
					"Illegal State! Cookies cannot be set on a URLConnection that is already connected. "
							+ "Only call setCookies(java.net.URLConnection) AFTER calling java.net.URLConnection.connect().");
			throw ioe;
		}
	}

	private String getDomainFromHost(String host) {
		if (host.indexOf(DOT) != host.lastIndexOf(DOT)) {
			return host.substring(host.indexOf(DOT) + 1);
		} else {
			return host;
		}
	}

	/**
	 * Returns a string representation of stored cookies organized by domain.
	 */

	public String toString() {
		return cookieMap.toString();
	}
	
	/**
	 * Put a cookie in the map.
	 * @param key
	 * @param value
	 */
	public void putCookie(String key, String value){
		this.cookieMap.put(key, value);
	}

}
