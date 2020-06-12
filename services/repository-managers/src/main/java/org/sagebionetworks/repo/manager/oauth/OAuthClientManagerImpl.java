package org.sagebionetworks.repo.manager.oauth;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.PrivateFieldUtils;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthClientManagerImpl implements OAuthClientManager {
	
	@Autowired
	private OAuthClientDao oauthClientDao;
	
	@Autowired
	private SimpleHttpClient httpClient;
	
	@Autowired
	private AuthorizationManager authManager;

	public static void validateOAuthClientForCreateOrUpdate(OAuthClient oauthClient) {
		ValidateArgument.required(oauthClient.getClient_name(), "OAuth client name");
		ValidateArgument.required(oauthClient.getRedirect_uris(), "OAuth client redirect URI list.");
		ValidateArgument.requirement(!oauthClient.getRedirect_uris().isEmpty(), "OAuth client must register at least one redirect URI.");
		for (String uri: oauthClient.getRedirect_uris()) {
			ValidateArgument.validUrl(uri, "Redirect URI");
		}
		if (StringUtils.isNotEmpty(oauthClient.getSector_identifier_uri())) {
			ValidateArgument.validUrl(oauthClient.getSector_identifier_uri(), "Sector Identifier URI");
		}
	}

	public List<String> readSectorIdentifierFile(URI uri) throws ServiceUnavailableException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(uri.toString());
		SimpleHttpResponse response = null;
		try {
			response = httpClient.get(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Failed to read the content of "+uri+
					".  Please check the URL and the file at the address, then try again.", e);
		}
		if (response.getStatusCode() != HttpStatus.SC_OK) {
			throw new ServiceUnavailableException("Received "+response.getStatusCode()+" status while trying to read the content of "+uri+
					".  Please check the URL and the file at the address, then try again.");
		}
		List<String> result = new ArrayList<String>();
		JSONArray array;
		try {
			array =  new JSONArray(response.getContent());
			for (int i=0; i<array.length(); i++) {
				result.add(array.getString(i));
			}
		} catch (JSONException e) {
			throw new IllegalArgumentException("The content of "+uri+" is not a valid JSON array of strings.", e);
		}
		return result;
	}
	
	public static URI getUri(String s) {
		if (s==null) throw new IllegalArgumentException("null URI is not allowed.");
		try {
			return new URI(s);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(s+" is not a valid URI.");
		}
	}

	// implements https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	public String resolveSectorIdentifier(String sectorIdentifierUriString, List<String> redirectUris) throws ServiceUnavailableException {
		if (StringUtils.isEmpty(sectorIdentifierUriString)) {
			ValidateArgument.required(redirectUris, "Redirect URI list");
			// the sector ID is the host common to all uris in the list
			String result=null;
			for (String uriString: redirectUris) {
				URI uri=getUri(uriString);
				if (result==null) {
					result=uri.getHost();
				} else {
					ValidateArgument.requirement(result.equals(uri.getHost()), 
							"if redirect URIs do not share a common host then you must register a sector identifier URI.");
				}
			}
			ValidateArgument.requirement(result!=null, "Missing redirect URI.");
			return result;
		} else {
			// scheme must be https
			URI uri=getUri(sectorIdentifierUriString);
			ValidateArgument.requirement(uri.getScheme().equalsIgnoreCase("https"), 
					sectorIdentifierUriString+" must use the https scheme.");
			// read file, parse json, and make sure it contains all of redirectUris values
			List<String> siList = readSectorIdentifierFile(uri);
			ValidateArgument.requirement(siList.containsAll(redirectUris), 
					"Not all of the submitted redirect URIs are found in the list hosted at "+uri);
			// As per https://openid.net/specs/openid-connect-registration-1_0.html#SectorIdentifierValidation,
			// the sector ID is the host of the sectorIdentifierUri
			return uri.getHost();
		}
	}

	private void ensureSectorIdentifierExists(String sectorIdentiferHostName, Long createdBy) {
		if (oauthClientDao.doesSectorIdentifierExistForURI(sectorIdentiferHostName)) {
			return;
		}
		SectorIdentifier sectorIdentifier = new SectorIdentifier();
		sectorIdentifier.setCreatedBy(createdBy);
		sectorIdentifier.setCreatedOn(System.currentTimeMillis());
		String sectorIdentifierSecret = EncryptionUtils.newSecretKey();
		sectorIdentifier.setSecret(sectorIdentifierSecret);
		sectorIdentifier.setSectorIdentifierUri(sectorIdentiferHostName);
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
	}

	public static boolean canCreate(UserInfo userInfo) {
		return !AuthorizationUtils.isUserAnonymous(userInfo);
	}

	public static boolean canAdministrate(UserInfo userInfo, String createdBy) {
		return createdBy.equals(userInfo.getId().toString()) || userInfo.isAdmin();
	}

	@WriteTransaction
	@Override
	public OAuthClient createOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) throws ServiceUnavailableException {
		if (!canCreate(userInfo)) {
			throw new UnauthorizedException("Anonymous user may not create an OAuth Client");
		}
		validateOAuthClientForCreateOrUpdate(oauthClient);

		oauthClient.setCreatedBy(userInfo.getId().toString());
		oauthClient.setEtag(UUID.randomUUID().toString());
		oauthClient.setVerified(false);

		String resolvedSectorIdentifier = resolveSectorIdentifier(oauthClient.getSector_identifier_uri(), oauthClient.getRedirect_uris());
		oauthClient.setSector_identifier(resolvedSectorIdentifier);
		// find or create SectorIdentifier
		ensureSectorIdentifierExists(resolvedSectorIdentifier, userInfo.getId());

		return oauthClientDao.createOAuthClient(oauthClient);
	}
	
	@Override
	public OAuthClient getOpenIDConnectClient(UserInfo userInfo, String id) {
		OAuthClient result = oauthClientDao.getOAuthClient(id);
		if (!canAdministrate(userInfo, result.getCreatedBy())) {
			PrivateFieldUtils.clearPrivateFields(result);
		}
		return result;
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(UserInfo userInfo, String nextPageToken) {
		return oauthClientDao.listOAuthClients(nextPageToken, userInfo.getId());
	}

	@WriteTransaction
	@Override
	public OAuthClient updateOpenIDConnectClient(UserInfo userInfo, OAuthClient toUpdate) throws ServiceUnavailableException {
		ValidateArgument.requiredNotEmpty(toUpdate.getClient_id(), "Client ID");
		OAuthClient currentClient = oauthClientDao.selectOAuthClientForUpdate(toUpdate.getClient_id());
		if (!canAdministrate(userInfo, currentClient.getCreatedBy())) {
			throw new UnauthorizedException("You can only update your own OAuth client(s).");
		}
		validateOAuthClientForCreateOrUpdate(toUpdate);
		
		ValidateArgument.requiredNotEmpty(toUpdate.getEtag(), "etag");
		if (!currentClient.getEtag().equals(toUpdate.getEtag())) {
			throw new ConflictingUpdateException(
					"OAuth Client was updated since you last fetched it.  Retrieve it again and reapply the update.");
		}
		
		String resolvedSectorIdentifier = resolveSectorIdentifier(toUpdate.getSector_identifier_uri(), toUpdate.getRedirect_uris());
		if (!resolvedSectorIdentifier.equals(currentClient.getSector_identifier())) {
			ensureSectorIdentifierExists(resolvedSectorIdentifier, userInfo.getId());
		}
		
		OAuthClient toStore = new OAuthClient();

		// now fill in 'toStore' with info from updatedClient
		// we *never* change: clientID, createdBy, createdOn
		// (1) immutable:
		toStore.setClient_id(currentClient.getClient_id());
		toStore.setCreatedBy(currentClient.getCreatedBy());
		toStore.setCreatedOn(currentClient.getCreatedOn());
		// (2) settable by client
		toStore.setClient_name(toUpdate.getClient_name());
		toStore.setClient_uri(toUpdate.getClient_uri());
		toStore.setPolicy_uri(toUpdate.getPolicy_uri());
		toStore.setTos_uri(toUpdate.getTos_uri());
		toStore.setUserinfo_signed_response_alg(toUpdate.getUserinfo_signed_response_alg());
		toStore.setRedirect_uris(toUpdate.getRedirect_uris());
		toStore.setSector_identifier_uri(toUpdate.getSector_identifier_uri());
		// set by system
		toStore.setModifiedOn(new Date());
		toStore.setEtag(UUID.randomUUID().toString());
		toStore.setVerified(currentClient.getVerified());
		toStore.setSector_identifier(resolvedSectorIdentifier);
		if (!resolvedSectorIdentifier.equals(currentClient.getSector_identifier())) {
			toStore.setVerified(false);
		}
		return oauthClientDao.updateOAuthClient(toStore);
	}
	
	@WriteTransaction
	@Override
	public OAuthClient updateOpenIDConnectClientVerifiedStatus(UserInfo userInfo, String clientId, String etag, boolean verifiedStatus) {
		ValidateArgument.required(userInfo, "User info");
		ValidateArgument.requiredNotBlank(clientId, "Client ID");
		ValidateArgument.requiredNotBlank(etag, "The etag");
		
		if (!authManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("You must be an administrator or a member of the ACT team to update the verification status of a client");
		}
		
		OAuthClient client = oauthClientDao.selectOAuthClientForUpdate(clientId);
		
		if (!client.getEtag().equals(etag)) {
			throw new ConflictingUpdateException("The client etag does not match, the client information might have been updated.");
		}
		
		if (verifiedStatus != BooleanUtils.isTrue(client.getVerified())) {
			client.setVerified(verifiedStatus);
			client.setModifiedOn(new Date());
			client.setEtag(UUID.randomUUID().toString());
			client = oauthClientDao.updateOAuthClient(client);
		}

		return client;
	}
	
	@WriteTransaction
	@Override
	public void deleteOpenIDConnectClient(UserInfo userInfo, String id) {
		String creator = oauthClientDao.getOAuthClientCreator(id);
		if (!canAdministrate(userInfo, creator)) {
			throw new UnauthorizedException("You can only delete your own OAuth client(s).");
		}
		oauthClientDao.deleteOAuthClient(id);
	}

	@WriteTransaction
	@Override
	public OAuthClientIdAndSecret createClientSecret(UserInfo userInfo, String clientId) {
		String creator = oauthClientDao.getOAuthClientCreator(clientId);
		if (!canAdministrate(userInfo, creator)) {
			throw new UnauthorizedException("You can only generate credentials for your own OAuth client(s).");
		}		
		String secret = PBKDF2Utils.generateSecureRandomString();
		String secretHash = PBKDF2Utils.hashPassword(secret, null);
		oauthClientDao.setOAuthClientSecretHash(clientId, secretHash, UUID.randomUUID().toString());
		OAuthClientIdAndSecret result = new OAuthClientIdAndSecret();
		result.setClient_id(clientId);
		result.setClient_secret(secret);
		return result;
	}

	@Override
	public boolean validateClientCredentials(OAuthClientIdAndSecret clientIdAndSecret) {
		ValidateArgument.required(clientIdAndSecret, "Client ID and Secret");
		if (StringUtils.isEmpty(clientIdAndSecret.getClient_id()) || StringUtils.isEmpty(clientIdAndSecret.getClient_secret())) {
			return false;
		}
		try {
			byte[] secretSalt = oauthClientDao.getSecretSalt(clientIdAndSecret.getClient_id());
			String hash = PBKDF2Utils.hashPassword(clientIdAndSecret.getClient_secret(), secretSalt);
			return oauthClientDao.checkOAuthClientSecretHash(clientIdAndSecret.getClient_id(), hash);
		} catch (NotFoundException e) {
			return false;
		}
	}

}
