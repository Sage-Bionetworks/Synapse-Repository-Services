package org.sagebionetworks.web.unitclient.cookie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.sagebionetworks.web.client.cookie.CookieProvider;

/**
 * A cookie provider that is backed by a map for use in tests.
 * 
 * @author jmhill
 *
 */
public class StubCookieProvider implements CookieProvider {
	
	private TreeMap<String, String> map = new TreeMap<String, String>();

	@Override
	public String getCookie(String key) {
		return map.get(key);
	}

	@Override
	public Collection<String> getCookieNames() {
		List<String> results = new ArrayList<String>();
		Set<String> keySet = map.keySet();
		Iterator<String> it = keySet.iterator();
		while(it.hasNext()){
			String key = it.next();
			results.add(key);
		}
		return results;
	}

	@Override
	public void removeCookie(String key) {
		map.remove(key);
	}

	@Override
	public void setCookie(String key, String value) {
		map.put(key, value);
	}

	@Override
	public void setCookie(String key, String value, Date expires) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void setCookie(String name, String value, Date expires,
			String domain, String path, boolean secure) {
		throw new UnsupportedOperationException("Not implemented");
	}

}
