package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {
	
	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});
	public static List<Long> unmetAccessRequirementIds(
			UserInfo userInfo, 
			RestrictableObjectDescriptor subjectId,
			NodeDAO nodeDAO,
			AccessRequirementDAO accessRequirementDAO) throws NotFoundException {
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		if (RestrictableObjectType.ENTITY.equals(subjectId.getType())) {
			accessTypes.add(ACCESS_TYPE.DOWNLOAD);
			// if the user is the owner of the object, then she automatically 
			// has access to the object and therefore has no unmet access requirements
			Long principalId = Long.parseLong(userInfo.getIndividualGroup().getId());
			Node node = nodeDAO.getNode(subjectId.getId());
			if (node.getCreatedByPrincipalId().equals(principalId)) {
				return EMPTY_LIST;
			}
		} else if (RestrictableObjectType.EVALUATION.equals(subjectId.getType())) {
			accessTypes.add(ACCESS_TYPE.DOWNLOAD);
			accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		} else {
			throw new IllegalArgumentException("Unexpected type: "+subjectId.getType());
		}

		Set<Long> principalIds = new HashSet<Long>();
		for (UserGroup ug : userInfo.getGroups()) {
			principalIds.add(Long.parseLong(ug.getId()));
		}
		
		return accessRequirementDAO.unmetAccessRequirements(subjectId, principalIds, accessTypes);
	}
}
