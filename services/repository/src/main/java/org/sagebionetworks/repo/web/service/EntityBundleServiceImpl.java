package org.sagebionetworks.repo.web.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
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
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	public EntityBundle getEntityBundle(UserInfo userInfo, String entityId, EntityBundleRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userInfo, entityId, null, request);
	}

	@Override
	public EntityBundle getEntityBundle(UserInfo userInfo, String entityId,
										Long versionNumber, EntityBundleRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		EntityBundle eb = new EntityBundle();
		Entity entity = null;
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(entityId, versionNumber);
		if (isTrue(request.getIncludeEntity()) || isTrue(request.getIncludeFileName())) {
			if(versionNumber == null) {
				entity = serviceProvider.getEntityService().getEntity(userInfo, entityId);
			} else {
				entity = serviceProvider.getEntityService().getEntityForVersion(userInfo, entityId, versionNumber);
			}
			if (isTrue(request.getIncludeEntity())) {
				eb.setEntity(entity);
				eb.setEntityType(EntityTypeUtils.getEntityTypeForClass(entity.getClass()));
			}
		}
		if (isTrue(request.getIncludeAnnotations())) {
			if(versionNumber == null) {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotations(userInfo, entityId));
			} else {
				eb.setAnnotations(serviceProvider.getEntityService().getEntityAnnotationsForVersion(userInfo, entityId, versionNumber));
			}
		}
		if (isTrue(request.getIncludePermissions())) {
			eb.setPermissions(serviceProvider.getEntityService().getUserEntityPermissions(userInfo, entityId));
		}
		if (isTrue(request.getIncludeEntityPath())) {
			List<EntityHeader> path = serviceProvider.getEntityService().getEntityPath(userInfo, entityId);
			EntityPath ep = new EntityPath();
			ep.setPath(path);
			eb.setPath(ep);
		}
		if (isTrue(request.getIncludeHasChildren())) {
			eb.setHasChildren(serviceProvider.getEntityService().doesEntityHaveChildren(userInfo, entityId));
		}
		if (isTrue(request.getIncludeAccessControlList())) {
			try {
				eb.setAccessControlList(serviceProvider.getEntityService().getEntityACL(entityId, userInfo));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. Set ACL to null.
				eb.setAccessControlList(null);
			}
		}
		if (isTrue(request.getIncludeBenefactorACL())) {
			try {
				// If this entity is its own benefactor then we just get the ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(entityId, userInfo));
			} catch (ACLInheritanceException e) {
				// ACL is inherited from benefactor. So get the benefactor's ACL
				eb.setBenefactorAcl(serviceProvider.getEntityService().getEntityACL(e.getBenefactorId(), userInfo));
			}
		}
		List<FileHandle> fileHandles = null;
		if (isTrue(request.getIncludeFileHandles()) || isTrue(request.getIncludeFileName())) {
			try {
				if (versionNumber == null) {
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForCurrentVersion(userInfo, entityId).getList();
				} else{
					fileHandles = serviceProvider.getEntityService().
							getEntityFileHandlesForVersion(userInfo, entityId, versionNumber).getList();
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
				WikiPageKey rootKey = serviceProvider.getWikiService().getRootWikiKey(userInfo, entityId, ObjectType.ENTITY);
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
			EntityThreadCounts result = serviceProvider.getDiscussionService().getThreadCounts(userInfo, entityIdList );
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
			RestrictionInformationResponse restrictionInfo = serviceProvider.getDataAccessService().getRestrictionInformation(userInfo, restrictionInfoRequest);
			eb.setRestrictionInformation(restrictionInfo);
		}
		return eb;
	}


	@WriteTransaction
	@Override
	public EntityBundle createEntityBundle(UserInfo userInfo, EntityBundleCreate ebc, String activityId) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if (ebc.getEntity() == null) {
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}

		EntityBundleRequest fetchRequest = new EntityBundleRequest();

		// Create the Entity
		fetchRequest.setIncludeEntity(true);
		Entity toCreate = ebc.getEntity();
		Entity entity = serviceProvider.getEntityService().createEntity(userInfo, toCreate, activityId);

		// Create the ACL
		if (ebc.getAccessControlList() != null) {
			fetchRequest.setIncludeAccessControlList(true);
			AccessControlList acl = ebc.getAccessControlList();
			acl.setId(entity.getId());
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userInfo, acl);
		}

		// Create the Annotations
		if (ebc.getAnnotations() != null) {
			fetchRequest.setIncludeAnnotations(true);
			Annotations annos =serviceProvider.getEntityService().getEntityAnnotations(userInfo, entity.getId());
			annos.getAnnotations().putAll(ebc.getAnnotations().getAnnotations());
			serviceProvider.getEntityService().updateEntityAnnotations(userInfo, entity.getId(), annos);
		}

		return getEntityBundle(userInfo, entity.getId(), fetchRequest);
	}

	@WriteTransaction
	@Override
	public EntityBundle updateEntityBundle(UserInfo userInfo, String entityId,
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
			entity = serviceProvider.getEntityService().updateEntity(userInfo, entity, false, activityId);
		}

		// Update the ACL
		if (ebc.getAccessControlList() != null) {
			Long entityKey = KeyFactory.stringToKey(entityId);
			Long aclKey = KeyFactory.stringToKey(acl.getId());
			if (!entityKey.equals(aclKey)) {
				throw new IllegalArgumentException("ACL does not match requested entity ID");
			}
			fetchRequest.setIncludeAccessControlList(true);
			acl = serviceProvider.getEntityService().createOrUpdateEntityACL(userInfo, acl);
		}

		// Update the Annotations
		if (ebc.getAnnotations() != null) {
			if (!entityId.equals(ebc.getAnnotations().getId()))
				throw new IllegalArgumentException("Annotations do not match requested entity ID");
			fetchRequest.setIncludeAnnotations(true);
			Annotations toUpdate = serviceProvider.getEntityService().getEntityAnnotations(userInfo, entityId);
			toUpdate.getAnnotations().putAll(annos.getAnnotations());
			serviceProvider.getEntityService().updateEntityAnnotations(userInfo, entityId, toUpdate);
		}

		return getEntityBundle(userInfo, entityId, fetchRequest);
	}

	@Deprecated
	@Override
	public org.sagebionetworks.repo.model.EntityBundle getEntityBundle(UserInfo userInfo, String entityId, int mask)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException {
		return getEntityBundle(userInfo, entityId, null, mask);
	}

	@Deprecated
	@Override
	public org.sagebionetworks.repo.model.EntityBundle getEntityBundle(UserInfo userInfo, String entityId,
																	   Long versionNumber, int mask)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException, ParseException {

		//translate from V2 bundle
		org.sagebionetworks.repo.model.EntityBundle eb = translateEntityBundle(getEntityBundle(userInfo,entityId,versionNumber, requestFromMask(mask)));

		///additional deprecated flags not supported by V2 bundle
		if ((mask & org.sagebionetworks.repo.model.EntityBundle.ENTITY_REFERENCEDBY) > 0) {
			throw new IllegalArgumentException("ENTITY_REFERENCEDBY is deprecated.");
		}
		if ((mask & org.sagebionetworks.repo.model.EntityBundle.ACCESS_REQUIREMENTS) > 0) {
			// This is deprecated.
			eb.setAccessRequirements(new LinkedList<AccessRequirement>());
		}
		if ((mask & org.sagebionetworks.repo.model.EntityBundle.UNMET_ACCESS_REQUIREMENTS) > 0) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setId(entityId);
			subjectId.setType(RestrictableObjectType.ENTITY);
			eb.setUnmetAccessRequirements(accessRequirementManager.getAllUnmetAccessRequirements(
					userInfo, subjectId, ACCESS_TYPE.DOWNLOAD));
		}
		return eb;
	}

	@WriteTransaction
	@Override
	@Deprecated
	public org.sagebionetworks.repo.model.EntityBundle createEntityBundle(UserInfo userInfo, org.sagebionetworks.repo.model.EntityBundleCreate ebc, String activityId) throws ConflictingUpdateException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		if (ebc.getEntity() == null) {
			throw new IllegalArgumentException("Invalid request: no entity to create");
		}
		return translateEntityBundle(createEntityBundle(userInfo, translateEntityBundleCreate(ebc), activityId));
	}

	@WriteTransaction
	@Override
	@Deprecated
	public org.sagebionetworks.repo.model.EntityBundle updateEntityBundle(UserInfo userInfo, String entityId,
																		  org.sagebionetworks.repo.model.EntityBundleCreate ebc, String activityId)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,
			ACLInheritanceException, ParseException {
		
		return translateEntityBundle(updateEntityBundle(userInfo, entityId, translateEntityBundleCreate(ebc), activityId));
	}

	@Deprecated
	static org.sagebionetworks.repo.model.EntityBundle translateEntityBundle(EntityBundle v2Bundle){
		org.sagebionetworks.repo.model.EntityBundle entityBundle = new org.sagebionetworks.repo.model.EntityBundle();
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
	static EntityBundleCreate translateEntityBundleCreate(org.sagebionetworks.repo.model.EntityBundleCreate entityBundleCreate){
		EntityBundleCreate v2Create = new EntityBundleCreate();
		v2Create.setEntity(entityBundleCreate.getEntity());
		v2Create.setAnnotations(AnnotationsV2Translator.toAnnotationsV2(entityBundleCreate.getAnnotations()));
		v2Create.setAccessControlList(entityBundleCreate.getAccessControlList());
		return v2Create;
	}

	@Deprecated
	static EntityBundleRequest requestFromMask(int mask){
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeEntity((mask & org.sagebionetworks.repo.model.EntityBundle.ENTITY) > 0);
		request.setIncludeAnnotations((mask & org.sagebionetworks.repo.model.EntityBundle.ANNOTATIONS) > 0);
		request.setIncludePermissions((mask & org.sagebionetworks.repo.model.EntityBundle.PERMISSIONS) > 0);
		request.setIncludeEntityPath((mask & org.sagebionetworks.repo.model.EntityBundle.ENTITY_PATH) > 0);
		request.setIncludeHasChildren((mask & org.sagebionetworks.repo.model.EntityBundle.HAS_CHILDREN) > 0);
		request.setIncludeAccessControlList((mask & org.sagebionetworks.repo.model.EntityBundle.ACL) > 0);
		request.setIncludeFileHandles((mask & org.sagebionetworks.repo.model.EntityBundle.FILE_HANDLES) > 0);
		request.setIncludeTableBundle((mask & org.sagebionetworks.repo.model.EntityBundle.TABLE_DATA) > 0);
		request.setIncludeRootWikiId((mask & org.sagebionetworks.repo.model.EntityBundle.ROOT_WIKI_ID) > 0);
		request.setIncludeBenefactorACL((mask & org.sagebionetworks.repo.model.EntityBundle.BENEFACTOR_ACL) > 0);
		request.setIncludeDOIAssociation((mask & org.sagebionetworks.repo.model.EntityBundle.DOI) > 0);
		request.setIncludeFileName((mask & org.sagebionetworks.repo.model.EntityBundle.FILE_NAME) > 0);
		request.setIncludeThreadCount((mask & org.sagebionetworks.repo.model.EntityBundle.THREAD_COUNT) > 0);
		request.setIncludeRestrictionInformation((mask & org.sagebionetworks.repo.model.EntityBundle.RESTRICTION_INFORMATION) > 0);
		return request;
	}

}
