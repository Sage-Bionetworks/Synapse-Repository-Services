package org.sagebionetworks.repo.web.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleCreate;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
	public EntityBundle getEntityBundle(Long userId, String entityId, EntityBundleRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userId, entityId, null, request);
	}

	@Override
	public EntityBundle getEntityBundle(Long userId, String entityId,
										Long versionNumber, EntityBundleRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundle eb = new EntityBundle();
		Entity entity = null;
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(entityId, versionNumber);
		if (isTrue(request.getIncludeEntity()) || isTrue(request.getIncludeFileName())) {
			if(versionNumber == null) {
				entity = serviceProvider.getEntityService().getEntity(userId, entityId);
			} else {
				entity = serviceProvider.getEntityService().getEntityForVersion(userId, entityId, versionNumber);
			}
			if (isTrue(request.getIncludeEntity())) {
				eb.setEntity(entity);
				eb.setEntityType(EntityTypeUtils.getEntityTypeForClass(entity.getClass()));
			}
		}
		if (isTrue(request.getIncludeAnnotations())) {
			if(versionNumber == null) {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotations(userId, entityId));
			} else {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotationsForVersion(userId, entityId, versionNumber));
			}
		}
		if (isTrue(request.getIncludePermissions())) {
			eb.setPermissions(serviceProvider.getEntityService().getUserEntityPermissions(userId, entityId));
		}
		if (isTrue(request.getIncludeEntityPath())) {
			List<EntityHeader> path = serviceProvider.getEntityService().getEntityPath(userId, entityId);
			EntityPath ep = new EntityPath();
			ep.setPath(path);
			eb.setPath(ep);
		}
		if (isTrue(request.getIncludeHasChildren())) {
			eb.setHasChildren(serviceProvider.getEntityService().doesEntityHaveChildren(userId, entityId));
		}
		if (isTrue(request.getIncludeAccessControlList())) {
			try {
				eb.setAccessControlList(serviceProvider.getEntityService().getEntityACL(entityId, userId));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. Set ACL to null.
				eb.setAccessControlList(null);
			}
		}
		if (isTrue(request.getIncludeBenefactorACL())) {
			try {
				// If this entity is its own benefactor then we just get the ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(entityId, userId));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. So get the benefactor's ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(e.getBenefactorId(), userId));
			}
		}
		List<FileHandle> fileHandles = null;
		if (isTrue(request.getIncludeFileHandles()) || isTrue(request.getIncludeFileName())) {
			try {
				if (versionNumber == null) {
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForCurrentVersion(userId, entityId).getList();
				} else{
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForVersion(userId, entityId, versionNumber).getList();
				}
			}catch (NotFoundException | UnauthorizedException e) {
				// If there are no file handle(s) or if the user does not have permission to see the handles then set them to be an empty list.
				fileHandles = new LinkedList<FileHandle>();
			}
			if (isTrue(request.getIncludeFileHandles())) {
				eb.setFileHandles(fileHandles);
			}
		}
		if (isTrue(request.getIncludeTableBundle())) {
			// This mask only has meaning for implementations of tables.
			eb.setTableBundle(serviceProvider.getTableServices().getTableBundle(idAndVersion));
		}
		if(isTrue(request.getIncludeRootWikiId())){
			try {
				WikiPageKey rootKey = serviceProvider.getWikiService().getRootWikiKey(userId, entityId, ObjectType.ENTITY);
				eb.setRootWikiId(rootKey.getWikiPageId());
			} catch (NotFoundException e) {
				// does not exist
				eb.setRootWikiId(null);
			}
		}
		if(isTrue(request.getIncludeDOIAssociation()) ){
			try {
				if (versionNumber == null && (entity instanceof VersionableEntity)) {
					// DOIs on VersionableEntity cannot be versionless, so we want to get the DOI for the current version
					Long currentVersionNumber = ((VersionableEntity) entity).getVersionNumber();
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(entityId, ObjectType.ENTITY, currentVersionNumber));
				} else {
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(entityId, ObjectType.ENTITY, versionNumber));
				}
			} catch (NotFoundException e) {
				// does not exist
				eb.setDoiAssociation(null);
			}
		}
		if(isTrue(request.getIncludeFileName()) && (entity instanceof FileEntity)){
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
		if (isTrue(request.getIncludeThreadCount())) {
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
		if (isTrue(request.getIncludeRestrictionInformation())) {
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
	public EntityBundle createEntityBundle(Long userId, EntityBundleCreate ebc, String activityId) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if (ebc.getEntity() == null) {
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}

		EntityBundleRequest fetchRequest = new EntityBundleRequest();

		// Create the Entity
		fetchRequest.setIncludeEntity(true);
		Entity toCreate = ebc.getEntity();
		Entity entity = serviceProvider.getEntityService().createEntity(userId, toCreate, activityId);

		// Create the ACL
		if (ebc.getAccessControlList() != null) {
			fetchRequest.setIncludeAccessControlList(true);
			AccessControlList acl = ebc.getAccessControlList();
			acl.setId(entity.getId());
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userId, acl);
		}

		// Create the Annotations
		if (ebc.getAnnotations() != null) {
			fetchRequest.setIncludeAnnotations(true);
			Annotations annos =serviceProvider.getEntityService().getEntityAnnotations(userId, entity.getId());
			annos.getAnnotations().putAll(ebc.getAnnotations().getAnnotations());
			serviceProvider.getEntityService().updateEntityAnnotations(userId, entity.getId(), annos);
		}

		return getEntityBundle(userId, entity.getId(), fetchRequest);
	}

	@WriteTransaction
	@Override
	public EntityBundle updateEntityBundle(Long userId, String entityId,
										   EntityBundleCreate ebc, String activityId)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,
			ACLInheritanceException, ParseException {


		Entity entity = ebc.getEntity();
		AccessControlList acl = ebc.getAccessControlList();
		Annotations annos = ebc.getAnnotations();

		EntityBundleRequest fetchRequest = new EntityBundleRequest();

		// Update the Entity
		if (ebc.getEntity() != null) {
			if (!entityId.equals(ebc.getEntity().getId()))
				throw new IllegalArgumentException("Entity does not match requested entity ID");
			fetchRequest.setIncludeEntity(true);
			entity = serviceProvider.getEntityService().updateEntity(userId, entity, false, activityId);
		}

		// Update the ACL
		if (ebc.getAccessControlList() != null) {
			Long entityKey = KeyFactory.stringToKey(entityId);
			Long aclKey = KeyFactory.stringToKey(acl.getId());
			if (!entityKey.equals(aclKey)) {
				throw new IllegalArgumentException("ACL does not match requested entity ID");
			}
			fetchRequest.setIncludeAccessControlList(true);
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userId, acl);
		}

		// Update the Annotations
		if (ebc.getAnnotations() != null) {
			if (!entityId.equals(ebc.getAnnotations().getId()))
				throw new IllegalArgumentException("Annotations do not match requested entity ID");
			fetchRequest.setIncludeAnnotations(true);
			Annotations toUpdate = serviceProvider.getEntityService().getEntityAnnotations(userId, entityId);
			toUpdate.getAnnotations().putAll(annos.getAnnotations());
			serviceProvider.getEntityService().updateEntityAnnotations(userId, entityId, toUpdate);
		}

		return getEntityBundle(userId, entityId, fetchRequest);
	}

}
