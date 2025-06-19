package org.sagebionetworks.repo.manager.doi;

import java.sql.Timestamp;
import java.util.UUID;

import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.portals.PortalManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.portals.DBOPortal;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

public class DoiManagerImpl implements DoiManager {
	
	// In order to maintain backward compatibility the API accepts an optional portalId parameter
	// that was added when DOI support was extended to external Synapse portals
	static String resolvePortalId(String portalId) {
		if (portalId == null) {
			return SYNAPSE_PORTAL_ID; 
		}
		return portalId;
	}

	@Autowired
	private StackConfiguration stackConfiguration;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private PortalManager portalManager;
	@Autowired
	private DoiAssociationDao doiAssociationDao;
	@Autowired
	private DataciteClient dataciteClient;

	public static final String ENTITY_URL_PREFIX = "#!Synapse:";
	
	public static final String LOCATE_RESOURCE_PATH = "/doi/locate";
	public static final String PORTAL_ID_PATH_PARAM = "portalId";
	public static final String OBJECT_ID_PATH_PARAM = "id";
	public static final String OBJECT_TYPE_PATH_PARAM = "type";
	public static final String OBJECT_VERSION_PATH_PARAM = "version";
	
	public static final String SYNAPSE_PORTAL_ID = DBOPortal.SYNAPSE_PORTAL_ID.toString();

	@Override
	public Doi getDoi(final String portalId, final String objectId, final DoiObjectType objectType, final Long versionNumber) throws ServiceUnavailableException {
		// Retrieve our record of the DOI/object association.
		DoiAssociation association = getDoiAssociation(portalId, objectId, objectType, versionNumber);

		// Get the metadata from DataCite. If their API is down, this may fail with NotReadyException/ServiceUnavailableException
		DataciteMetadata metadata = null;
		try {
			metadata = dataciteClient.get(association.getDoiUri());
		} catch (NotReadyException e) {
			throw new ServiceUnavailableException(e);
		}
		return mergeMetadataAndAssociation(metadata, association);
	}

	@Override
	public DoiAssociation getDoiAssociation(final String portalId, final String objectId, final DoiObjectType objectType, final Long versionNumber) {
		ValidateArgument.required(objectId, "The objectId");
		ValidateArgument.required(objectType, "The objectType");
		
		String resolvedPortalId = resolvePortalId(portalId);

		// No need to check authorization, DOIs are public
		DoiAssociation association = doiAssociationDao.getDoiAssociation(resolvedPortalId, objectId, objectType, versionNumber);
		
		association.setDoiUri(generateDoiUri(association));
		association.setDoiUrl(generateLocationRequestUrl(association));
		
		return association;
	}

	@Override
	@WriteTransaction
	public Doi createOrUpdateDoi(final UserInfo user, final Doi dto) throws RecoverableMessageException {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(dto.getObjectId(), "The objectId");
		ValidateArgument.required(dto.getObjectType(), "The objectType");
		
		dto.setPortalId(resolvePortalId(dto.getPortalId()));
				
		verifyDoiMintingAuthorization(user, dto.getPortalId(), dto.getObjectId(), dto.getObjectType());

		// Set updated fields
		dto.setUpdatedBy(user.getId().toString());
		// MySQL TIMESTAMP only keeps seconds (not ms)
		dto.setUpdatedOn(new Timestamp(DateTime.now().getMillis() / 1000L * 1000L));

		DoiAssociation association = createOrUpdateAssociation(dto);
		
		dto.setDoiUri(generateDoiUri(association));
		dto.setDoiUrl(generateLocationRequestUrl(association));
		
		DataciteMetadata metadata = createOrUpdateDataciteMetadata(dto);
		
		return mergeMetadataAndAssociation(metadata, association);
	}

