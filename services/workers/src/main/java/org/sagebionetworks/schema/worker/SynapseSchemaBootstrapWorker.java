package org.sagebionetworks.schema.worker;

import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.manager.search.SemanticEmbeddingBootstrapper;
import org.sagebionetworks.repo.manager.search.TextAnalyzerBootstrap;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A periodic singleton worker that ensures the Synapse schema objects are
 * translated and registered and available for the JSON Schema services,
 * that system text analyzers are bootstrapped, and that the semantic
 * embedding model is re-resolved.
 *
 */
public class SynapseSchemaBootstrapWorker implements ProgressingRunner {

	private final SynapseSchemaBootstrap bootstrap;
	private final TextAnalyzerBootstrap textAnalyzerBootstrap;
	private final SemanticEmbeddingBootstrapper semanticEmbeddingBootstrapper;


	@Autowired
	public SynapseSchemaBootstrapWorker(SynapseSchemaBootstrap bootstrap, TextAnalyzerBootstrap textAnalyzerBootstrap,
			SemanticEmbeddingBootstrapper semanticEmbeddingBootstrapper) {
		this.bootstrap = bootstrap;
		this.textAnalyzerBootstrap = textAnalyzerBootstrap;
		this.semanticEmbeddingBootstrapper = semanticEmbeddingBootstrapper;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		bootstrap.bootstrapSynapseSchemas();
		textAnalyzerBootstrap.bootstrapSystemAnalyzers();
		semanticEmbeddingBootstrapper.bootstrapSemanticEmbedding();
	}

}
