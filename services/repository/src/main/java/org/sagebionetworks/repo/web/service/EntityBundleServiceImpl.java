package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityBundleServiceImpl implements EntityBundleService {
	
	@Autowired
	EntityService entityService;
	
	@Autowired
	UserProfileService userProfileService;
	
	@Autowired
	UserGroupService userGroupService;

	@Override
	public EntityBundle getEntityBundle(String userId, String entityId, int mask, HttpServletRequest request, 
			Integer offset, Integer limit, String sort, Boolean ascending)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		EntityBundle eb = new EntityBundle();
		if ((mask & EntityBundle.ENTITY) > 0)
			eb.setEntity(entityService.getEntity(userId, entityId, request));
		if ((mask & EntityBundle.ANNOTATIONS) > 0)
			eb.setAnnotations(entityService.getEntityAnnotations(userId, entityId, request));
		if ((mask & EntityBundle.PERMISSIONS) > 0)
			eb.setPermissions(entityService.getUserEntityPermissions(userId, entityId));
		if ((mask & EntityBundle.ENTITY_PATH) > 0) {
			List<EntityHeader> path = entityService.getEntityPath(userId, entityId);
			EntityPath ep = new EntityPath();
			ep.setPath(path);
			eb.setPath(ep);
		}
		if ((mask & EntityBundle.ENTITY_REFERENCEDBY) > 0)
			eb.setReferencedBy(entityService.getEntityReferences(userId, entityId, null, null, null, request));
		if ((mask & EntityBundle.CHILD_COUNT) > 0) {
			try {
				eb.setChildCount(entityService.getChildCount(userId, entityId, request));
			} catch (ParseException e) {
				eb.setChildCount(null);
				throw e;
			}
		}
		if ((mask & EntityBundle.ACL) > 0)			
			eb.setAccessControlList(entityService.getEntityACL(entityId, userId, request));			
		if ((mask & EntityBundle.USERS) > 0) {
			//TODO: Make new request?
			eb.setUsers(userProfileService.getUserProfilesPaginated(request, userId, offset, limit, sort, ascending));
		}
		if ((mask & EntityBundle.GROUPS) > 0) {
			//TODO: Make new request?
			eb.setGroups(userGroupService.getUserGroups(request, userId, offset, limit, sort, ascending));
		}
		return eb;
	}	

}
