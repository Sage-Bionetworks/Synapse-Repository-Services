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
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EntityBundleServiceImpl implements EntityBundleService {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	@Override
	public void setServiceProvider(ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

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
		if ((mask & EntityBundle.HAS_CHILDREN) > 0) {
			eb.setHasChildren(serviceProvider.getEntityService().doesEntityHaveChildren(userId, entityId, request));
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
		
		if ((mask & EntityBundle.ACCESS_REQUIREMENTS) > 0) {
			eb.setAccessRequirements(serviceProvider.getAccessRequirementService().getAccessRequirements(userId, entityId, request).getResults());
		}
		
		if ((mask & EntityBundle.UNMET_ACCESS_REQUIREMENTS) > 0) {
			eb.setUnmetAccessRequirements(serviceProvider.getAccessRequirementService().getUnfulfilledAccessRequirements(userId, entityId, request).getResults());
		}
		return eb;

	}	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public EntityBundle createEntityBundle(String userId, EntityBundleCreate ebc, HttpServletRequest request) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if (ebc.getEntity() == null) {
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}
		
		int partsMask = 0;
		
		// Create the Entity
		partsMask += EntityBundle.ENTITY;
		Entity toCreate = ebc.getEntity();
		Entity entity = serviceProvider.getEntityService().createEntity(userId, toCreate, request);
		
		// Create the ACL
		if (ebc.getAccessControlList() != null) {
			partsMask += EntityBundle.ACL;
			AccessControlList acl = ebc.getAccessControlList();
			acl.setId(entity.getId());
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userId, acl, null, request);
			ebc.setAccessControlList(acl);
		}
		
		// Create the Annotations
		if (ebc.getAnnotations() != null) {
			partsMask += EntityBundle.ANNOTATIONS;
			Annotations annos = serviceProvider.getEntityService().getEntityAnnotations(userId, entity.getId(), request);
			annos.addAll(ebc.getAnnotations());
			annos = serviceProvider.getEntityService().updateEntityAnnotations(userId, entity.getId(), annos, request);
			ebc.setAnnotations(annos);
		}
		
		return getEntityBundle(userId, entity.getId(), partsMask, request, null, null, null, null);
	}
	


}
