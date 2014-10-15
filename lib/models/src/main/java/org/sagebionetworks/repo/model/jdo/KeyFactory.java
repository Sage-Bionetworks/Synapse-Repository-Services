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
	
	public static final Long ROOT_ID = new Long(4489);
	public static final String SYN_ROOT_ID = KeyFactory.keyToString(ROOT_ID);

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
		if (key==null) return null;
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
			String decodedId = urlDecode(id);
			return new Long(decodedId);
		} catch (NumberFormatException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * Decodes an application/x-www-form-urlencoded string encoded in UTF-8.
	 *
	 * @return the decoded key trimmed and in lower case
	 * @throws DatastoreException
	 */
	public static String urlDecode(String id) throws DatastoreException {
		try {
			String decodedId = URLDecoder.decode(id, "UTF-8").trim().toLowerCase();
			if (decodedId.startsWith(SYNAPSE_ID_PREFIX)) {
				decodedId = decodedId.substring(SYNAPSE_ID_PREFIX.length());
			}
			return decodedId;
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
	}
}
