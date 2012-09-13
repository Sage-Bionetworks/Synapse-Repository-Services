package org.sagebionetworks.repo.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * http://stackoverflow.com/questions/221525/how-would-you-implement-an-lru-cache-in-java-6
 * 
 * @author deflaux
 *
 * @param <A>
 * @param <B>
 */
@SuppressWarnings("serial")
public class LruCache<A, B> extends LinkedHashMap<A, B> {
	private final int maxEntries;

	/**
	 * Be sure to use a thread-safe instance of the cache
	 * Map<String, String> example = Collections.synchronizedMap(new LruCache<String, String>(CACHE_SIZE));
	 * 
	 * @param maxEntries
	 */
	public LruCache(final int maxEntries) {
		super(maxEntries + 1, 1.0f, true);
		this.maxEntries = maxEntries;
	}

	/**
	 * Returns <tt>true</tt> if this <code>LruCache</code> has more entries than
	 * the maximum specified when it was created.
	 * 
	 * <p>
	 * This method <em>does not</em> modify the underlying <code>Map</code>; it
	 * relies on the implementation of <code>LinkedHashMap</code> to do that,
	 * but that behavior is documented in the JavaDoc for
	 * <code>LinkedHashMap</code>.
	 * </p>
	 * 
	 * @param eldest
	 *            the <code>Entry</code> in question; this implementation
	 *            doesn't care what it is, since the implementation is only
	 *            dependent on the size of the cache
	 * @return <tt>true</tt> if the oldest
	 * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
	 */
	@Override
	protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
		return super.size() > maxEntries;
	}
}
