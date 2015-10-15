package org.sagebionetworks.repo.manager.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class OrcidOauth2Provider implements OAuthAuthenticationProviderBinding,
		OAuthIDAssociationProviderBinding {

	/*
	 * "/authenticate scope indicates to ORCID that we just want to request the user's ORCID ID
	 * after authentication.
	 */
	// reference http://members.orcid.org./api/orcid-scopes
	private static final String SCOPE_AUTHENTICATE = "/authenticate"; 
	
	public static final String ORCID = "orcid";


	private String apiKey;
	private String apiSecret;
	
	public OrcidOauth2Provider(String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}


	/**
	 * Build a service using the provided redirectUrl
	 * @param redirectUrl
	 * @return
	 */
	private OAuthService buildService(String redirectUrl) {
		return new ServiceBuilder()
		.provider(ORCID2Api.class)
		.apiKey(apiKey)
		.apiSecret(apiSecret)
		.callback(redirectUrl)
		.build();
	}

	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return  new ORCID2Api().getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, SCOPE_AUTHENTICATE, null));
	}

	@Override
	public String associateProvidersId(String authorizationCode,
			String redirectUrl) {
		if(redirectUrl == null){
			throw new IllegalArgumentException("RedirectUrl cannot be null");
		}
		try{
			OAuthService service = buildService(redirectUrl);
			/*
			 * Get an access token from ORCID using the provided authorization code.
			 * This token is used to sign request for user's information.
			 */
			Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));
			return parseOrcidId(accessToken.getRawResponse());
		}catch(OAuthException e){
			throw new UnauthorizedException(e);
		}
	}
	
	/**
	 * Parse the Response from https://api.orcid.org/oauth/token.
	 * 
	 * @param body
	 * @return
	 */
	public String parseOrcidId(String body){
		try {
			JSONObject json = new JSONObject(body);
			if(json.has(ORCID)){
				return json.getString(ORCID);
			} else {
				throw new RuntimeException("Missing "+ORCID+" from "+body);
			}
		} catch (JSONException e) {
			throw new UnauthorizedException(e);
		}
	}



}
