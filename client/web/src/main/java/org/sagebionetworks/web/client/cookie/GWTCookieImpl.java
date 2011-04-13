package org.sagebionetworks.web.client.cookie;

import java.util.Collection;
import java.util.Date;

import com.google.gwt.user.client.Cookies;

/**
 * A simple wrapper of the GWT cookie implementation.
 * 
 * @author jmhill
 *
 */
public class GWTCookieImpl implements CookieProvider {

	@Override
	public String getCookie(String name) {
		return Cookies.getCookie(name);
	}

	@Override
	public Collection<String> getCookieNames() {
		return Cookies.getCookieNames();
	}

	@Override
	public void removeCookie(String key) {
		Cookies.removeCookie(key);
	}

	@Override
	public void setCookie(String name, String value) {
		Cookies.setCookie(name, value);

	}

	@Override
	public void setCookie(String name, String value, Date expires) {
		Cookies.setCookie(name, value, expires);
	}

	@Override
	public void setCookie(String name, String value, Date expires,
			String domain, String path, boolean secure) {
		Cookies.setCookie(name, value, expires, domain, path, secure);
	}

}
