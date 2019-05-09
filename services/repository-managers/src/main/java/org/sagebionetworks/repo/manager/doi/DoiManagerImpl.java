package org.sagebionetworks.repo.manager.doi;

import java.sql.Timestamp;
import java.util.UUID;

import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

public class DoiManagerImpl implements DoiManager {

	@Autowired
	private StackConfiguration stackConfiguration;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DoiAssociationDao doiAssociationDao;
	@Autowired
	private DataciteClient dataciteClient;

	public static final String ENTITY_URL_PREFIX = "#!Synapse:";
	public static final String LOCATE_RESOURCE_PATH = "/doi/locate";
	public static final String OBJECT_ID_PATH_PARAM = "id";
	public static final String OBJECT_TYPE_PATH_PARAM = "type";
	public static final String OBJECT_VERSION_PATH_PARAM = "version";

	public Doi getDoi(final String objectId, final ObjectType objectType, final Long versionNumber) throws ServiceUnavailableException {
		// Retrieve our record of the DOI/object association.
		DoiAssociation association = getDoiAssociation(objectId, objectType, versionNumber);

		// Get the metadata from DataCite. If their API is down, this may fail with NotReadyException/ServiceUnavailableException
		DataciteMetadata metadata = null;
		try {
			metadata = dataciteClient.get(association.getDoiUri());
		} catch (NotReadyException e) {
			throw new ServiceUnavailableException(e);
		}
		return mergeMetadataAndAssociation(metadata, association);
	}

	public DoiAssociation getDoiAssociation(final String objectId, final ObjectType objectType, final Long versionNumber) {
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null or empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null or empty.");
		}

		// No need to check authorization, DOIs are public
		DoiAssociation association = doiAssociationDao.getDoiAssociation(objectId, objectType, versionNumber);
		association.setDoiUri(generateDoiUri(objectId, objectType, versionNumber));
		association.setDoiUrl(generateLocationRequestUrl(objectId, objectType, versionNumber));
		return association;
	}

	@WriteTransaction
	public Doi createOrUpdateDoi(final UserInfo user, final Doi dto) throws RecoverableMessageException {
		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null");
		}
		if (dto.getObjectType() == null || !dto.getObjectType().equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Object must be an entity.");
		}

		// Ensure the user is authorized to update the object that we are minting a DOI for
		UserInfo.validateUserInfo(user);
		authorizationManager.canAccess(user, dto.getObjectId(), dto.getObjectType(), ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

		// Set updated fields
		dto.setUpdatedBy(user.getId().toString());
		// MySQL TIMESTAMP only keeps seconds (not ms)
		dto.setUpdatedOn(new Timestamp(DateTime.now().getMillis() / 1000L * 1000L));

		DoiAssociation association = createOrUpdateAssociation(dto);
		dto.setDoiUri(generateDoiUri(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()));
		dto.setDoiUrl(generateLocationRequestUrl(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()));
		DataciteMetadata metadata = createOrUpdateDataciteMetadata(dto);
		return mergeMetadataAndAssociation(metadata, association);
	}

	DoiAssociation createOrUpdateAssociation(DoiAssociation dto) throws RecoverableMessageException {
		DoiAssociation association;
		DoiAssociation existing = doiAssociationDao.getDoiAssociationForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
		if (existing != null) {
			if (!existing.getEtag().equals(dto.getEtag())) {
				// We say "cannot create" because the client may have called "createOrUpdate" before discovering that
				// another client created a DOI
				throw new ConflictingUpdateException("Cannot create or update the DOI because the submitted eTag does not match the existing eTag.");
			}

			// Set fields from the old object that the client cannot change
			dto.setAssociationId(existing.getAssociationId());
			dto.setAssociatedBy(existing.getAssociatedBy());
			dto.setAssociatedOn(existing.getAssociatedOn());
			dto.setEtag(UUID.randomUUID().toString());
			association = doiAssociationDao.updateDoiAssociation(dto);
		} else { // The DOI does not already exist
			try {
				dto.setAssociatedBy(dto.getUpdatedBy());
				dto.setAssociatedOn(dto.getUpdatedOn());
				dto.setEtag(UUID.randomUUID().toString());
				association = doiAssociationDao.createDoiAssociation(dto); // Create
			} catch (DuplicateKeyException e2) {
					/*
					 * This exception indicates there was a race condition where two callers attempted to create a DOI on the
					 * same object at the same time. The loser of this race will see this exception. However, since the
					 * winner might also fail before completion, we send this caller back to the beginning to retry.
					 */
					throw new RecoverableMessageException(e2);
			}
		}
		return association;
	}

	DataciteMetadata createOrUpdateDataciteMetadata(Doi dto) throws RecoverableMessageException {
		if (dto.getDoiUri() == null) {
			throw new IllegalArgumentException("DOI URI cannot be null");
		}
		if (dto.getDoiUrl() == null) {
			throw new IllegalArgumentException("DOI URL cannot be null");
		}

		try {
			dataciteClient.registerMetadata(dto, dto.getDoiUri());
			dataciteClient.registerDoi(dto.getDoiUri(), dto.getDoiUrl());
			return dataciteClient.get(dto.getDoiUri());
		} catch (NotReadyException | ServiceUnavailableException e) {
			/*
			 * The second call to DataCite may fail because the calls are "eventually consistent". It may also be the
			 * case that the external API is temporarily down. The client may decide to retry minting the DOI.
			 */
			throw new RecoverableMessageException(e);
		}
	}

	public void deactivateDoi(final UserInfo user, final String objectId, final ObjectType objectType, final Long versionNumber) throws RecoverableMessageException {
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null");
		}
		if (objectType == null || !objectType.equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Object type must be entity.");
		}

		// Ensure the user is authorized to update the object with the DOI (should verify that the object exists)
		UserInfo.validateUserInfo(user);
		authorizationManager.canAccess(user, objectId, objectType, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

		// Retrieve the DOI (verify that it has been minted)
		DoiAssociation doi = doiAssociationDao.getDoiAssociation(objectId, objectType, versionNumber);

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
		final String PERSISTENT_REPOSITORY_ENDPOINT = "https://repo-" + stackConfiguration.getStack() + "." + stackConfiguration.getStack() + ".sagebase.org/repo/v1";
		String request = PERSISTENT_REPOSITORY_ENDPOINT + LOCATE_RESOURCE_PATH;
		request += "?" + OBJECT_ID_PATH_PARAM + "=" + objectId + "&" + OBJECT_TYPE_PATH_PARAM + "=" + objectType.name();
		if (versionNumber != null) {
			request += "&" + OBJECT_VERSION_PATH_PARAM + "=" + versionNumber;
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
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null");
		}
		if (objectType == null || !objectType.equals(ObjectType.ENTITY)) {
			throw new IllegalArgumentException("Object type must be entity.");
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
