package org.sagebionetworks.schema.worker;

import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.manager.search.TextAnalyzerBootstrap;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A periodic singleton worker that ensures the Synapse schema objects are
 * translated and registered and available for the JSON Schema services,
 * and that system text analyzers are bootstrapped.
 *
 */
public class SynapseSchemaBootstrapWorker implements ProgressingRunner {

	private final SynapseSchemaBootstrap bootstrap;
	private final TextAnalyzerBootstrap textAnalyzerBootstrap;


	@Autowired
	public SynapseSchemaBootstrapWorker(SynapseSchemaBootstrap bootstrap, TextAnalyzerBootstrap textAnalyzerBootstrap) {
		this.bootstrap = bootstrap;
		this.textAnalyzerBootstrap = textAnalyzerBootstrap;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		bootstrap.bootstrapSynapseSchemas();
		textAnalyzerBootstrap.bootstrapSystemAnalyzers();
	}

}
