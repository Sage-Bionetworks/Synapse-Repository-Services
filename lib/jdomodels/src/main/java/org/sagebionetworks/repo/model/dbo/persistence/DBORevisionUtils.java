package org.sagebionetworks.repo.model.dbo.persistence;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DBORevisionUtils {
	
	/**
	 * Convert a blob of references to a Reference object. Assuming that the blob
	 * contains a map from a string to a one element set of Reference. 
	 * This method is used to migrate the JDORevision table.
	 * 
	 * @param blob that contains the map of string to a set of references
	 * @return the only reference found in the map
	 * @throws IllegalArgumentException is the map has more than one key or 
	 * 		the set has more than one element.
	 * @throws IOException
	 */
	public static Reference convertBlobToReference(byte[] blob) throws IOException {
		if (blob == null) {
			return null;
		}
		try {
			Map<String, Set<Reference>> map = JDOSecondaryPropertyUtils.decompressedReferences(blob);
			if (map.isEmpty()) {
				return null;
			}
			if (map.size() > 1) {
				throw new IllegalArgumentException();
			}
			String key = map.keySet().iterator().next();
			Set<Reference> set = map.get(key);
			if (set.isEmpty()) {
				return null;
			}
			if (set.size() > 1) {
				throw new IllegalArgumentException();
			}
			return set.iterator().next();
		} catch (ClassCastException e) {
			try {
				// if the blob contains a Reference object, return the object
				return JDOSecondaryPropertyUtils.decompressedReference(blob);
			} catch (ClassCastException e2) {
				throw new IllegalArgumentException();
			}
		}
	}

}
