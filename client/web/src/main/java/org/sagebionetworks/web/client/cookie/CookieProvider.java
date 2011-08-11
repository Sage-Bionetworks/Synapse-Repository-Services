package org.sagebionetworks.web.client.cookie;

import java.util.Collection;
import java.util.Date;

/**
 * A simple layer of abstraction around cookies. This lets us test cookies
 * without depending on the GWT client implementation.
 * 
 * @author jmhill
 * 
 */
public interface CookieProvider {

	public String getCookie(String key);

	public Collection<String> getCookieNames();

	public void removeCookie(String key);

	public void setCookie(String key, String value);

	public void setCookie(String key, String value, Date expires);

	public void setCookie(String name, String value, Date expires,
			String domain, String path, boolean secure);
	
}
