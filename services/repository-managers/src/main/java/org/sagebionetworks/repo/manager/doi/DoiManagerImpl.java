package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.DataciteRegistrationStatus;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;


public class DoiManagerImpl implements DoiManager {

	@Autowired
	private StackConfiguration stackConfiguration;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DoiAssociationDao doiAssociationDao;
	@Autowired
	private DataciteClient dataciteClient;

	public static final String ENTITY_URL_PREFIX = "#!Synapse:";
	public static final String RESOURCE_PATH = "/doi/locate";

	public Doi getDoi(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber) throws ServiceUnavailableException {
		// Retrieve our record of the DOI/object association.
		// Authorization is determined in the retrieval method
		DoiAssociation association = getDoiAssociation(userId, objectId, objectType, versionNumber);

		// Get the metadata from DataCite. If their API is down, this may fail with NotReadyException/ServiceUnavailableException
		DataciteMetadata metadata = null;
		try {
			metadata = dataciteClient.get(association.getDoiUri());
		} catch (NotReadyException | ServiceUnavailableException e) {
			throw new ServiceUnavailableException(e);
		}
		return mergeMetadataAndAssociation(metadata, association);
	}

	public DoiAssociation getDoiAssociation(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null or empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null or empty.");
		}

		// Ensure the user is authorized to view the object that we are retrieving
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, objectId, objectType, ACCESS_TYPE.READ));

		DoiAssociation association = doiAssociationDao.getDoiAssociation(objectId, objectType, versionNumber);
		association.setDoiUri(generateDoiUri(objectId, objectType, versionNumber));
		association.setDoiUrl(generateLocationRequestUrl(objectId, objectType, versionNumber));
		return association;
	}

	@WriteTransactionReadCommitted
	public Doi createOrUpdateDoi(final Long userId, final Doi dto) throws RecoverableMessageException {
		if (userId == null) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null");
		}
		if (!dto.getObjectType().equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Object must be an entity.");
		}

		// Ensure the user is authorized to update the object that we are minting a DOI for
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, dto.getObjectId(), dto.getObjectType(), ACCESS_TYPE.UPDATE));

		// Check if the DOI exists, and if so, check to make sure eTags match.
		try {
			if (!doiAssociationDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()).equals(dto.getEtag())) {
				throw new ConflictingUpdateException("eTags do not match.");
			}
			doiAssociationDao.updateDoiAssociation(dto);
		} catch (NotFoundException e1) {
			try {
				doiAssociationDao.createDoiAssociation(dto); //retry on fail
			} catch (Exception e2) {
				throw new RecoverableMessageException(e2);
			}
		}

		String uri = generateDoiUri(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
		String url = generateLocationRequestUrl(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
		try {
			dataciteClient.registerMetadata(dto, uri);
			dataciteClient.registerDoi(uri, url);
			if (dto.getStatus() == DataciteRegistrationStatus.REGISTERED) {
				dataciteClient.deactivate(uri);
			}
		} catch (NotReadyException | ServiceUnavailableException e) {
			throw new RecoverableMessageException(e);
		}

		return dto;
	}


	public void deactivateDoi(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber) throws RecoverableMessageException {
		if (userId == null) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}

		// Ensure the user is authorized to update the object with the DOI (should verify that the object exists)
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, objectId, objectType, ACCESS_TYPE.UPDATE));

		// Retrieve the DOI (verify that it has been minted)
		DoiAssociation doi = getDoiAssociation(userId, objectId, objectType, versionNumber);

		try {
			dataciteClient.deactivate(doi.getDoiUri());
		} catch (NotReadyException | ServiceUnavailableException e) {
			throw new RecoverableMessageException(e);
		}
	}

	public String getLocation(String objectId, ObjectType objectType, Long versionNumber) {
		if (!objectType.equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Retrieving the location of an object currently only supports entities");
		}
		String url = stackConfiguration.getSynapseBaseUrl();
		if (objectType.equals(ObjectType.ENTITY)) {
			url += ENTITY_URL_PREFIX + objectId;
			if (versionNumber != null) {
				url += "/version/" + versionNumber;
			}
		}
		return url;
	}

	public String generateLocationRequestUrl(String objectId, ObjectType objectType, Long versionNumber) {
		if (!objectType.equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Generating a location request currently only supports entities");
		}
		String request = stackConfiguration.getRepositoryServiceEndpoint() + RESOURCE_PATH;
		request += "?id=" + objectId + "&objectType=" + objectType.name();
		if (versionNumber != null) {
			request += "&version=" + versionNumber;
		}
		return request;
	}

	/**
	 * Generates a doiUri from the scheme {DOI_URI_PREFIX}/{object type prefix}{objectId}<.{version}>
	 * @param objectId The ID of the object to which a URI should refer
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, the URI should always refer to the most recent version
	 * @return A well-formatted DOI URI that should refer to the input object.
	 */
	String generateDoiUri(final String objectId, final ObjectType objectType, final Long versionNumber) {
		if (!objectType.equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("DOIs currently only support Entity");
		}
		String uri = "";
		uri += stackConfiguration.getDoiPrefix() + "/";
		uri += objectId;
		if (versionNumber != null) {
			uri += "." + versionNumber;
		}
		return uri;
	}

	static Doi mergeMetadataAndAssociation(DataciteMetadata metadata, DoiAssociation association) {
		Doi doi = new Doi();
		// Copy from metadata
		doi.setCreators(metadata.getCreators());
		doi.setPublicationYear(metadata.getPublicationYear());
		doi.setResourceType(metadata.getResourceType());
		doi.setStatus(metadata.getStatus());
		doi.setTitles(metadata.getTitles());
		// Copy from association
		doi.setAssociationId(association.getAssociationId());
		doi.setObjectId(association.getObjectId());
		doi.setObjectType(association.getObjectType());
		doi.setObjectVersion(association.getObjectVersion());
		doi.setAssociatedBy(association.getAssociatedBy());
		doi.setAssociatedOn(association.getAssociatedOn());
		doi.setUpdatedBy(association.getUpdatedBy());
		doi.setUpdatedOn(association.getUpdatedOn());
		doi.setEtag(association.getEtag());
		doi.setDoiUri(association.getDoiUri());
		doi.setDoiUrl(association.getDoiUrl());
		return doi;
	}
}
