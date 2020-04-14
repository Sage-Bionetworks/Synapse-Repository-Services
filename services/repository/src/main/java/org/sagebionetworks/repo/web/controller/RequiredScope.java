package org.sagebionetworks.repo.web.controller;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sagebionetworks.repo.model.oauth.OAuthScope;

@Retention(RUNTIME)
@Target(METHOD)
public @interface RequiredScope {
	OAuthScope[] value();
}
