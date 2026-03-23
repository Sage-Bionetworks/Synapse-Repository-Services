package org.sagebionetworks.repo.manager.search;

/**
 * Bootstraps the system-defined text analyzers into the repository.
 */
public interface TextAnalyzerBootstrap {

	/**
	 * Create or update all system text analyzers within the org.sagebionetworks organization.
	 */
	void bootstrapSystemAnalyzers();

}
