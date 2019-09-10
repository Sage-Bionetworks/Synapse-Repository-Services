package org.sagebionetworks.repo.web.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
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
import org.sagebionetworks.repo.model.EntityBundleV2;
import org.sagebionetworks.repo.model.EntityBundleV2Request;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	public EntityBundleV2 getEntityBundle(Long userId, String entityId, EntityBundleV2Request request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userId, entityId, null, request);
	}

	@Override
	public EntityBundleV2 getEntityBundle(Long userId, String entityId,
										  Long versionNumber, EntityBundleV2Request request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundleV2 eb = new EntityBundleV2();
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
			}catch (Exception e) {
				// If the user does not have permission to see the handles then set them to be an empty list.
				fileHandles = new LinkedList<FileHandle>();
			}
			if (isTrue(request.getIncludeFileHandles())) {
				eb.setFileHandles(fileHandles);
			}
		}
		if (isTrue(request.getIncludeTableEntityMetadata())) {
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
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(userId, entityId, ObjectType.ENTITY, currentVersionNumber));
				} else {
					eb.setDoiAssociation(serviceProvider.getDoiServiceV2().getDoiAssociation(userId, entityId, ObjectType.ENTITY, versionNumber));
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

	@Deprecated
	@Override
	public EntityBundle getEntityBundle(Long userId, String entityId, int mask)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userId, entityId, null, mask);
	}

	@Deprecated
	@Override
	public EntityBundle getEntityBundle(Long userId, String entityId,
			Long versionNumber, int mask)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundle eb = translateEntityBundle(getEntityBundle(userId,entityId,versionNumber, requestFromMask(mask)));
		if ((mask & EntityBundle.ENTITY_REFERENCEDBY) > 0) {
			throw new IllegalArgumentException("ENTITY_REFERENCEDBY is deprecated.");
		}
		if ((mask & EntityBundle.ACCESS_REQUIREMENTS) > 0) {
			// This is deprecated.
			eb.setAccessRequirements(new LinkedList<AccessRequirement>());
		}
		if ((mask & EntityBundle.UNMET_ACCESS_REQUIREMENTS) > 0) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setId(entityId);
			subjectId.setType(RestrictableObjectType.ENTITY);
			eb.setUnmetAccessRequirements(accessRequirementManager.getAllUnmetAccessRequirements(
					userManager.getUserInfo(userId), subjectId, ACCESS_TYPE.DOWNLOAD));
		}
		return eb;
	}

	@WriteTransaction
	@Override
	@Deprecated
	public EntityBundle createEntityBundle(Long userId, EntityBundleCreate ebc, String activityId) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
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
			Annotations annos = AnnotationsV2Translator.toAnnotationsV1(serviceProvider.getEntityService().getEntityAnnotations(userId, entity.getId()));
			annos.addAll(ebc.getAnnotations());
			ebc.setAnnotations(AnnotationsV2Translator.toAnnotationsV1(serviceProvider.getEntityService().updateEntityAnnotations(userId, entity.getId(), AnnotationsV2Translator.toAnnotationsV2(annos))));
		}
		
		return getEntityBundle(userId, entity.getId(), partsMask);
	}

	@WriteTransaction
	@Override
	@Deprecated
	public EntityBundle updateEntityBundle(Long userId, String entityId,
			EntityBundleCreate ebc, String activityId)
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
			Annotations toUpdate = AnnotationsV2Translator.toAnnotationsV1(serviceProvider.getEntityService().getEntityAnnotations(userId, entityId));
			toUpdate.addAll(annos);
			ebc.setAnnotations(AnnotationsV2Translator.toAnnotationsV1(serviceProvider.getEntityService().updateEntityAnnotations(userId, entityId, AnnotationsV2Translator.toAnnotationsV2(toUpdate))));
		}
		
		return getEntityBundle(userId, entityId, partsMask);
	}

	@Deprecated
	static EntityBundle translateEntityBundle(EntityBundleV2 v2Bundle){
		EntityBundle entityBundle = new EntityBundle();
		entityBundle.setEntity(v2Bundle.getEntity());
		entityBundle.setAnnotations(AnnotationsV2Translator.toAnnotationsV1(v2Bundle.getAnnotations()));
		entityBundle.setPermissions(v2Bundle.getPermissions());
		entityBundle.setPath(v2Bundle.getPath());
		entityBundle.setHasChildren(v2Bundle.getHasChildren());
		entityBundle.setAccessControlList(v2Bundle.getAccessControlList());
		entityBundle.setFileHandles(v2Bundle.getFileHandles());
		entityBundle.setTableBundle(v2Bundle.getTableBundle());
		entityBundle.setRootWikiId(v2Bundle.getRootWikiId());
		entityBundle.setBenefactorAcl(v2Bundle.getBenefactorAcl());
		entityBundle.setDoiAssociation(v2Bundle.getDoiAssociation());
		entityBundle.setFileName(v2Bundle.getFileName());
		entityBundle.setThreadCount(v2Bundle.getThreadCount());
		entityBundle.setRestrictionInformation(v2Bundle.getRestrictionInformation());
		return entityBundle;
	}

	@Deprecated
	static EntityBundleV2Request requestFromMask(int mask){
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeEntity((mask | EntityBundle.ENTITY) > 0);
		request.setIncludeAnnotations((mask | EntityBundle.ANNOTATIONS) > 0);
		request.setIncludePermissions((mask | EntityBundle.PERMISSIONS) > 0);
		request.setIncludeEntityPath((mask | EntityBundle.ENTITY_PATH) > 0);
		request.setIncludeHasChildren((mask | EntityBundle.HAS_CHILDREN) > 0);
		request.setIncludeAccessControlList((mask | EntityBundle.ACL) > 0);
		request.setIncludeFileHandles((mask | EntityBundle.FILE_HANDLES) > 0);
		request.setIncludeTableBundle((mask | EntityBundle.TABLE_DATA) > 0);
		request.setIncludeRootWikiId((mask | EntityBundle.ROOT_WIKI_ID) > 0);
		request.setIncludeBenefactorACL((mask | EntityBundle.BENEFACTOR_ACL) > 0);
		request.setIncludeDOIAssociation((mask | EntityBundle.DOI) > 0);
		request.setIncludeFileName((mask | EntityBundle.FILE_NAME) > 0);
		request.setIncludeThreadCount((mask | EntityBundle.THREAD_COUNT) > 0);
		request.setIncludeRestrictionInformation((mask | EntityBundle.RESTRICTION_INFORMATION) > 0);
		return request;
	}

}
