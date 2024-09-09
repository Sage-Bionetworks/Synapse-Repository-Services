package org.sagebionetworks.javadoc.velocity;

import java.util.Optional;

import jdk.javadoc.doclet.DocletEnvironment;

public class ContextInputImpl implements ContextInput {

	private final ContextFactory contextFactory;
	private final DocletEnvironment docletEnvironment;
	private final String authControllerName;

	public ContextInputImpl(ContextFactory contextFactory, DocletEnvironment docletEnvironment,
			String authControllerName) {
		super();
		this.contextFactory = contextFactory;
		this.docletEnvironment = docletEnvironment;
		this.authControllerName = authControllerName;
	}

	@Override
	public ContextFactory getContextFactory() {
		return contextFactory;
	}

	@Override
	public DocletEnvironment getDocletEnvironment() {
		return docletEnvironment;
	}

	@Override
	public Optional<String> getAuthControllerName() {
		return Optional.ofNullable(authControllerName);
	}

}
