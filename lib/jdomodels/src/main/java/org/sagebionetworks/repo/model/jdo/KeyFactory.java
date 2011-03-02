package org.sagebionetworks.repo.model.jdo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.sagebionetworks.repo.model.DatastoreException;

/**
 * This particular implementation of KeyFactory expects string keys from the
 * model layer and numeric keys in the persistence layer. It is similar to
 * com.google.appengine.api.datastore.KeyFactory.
 * 
 * @author deflaux
 * 
 */
public class KeyFactory {

	/**
	 * Converts a Long into a websafe string.
	 * 
	 * @param key
	 * @return a web-safe string representation of a key
	 * @throws DatastoreException
	 */
	public static String keyToString(Long key) throws DatastoreException {
		try {
			return URLEncoder.encode(key.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * Converts a String-representation of a Long into the Long instance it
	 * represents.
	 * 
	 * @param id
	 * @return the decoded key
	 * @throws DatastoreException
	 */
	public static Long stringToKey(String id) throws DatastoreException {
		try {
			return new Long(URLDecoder.decode(id, "UTF-8"));
		} catch (NumberFormatException e) {
			throw new DatastoreException(e);
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
	}
}
