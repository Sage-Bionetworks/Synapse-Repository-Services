package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ResourceAccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class AclSnapshotUtils {
	private static List<ACCESS_TYPE> accessTypeArray = Lists.newArrayList(ACCESS_TYPE.values());

	/**
	 * Generate a set of ResourceAccess, each with a given principalId and the same numberOfAccessType
	 * @param principalIds
	 * @param numberOfAccessType
	 */
	public static Set<ResourceAccess> createSetOfResourceAccess(List<Long> principalIds,
			int numberOfAccessType) {
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		Set<ACCESS_TYPE> accessTypes = Sets.newHashSet(numberOfAccessType == -1 ? accessTypeArray : accessTypeArray.subList(0,
				numberOfAccessType));
		for (Long principalId : principalIds) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(principalId);
			ra.setAccessType(accessTypes);
			set.add(ra);
		}
		return set;
	}
}
