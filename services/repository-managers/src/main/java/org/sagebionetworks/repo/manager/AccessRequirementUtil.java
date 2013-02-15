package org.sagebionetworks.repo.manager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {
	
	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});
	public static List<Long> unmetAccessRequirementIds(
			UserInfo userInfo, 
			String nodeId,
			ACCESS_TYPE accessType,
			NodeDAO nodeDAO,
			AccessRequirementDAO accessRequirementDAO) throws NotFoundException {
		{
			// if the user is the owner of the object, then she automatically 
			// has access to the object and therefore has no unmet access requirements
			Long principalId = Long.parseLong(userInfo.getIndividualGroup().getId());
			Node node = nodeDAO.getNode(nodeId);
			if (node.getCreatedByPrincipalId().equals(principalId)) {
				return EMPTY_LIST;
			}
		}

		Set<Long> principalIds = new HashSet<Long>();
		for (UserGroup ug : userInfo.getGroups()) {
			principalIds.add(Long.parseLong(ug.getId()));
		}
		return accessRequirementDAO.unmetAccessRequirements(nodeId, principalIds, accessType);
	}
}
