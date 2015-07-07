package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ResourceAccess;

public class AclSnapshotUtils {
	private static List<ACCESS_TYPE> accessTypeArray =
			new ArrayList<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ,
			ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.CHANGE_PERMISSIONS,
			ACCESS_TYPE.DELETE_SUBMISSION, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.PARTICIPATE,
			ACCESS_TYPE.SUBMIT, ACCESS_TYPE.UPDATE, ACCESS_TYPE.UPLOAD,
			ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SEND_MESSAGE,
			ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, ACCESS_TYPE.UPDATE_SUBMISSION));

	/**
	 * Generate a set of ResourceAccess, each with a given principalId and the same numberOfAccessType
	 * @param principalIds
	 * @param numberOfAccessType
	 */
	public static Set<ResourceAccess> createSetOfResourceAccess(List<Long> principalIds,
			int numberOfAccessType) {
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		Set<ACCESS_TYPE> accessTypes;
		accessTypes = new HashSet<ACCESS_TYPE>(accessTypeArray.subList(0, numberOfAccessType));
		for (Long principalId : principalIds) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(principalId);
			ra.setAccessType(accessTypes);
			set.add(ra);
		}
		return set;
	}
}