	DoiAssociation createOrUpdateAssociation(DoiAssociation dto) throws RecoverableMessageException {
		DoiAssociation association;
		DoiAssociation existing = doiAssociationDao.getDoiAssociationForUpdate(dto.getPortalId(), dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
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
		ValidateArgument.required(dto.getPortalId(), "The portalId");
		ValidateArgument.required(dto.getDoiUri(), "The doiUri");
		ValidateArgument.required(dto.getDoiUrl(), "The doiUrl");
		
		// Makes sure the publisher of the DOI is managed by Synapse and set to the Portal name
		dto.setPublisher(portalManager.getPortal(dto.getPortalId()).getName());
		
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

	@Override
	public void deactivateDoi(final UserInfo user, final String portalId, final String objectId, final DoiObjectType objectType, final Long versionNumber) throws RecoverableMessageException {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(portalId, "The portalId");
		ValidateArgument.required(objectId, "The objectId");
		ValidateArgument.required(objectType, "The objectType");
		
		String resolvedPortalId = resolvePortalId(portalId);
		
		verifyDoiMintingAuthorization(user, resolvedPortalId, objectId, objectType);

		// Retrieve the DOI (verify that it has been minted)
		DoiAssociation doi = getDoiAssociation(resolvedPortalId, objectId, objectType, versionNumber);

		try {
			dataciteClient.deactivate(doi.getDoiUri());
		} catch (NotReadyException | ServiceUnavailableException e) {
			throw new RecoverableMessageException(e);
		}
	}
	
	void verifyDoiMintingAuthorization(UserInfo user, String portalId, String objectId, DoiObjectType objectType) {
		
		AuthorizationStatus authStatus;
		
		if (SYNAPSE_PORTAL_ID.equals(portalId)) {
			if (!objectType.equals(DoiObjectType.ENTITY)) {
				throw new IllegalArgumentException("Object must be an entity.");
			}
			// Ensure the user is authorized to update the object that we are minting a DOI for
			authStatus = authorizationManager.canAccess(user, objectId, ObjectType.valueOf(objectType.name()), ACCESS_TYPE.UPDATE);
		} else {
			if (!objectType.equals(DoiObjectType.PORTAL_RESOURCE)) {
				throw new IllegalArgumentException("Object must be a portal resource.");
			}
			// For an external portal, only the portal "administrator" can mint a DOI
			authStatus = portalManager.canMintDoi(user, portalId);
		}
		
		authStatus.checkAuthorizationOrElseThrow();
	}
	
	@Override
	public String getLocation(String portalId, String objectId, DoiObjectType objectType, Long versionNumber) {
		String resolvedPortalId = resolvePortalId(portalId);
		String url;
		
		if (SYNAPSE_PORTAL_ID.equals(resolvedPortalId)) {
			ValidateArgument.requirement(objectType.equals(DoiObjectType.ENTITY), "Unsupported objectType: " + objectType.name());
			
			url = stackConfiguration.getSynapseBaseUrl() + ENTITY_URL_PREFIX + objectId;
			
			if (versionNumber != null) {
				url += "/version/" + versionNumber;
			}
			
		} else {
			url = portalManager.getPortal(resolvedPortalId).getUrl() + "/doi?objectId=" + objectId;
		}
		
		return url;
	}

	String generateLocationRequestUrl(DoiAssociation association) {
		String stack = stackConfiguration.getStack();
		
		final String PERSISTENT_REPOSITORY_ENDPOINT = "https://repo-" + stack + "." + stack + ".sagebase.org/repo/v1";
		
		String request = PERSISTENT_REPOSITORY_ENDPOINT + LOCATE_RESOURCE_PATH;
		
		request += "?" 
			+ PORTAL_ID_PATH_PARAM + "=" + association.getPortalId() + "&" 
			+ OBJECT_ID_PATH_PARAM + "=" + association.getObjectId() + "&" 
			+ OBJECT_TYPE_PATH_PARAM + "=" + association.getObjectType().name();
		
		if (association.getObjectVersion() != null) {
			request += "&" + OBJECT_VERSION_PATH_PARAM + "=" + association.getObjectVersion();
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
	String generateDoiUri(DoiAssociation association) {
		
		String uri = stackConfiguration.getDoiPrefix() + "/";

		switch (association.getObjectType()) {
		case ENTITY:
			ValidateArgument.required(association.getObjectId(), "The objectId");
			uri += association.getObjectId();
			if (association.getObjectVersion() != null) {
				uri += "." + association.getObjectVersion();
			}
			break;
		case PORTAL_RESOURCE:
			ValidateArgument.required(association.getAssociationId(), "The associationId");
			uri += association.getAssociationId();
			break;
		default:
			throw new IllegalStateException("Unsupported object type: " + association.getObjectType());
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
		doi.setPublisher(metadata.getPublisher());
		
		// Copy from association
		doi.setAssociationId(association.getAssociationId());
		doi.setPortalId(association.getPortalId());
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
