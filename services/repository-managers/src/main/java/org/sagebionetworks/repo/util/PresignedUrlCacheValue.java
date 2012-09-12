package org.sagebionetworks.repo.util;

import org.joda.time.DateTime;

/**
 * Note that the cache value must also include the expires time for this url so
 * that cache consumers can decide whether there is enough time left on the url
 * to make it useful
 * 
 * @author deflaux
 * 
 */
public class PresignedUrlCacheValue {
	private final String url;
	private final DateTime expires;

	/**
	 * @param url
	 * @param expires
	 */
	public PresignedUrlCacheValue(String url, DateTime expires) {
		super();
		this.url = url;
		this.expires = expires;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the expires
	 */
	public DateTime getExpires() {
		return expires;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PresignedUrlCacheValue [expires=" + expires + ", url=" + url
				+ "]";
	}

}
