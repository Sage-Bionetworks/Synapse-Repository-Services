package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

/*
 * This provider returns OpenID Connect Claims of a given type
 */
public interface OIDCClaimProvider {
	/**
	 * get the Claim Name enum for which this provider returns a claim value
	 * 
	 * @return
	 */
	public OIDCClaimName getName();
	
	/**
	 * Get the user readable description of the claim, suitable for presentation to
	 * the user when requesting their consent.
	 * 
	 * @return
	 */
	public String getDescription();
	
	/**
	 * Get the claim value for the claim type handled by this provider
	 * 
	 * @param userId the ID of the user for which this claim is provided.  This value is always provided and never null.
	 * @param details the details of the request (e.g. for the team claim type the list of team IDs to check)
	 * A null value means to provide the 'standard' claim value for the specified user and the claim name.
	 * @return an object convertable to JSON in the resulting JSON Web Token.  The conversion is
	 * done by the FasterXML Object mapper which supports a variety of types including String, boolean, 
	 * number and collections of the above.
	 */
	public Object getClaim(String userId, OIDCClaimsRequestDetails details);
}
