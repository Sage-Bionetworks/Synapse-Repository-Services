package org.sagebionetworks.repo.model.jdo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
	 * Synapse keys sent to the repo svc may be optionally prefixed with this
	 * string. All keys leaving the repo svc should have this prefix.
	 */
	public static final String SYNAPSE_ID_PREFIX = "syn";

	/**
	 * Converts a Long into a websafe string.
	 * 
	 * @param key
	 * @return a web-safe string representation of a key
	 */
	public static String keyToString(Long key) {
		return SYNAPSE_ID_PREFIX + key.toString();
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
			String decodedId = URLDecoder.decode(id, "UTF-8").trim().toLowerCase();
			if (decodedId.startsWith(SYNAPSE_ID_PREFIX)) {
				decodedId = decodedId.substring(SYNAPSE_ID_PREFIX.length());
			}
			return new Long(decodedId);
		} catch (NumberFormatException e) {
			throw new DatastoreException(e);
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
	}
}
