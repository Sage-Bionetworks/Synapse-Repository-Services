package org.sagebionetworks.repo.web.service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityBundleServiceImpl implements EntityBundleService {
	
	@Autowired
	ServiceProvider serviceProvider;

	@Autowired
	UserManager userManager;

	@Autowired
	AccessRequirementManager accessRequirementManager;
	
	public EntityBundleServiceImpl() {}
	
	/**
	 * Direct constructor (for testing purposes)
	 * 
	 * @param serviceProvider
	 */
	public EntityBundleServiceImpl(ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
	}
	
	@Override
	public EntityBundle getEntityBundle(Long userId, String entityId, int mask, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userId, entityId, null, mask, request);
	}

	@Override
	public EntityBundle getEntityBundle(Long userId, String entityId,
			Long versionNumber, int mask, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundle eb = new EntityBundle();
		Entity entity = null;
		if ((mask & (EntityBundle.ENTITY | EntityBundle.FILE_NAME)) > 0) {
			if(versionNumber == null) {
				entity = serviceProvider.getEntityService().getEntity(userId, entityId);
			} else {
				entity = serviceProvider.getEntityService().getEntityForVersion(userId, entityId, versionNumber);
			}
			if ((mask & EntityBundle.ENTITY) > 0) {
				eb.setEntity(entity);
			}
		}
		if ((mask & EntityBundle.ANNOTATIONS) > 0) {
			if(versionNumber == null) {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotations(userId, entityId));
			} else {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotationsForVersion(userId, entityId, versionNumber));
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
			throw new IllegalArgumentException("ENTITY_REFERENCEDBY is deprecated.");
		}
		if ((mask & EntityBundle.HAS_CHILDREN) > 0) {
			eb.setHasChildren(serviceProvider.getEntityService().doesEntityHaveChildren(userId, entityId));
		}
		if ((mask & EntityBundle.ACL) > 0) {
			try {
				eb.setAccessControlList(serviceProvider.getEntityService().getEntityACL(entityId, userId));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. Set ACL to null.
				eb.setAccessControlList(null);
			}
		}
		if ((mask & EntityBundle.BENEFACTOR_ACL) > 0) {
			try {
				// If this entity is its own benefactor then we just get the ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(entityId, userId));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. So get the benefactor's ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(e.getBenefactorId(), userId));
			}
		}
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		if ((mask & EntityBundle.ACCESS_REQUIREMENTS) > 0) {
			// This is deprecated.
			eb.setAccessRequirements(new LinkedList<AccessRequirement>());
		}
		if ((mask & EntityBundle.UNMET_ACCESS_REQUIREMENTS) > 0) {
			eb.setUnmetAccessRequirements(accessRequirementManager.getAllUnmetAccessRequirements(
					userManager.getUserInfo(userId), subjectId, ACCESS_TYPE.DOWNLOAD));
		}
		List<FileHandle> fileHandles = null;
		if ((mask & (EntityBundle.FILE_HANDLES | EntityBundle.FILE_NAME)) > 0 ) {
			try {
				if (versionNumber == null) {
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForCurrentVersion(userId, entityId).getList();
				} else{
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForVersion(userId, entityId, versionNumber).getList();
				} 
			}catch (Exception e) {
				// If the user does not have permission to see the handles then set them to be an empty list.
				fileHandles = new LinkedList<FileHandle>();
			}
			if ((mask & EntityBundle.FILE_HANDLES)>0) {
				eb.setFileHandles(fileHandles);
			}
		}
		if((mask & EntityBundle.TABLE_DATA) > 0 ){
			PaginatedColumnModels paginated = serviceProvider.getTableServices().getColumnModelsForTableEntity(userId, entityId);
			TableBundle tableBundle = new TableBundle();
			tableBundle.setColumnModels(paginated.getResults());
			tableBundle.setMaxRowsPerPage(serviceProvider.getTableServices().getMaxRowsPerPage(paginated.getResults()));
			eb.setTableBundle(tableBundle);
		}
		if((mask & EntityBundle.ROOT_WIKI_ID) > 0 ){
			 try {
				WikiPageKey rootKey = serviceProvider.getWikiService().getRootWikiKey(userId, entityId, ObjectType.ENTITY);
				eb.setRootWikiId(rootKey.getWikiPageId());
			} catch (NotFoundException e) {
				// does not exist
				eb.setRootWikiId(null);
			}
		}
		if((mask & EntityBundle.DOI) > 0 ){
			try {
				if (versionNumber == null && (entity instanceof VersionableEntity)) {
					// DOIs on VersionableEntity cannot be versionless, so we want to get the DOI for the current version
					Long currentVersionNumber = ((VersionableEntity) entity).getVersionNumber();
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(userId, entityId, ObjectType.ENTITY, currentVersionNumber));
				} else {
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(userId, entityId, ObjectType.ENTITY, versionNumber));
				}
			} catch (NotFoundException e) {
				// does not exist
				eb.setDoiAssociation(null);
			}
		}
		if((mask & EntityBundle.FILE_NAME) > 0 && (entity instanceof FileEntity)){
			FileEntity fileEntity = (FileEntity)entity;
			if (fileEntity.getFileNameOverride()==null) {
				for (FileHandle fileHandle : fileHandles) {
					if (fileHandle.getId().equals(fileEntity.getDataFileHandleId())) {
						eb.setFileName(fileHandle.getFileName());
						break;
					}
				}
			} else {
				eb.setFileName(fileEntity.getFileNameOverride());
			}
		}
		if ((mask & EntityBundle.THREAD_COUNT) > 0) {
			EntityIdList entityIdList = new EntityIdList();
			entityIdList.setIdList(Arrays.asList(entityId));
			EntityThreadCounts result = serviceProvider.getDiscussionService().getThreadCounts(userId, entityIdList );
			if (result.getList().isEmpty()) {
				eb.setThreadCount(0L);
			} else if (result.getList().size() == 1) {
				eb.setThreadCount(result.getList().get(0).getCount());
			} else {
				throw new IllegalStateException("Unexpected EntityThreadCount list size: "+result.getList().size());
			}
		}
		if ((mask & EntityBundle.RESTRICTION_INFORMATION) > 0) {
			RestrictionInformationRequest restrictionInfoRequest = new RestrictionInformationRequest();
			restrictionInfoRequest.setObjectId(entityId);
			restrictionInfoRequest.setRestrictableObjectType(RestrictableObjectType.ENTITY);
			RestrictionInformationResponse restrictionInfo = serviceProvider.getDataAccessService().getRestrictionInformation(userId, restrictionInfoRequest);
			eb.setRestrictionInformation(restrictionInfo);
		}
		return eb;
	}

	@WriteTransaction
	@Override
	public EntityBundle createEntityBundle(Long userId, EntityBundleCreate ebc, String activityId, HttpServletRequest request) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if (ebc.getEntity() == null) {
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}
		
		int partsMask = 0;
		
		// Create the Entity
		partsMask += EntityBundle.ENTITY;
		Entity toCreate = ebc.getEntity();
		Entity entity = serviceProvider.getEntityService().createEntity(userId, toCreate, activityId);
		
		// Create the ACL
		if (ebc.getAccessControlList() != null) {
			partsMask += EntityBundle.ACL;
			AccessControlList acl = ebc.getAccessControlList();
			acl.setId(entity.getId());
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userId, acl, null);
		}
		
		// Create the Annotations
		if (ebc.getAnnotations() != null) {
			partsMask += EntityBundle.ANNOTATIONS;
			Annotations annos = serviceProvider.getEntityService().getEntityAnnotations(userId, entity.getId());
			annos.addAll(ebc.getAnnotations());
			annos = serviceProvider.getEntityService().updateEntityAnnotations(userId, entity.getId(), annos);
		}
		
		return getEntityBundle(userId, entity.getId(), partsMask, request);
	}
	
	@WriteTransaction
	@Override
	public EntityBundle updateEntityBundle(Long userId, String entityId,
			EntityBundleCreate ebc, String activityId, HttpServletRequest request)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,
			ACLInheritanceException, ParseException {
		
		int partsMask = 0;
		
		Entity entity = ebc.getEntity();
		AccessControlList acl = ebc.getAccessControlList();
		Annotations annos = ebc.getAnnotations();
		
		// Update the Entity
		if (ebc.getEntity() != null) {
			if (!entityId.equals(ebc.getEntity().getId()))
				throw new IllegalArgumentException("Entity does not match requested entity ID");
			partsMask += EntityBundle.ENTITY;
			entity = serviceProvider.getEntityService().updateEntity(userId, entity, false, activityId);
		}
			
		// Update the ACL
		if (ebc.getAccessControlList() != null) {
			Long entityKey = KeyFactory.stringToKey(entityId);
			Long aclKey = KeyFactory.stringToKey(acl.getId());
			if (!entityKey.equals(aclKey)) {
				throw new IllegalArgumentException("ACL does not match requested entity ID");
			}
			partsMask += EntityBundle.ACL;
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userId, acl, null);
		}
		
		// Update the Annotations
		if (ebc.getAnnotations() != null) {
			if (!entityId.equals(ebc.getAnnotations().getId()))
				throw new IllegalArgumentException("Annotations do not match requested entity ID");
			partsMask += EntityBundle.ANNOTATIONS;
			Annotations toUpdate = serviceProvider.getEntityService().getEntityAnnotations(userId, entityId);
			toUpdate.addAll(annos);
			annos = serviceProvider.getEntityService().updateEntityAnnotations(userId, entityId, toUpdate);
		}
		
		return getEntityBundle(userId, entityId, partsMask, request);
	}
	
}
