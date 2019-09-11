package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

/*
 * This provider returns OpenID Connect Claims of a given type
 */
public interface OIDCClaimProvider {
	public OIDCClaimName getName();
	
	public String getDescription();
	
	public Object getClaim(String userId, OIDCClaimsRequestDetails details);
}
