package org.sagebionetworks.repo.model.jdo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;

/**
 * This particular implementation of KeyFactory expects string keys from the
 * model layer and numeric keys in the persistence layer. It is similar to
 * com.google.appengine.api.datastore.KeyFactory.
 * 
 * @author deflaux
 * 
 */
public class KeyFactory {
	
	public static final String IS_NOT_A_VALID_SYNAPSE_ID_SUFFIX = " is not a valid Synapse ID.";
	public static final Long ROOT_ID = NodeConstants.BOOTSTRAP_NODES.ROOT.getId();
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
			throw new IllegalArgumentException(id+IS_NOT_A_VALID_SYNAPSE_ID_SUFFIX);
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
	public static List<Long> stringToKey(Collection<String> ids) throws DatastoreException {
		ValidateArgument.required(ids, "ids");
		List<Long> resutls = new LinkedList<Long>();
		for(String key: ids){
			resutls.add(stringToKey(key));
		}
		return resutls;
	}
	
	/**
	 * Given a single string return singleton long list.
	 * @param id
	 * @return
	 * @throws DatastoreException
	 */
	public static List<Long> stringToKeySingletonList(String id) throws DatastoreException {
		return Collections.singletonList(stringToKey(id));
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
	
	/**
	 * Are the two Entity Ids equal?
	 * 
	 * @param idOne
	 * @param idTwo
	 * @return
	 */
	public static boolean equals(String idOne, String idTwo){
		if(idOne == null){
			return idTwo == null;
		}
		if(idTwo == null){
			return false;
		}
		// ignore syn for the final equals
		return KeyFactory.stringToKey(idOne).equals(KeyFactory.stringToKey(idTwo));
	}
	
	/**
	 * Compare two keys.  Handles null comparisons.
	 * 
	 * @param one
	 * @param two
	 * @return
	 */
	public static int compare(String one, String two) {
		if(one == null) {
			return two == null ? 0 : -1;
		}
		return two == null ? 1 : KeyFactory.stringToKey(one).compareTo(KeyFactory.stringToKey(two));
	}

	/**
	 * Create an IdAndVersion for the given entity id and optional version.
	 * @param entityId
	 * @param versionNumber
	 * @return
	 */
	public static IdAndVersion idAndVersion(String entityId, Long versionNumber) {
		ValidateArgument.required(entityId, "entityId");
		return IdAndVersion.newBuilder().setId(stringToKey(entityId)).setVersion(versionNumber).build();
	}
}
