package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityBundleServiceImpl implements EntityBundleService {
	
	@Autowired
	ServiceProvider serviceProvider;

	@Override
	public EntityBundle getEntityBundle(String userId, String entityId, int mask, HttpServletRequest request, 
			Integer offset, Integer limit, String sort, Boolean ascending)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userId, entityId, null, mask, request, offset, limit, sort, ascending);
	}

	@Override
	public EntityBundle getEntityBundle(String userId, String entityId,
			Long versionNumber, int mask, HttpServletRequest request,
			Integer offset, Integer limit, String sort, Boolean ascending)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundle eb = new EntityBundle();
		if ((mask & EntityBundle.ENTITY) > 0) {
			if(versionNumber == null) {
				eb.setEntity(serviceProvider.getEntityService().getEntity(userId, entityId, request));
			} else {
				eb.setEntity(serviceProvider.getEntityService().getEntityForVersion(userId, entityId, versionNumber, request));
			}
		}
		if ((mask & EntityBundle.ANNOTATIONS) > 0) {
			if(versionNumber == null) {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotations(userId, entityId, request));
			} else {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotationsForVersion(userId, entityId, versionNumber, request));				
			}
		}
		if ((mask & EntityBundle.PERMISSIONS) > 0) {
			eb.setPermissions(serviceProvider.getEntityService().getUserEntityPermissions(userId, entityId));
		}
		if ((mask & EntityBundle.ENTITY_PATH) > 0) {
			List<EntityHeader> path = serviceProvider.getEntityService().getEntityPath(userId, entityId);
			EntityPath ep = new EntityPath();
			ep.setPath(path);
			eb.setPath(ep);
		}
		if ((mask & EntityBundle.ENTITY_REFERENCEDBY) > 0) {
			eb.setReferencedBy(serviceProvider.getEntityService().getEntityReferences(userId, entityId, null, null, null, request));
		}
		if ((mask & EntityBundle.CHILD_COUNT) > 0) {
			try {
				eb.setChildCount(serviceProvider.getEntityService().getChildCount(userId, entityId, request));
			} catch (ParseException e) {
				eb.setChildCount(null);
				throw e;
			}
		}
		if ((mask & EntityBundle.ACL) > 0) {
			try {
				eb.setAccessControlList(serviceProvider.getEntityService().getEntityACL(entityId, userId, request));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. Set ACL to null.
				eb.setAccessControlList(null);
			}
		}
		
		// TODO : these do not belong in the entity bundle
		if ((mask & EntityBundle.USERS) > 0) {
			eb.setUsers(serviceProvider.getUserProfileService().getUserProfilesPaginated(request, userId, offset, limit, sort, ascending));
		}
		if ((mask & EntityBundle.GROUPS) > 0) {
			eb.setGroups(serviceProvider.getUserGroupService().getUserGroups(request, userId, offset, limit, sort, ascending));
		}
		return eb;

	}	
	
	@Override
	public EntityBundle createEntityBundle(String userId, EntityBundle eb, int partsMask, HttpServletRequest request) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if ((partsMask & EntityBundle.ENTITY) > 0 && eb.getEntity() != null) {
			Entity entity = serviceProvider.getEntityService().createEntity(userId, eb.getEntity(), request);
			if ((partsMask & EntityBundle.ACL) > 0 && eb.getAccessControlList() != null) {
				AccessControlList acl;				
				try {
					// ACLs are created automatically for entities whose parent is root;
					// update the existing ACL
					acl = serviceProvider.getEntityService().getEntityACL(entity.getId(), userId, request);
					acl.getResourceAccess().addAll(eb.getAccessControlList().getResourceAccess());
					acl = serviceProvider.getEntityService().updateEntityACL(userId, acl, null, request);
				} catch (ACLInheritanceException e) {
					// No ACL exists;
					// create a new ACL
					acl = eb.getAccessControlList();
					acl.setId(entity.getId());
					acl = serviceProvider.getEntityService().createEntityACL(userId, acl, request);
				}				
				eb.setAccessControlList(acl);
			}
			if ((partsMask & EntityBundle.ANNOTATIONS) > 0 && eb.getAnnotations() != null) {
				Annotations annos = serviceProvider.getEntityService().getEntityAnnotations(userId, entity.getId(), request);
				annos.addAll(eb.getAnnotations());
				annos = serviceProvider.getEntityService().updateEntityAnnotations(userId, entity.getId(), annos, request);
				eb.setAnnotations(annos);
			}			
		} else {
			// Bundle did not contain an entity
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}
		
		// TODO: Remove this once users, groups have been removed from bundle
		partsMask &= ~(EntityBundle.GROUPS | EntityBundle.PERMISSIONS);
		return getEntityBundle(userId, eb.getEntity().getId(), partsMask, request, null, null, null, null);
	}

}
