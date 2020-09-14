package org.sagebionetworks.repo.web;

/**
 * Error codes used for OAuth services, defined in the IANA OAuth Extensions Error registry: https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#extensions-error
 *
 * Proper usage of these error codes is informed by
 *  - OAuth 2.0 [RFC 6749] (https://tools.ietf.org/html/rfc6749)
 *  - OAuth 2.0 Token Revocation [RFC 7009] (https://tools.ietf.org/html/rfc7009)
 *  - OAuth 2.1 (draft) (https://oauth.net/2.1/)
 *  - OpenID Connect Core 1.0 (https://openid.net/specs/openid-connect-core-1_0.html)
 *
 */
public enum OAuthErrorCode {
	invalid_request,
	invalid_client,
	invalid_grant,
	invalid_token,
	unauthorized_client,
	unsupported_grant_type,
	unsupported_response_type,
	invalid_scope,
	insufficient_scope,
	unsupported_token_type,
	login_required
}
