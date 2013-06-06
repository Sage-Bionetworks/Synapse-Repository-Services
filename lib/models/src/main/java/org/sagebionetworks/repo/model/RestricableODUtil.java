package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class RestricableODUtil {
	
	public static Map<RestrictableObjectType, Collection<String>> sortByType(List<RestrictableObjectDescriptor> subjectIds) {
		Map<RestrictableObjectType, Collection<String>> ans = new HashMap<RestrictableObjectType, Collection<String>>();
		for (RestrictableObjectDescriptor rod : subjectIds) {
			Collection<String> rods = ans.get(rod.getType());
			if (rods==null) {
				rods = new HashSet<String>();
				ans.put(rod.getType(), rods);
			}
			rods.add(rod.getId());
		}
		return ans;
	}
}
