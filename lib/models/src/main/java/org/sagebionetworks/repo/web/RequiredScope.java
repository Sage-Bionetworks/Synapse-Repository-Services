package org.sagebionetworks.repo.web;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sagebionetworks.repo.model.oauth.OAuthScope;

/*
 * This annotation is required on all Synapse Controllers.  It lists the OAuth Scopes
 * required by the Controller it annotates.  Its value is an array of required scopes.
 * If the OAuth access token in the authenticated request lacks the required scope
 * then a 403 Forbidden status is returned.
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface RequiredScope {
	public OAuthScope[] value();
}
