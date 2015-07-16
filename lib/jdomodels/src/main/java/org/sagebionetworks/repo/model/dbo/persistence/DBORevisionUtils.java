package org.sagebionetworks.repo.model.dbo.persistence;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DBORevisionUtils {
	public static Reference convertMapToReference(byte[] blob) throws IOException {
		Map<String, Set<Reference>> map;
		try {
			map = JDOSecondaryPropertyUtils.decompressedReferences(blob);
		} catch (ClassCastException e) {
			try {
				return JDOSecondaryPropertyUtils.decompressedReference(blob);
			} catch (ClassCastException e2) {
				return null;
			}
		}
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
	}

}
