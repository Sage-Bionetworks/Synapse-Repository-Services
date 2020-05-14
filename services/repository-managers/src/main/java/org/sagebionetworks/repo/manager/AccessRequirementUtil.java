package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {

	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});

	public static List<Long> unmetDownloadAccessRequirementIdsForEntity(
			UserInfo userInfo, 
			String entityId,
			List<Long> entityAncestorIds,
			NodeDAO nodeDao, 
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<ACCESS_TYPE> accessTypes = Collections.singletonList(ACCESS_TYPE.DOWNLOAD);
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(KeyFactory.stringToKey(entityId));
		// if the user is the owner of the entity (and the entity is a File), then she automatically 
		// has access to the object and therefore has no unmet access requirements
		Long principalId = userInfo.getId();
		Node node = nodeDao.getNode(entityId);
		if (node.getCreatedByPrincipalId().equals(principalId) && EntityType.file.equals(node.getNodeType())) {
			return EMPTY_LIST;
		}
		// per PLFM-2477, we inherit the restrictions of the node's ancestors
		entityIds.addAll(entityAncestorIds);

		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		
		return accessRequirementDAO.getAllUnmetAccessRequirements(entityIds, RestrictableObjectType.ENTITY, principalIds, accessTypes);
	}

}
