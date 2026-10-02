package org.sagebionetworks.repo.manager.search;

import java.util.Optional;

/**
 * Resolves the platform's embedding model on the OpenSearch domain: the ML-Commons remote model,
 * backed by a connector to Bedrock, that both the build-time ingestion pipeline and the query-time
 * {@code neural} clause reference. Synapse-Stack-Builder provisions those resources; this stack only
 * reads them.
 */
public interface SemanticEmbeddingBootstrapper {

	/**
	 * A deployed embedding model.
	 *
	 * @param modelId   The ML-Commons model id to place on an ingestion processor or {@code neural}
	 *                  clause. ML-Commons mints a fresh id on every registration.
	 * @param model     The foundation model the connector calls, e.g.
	 *                  {@code amazon.titan-embed-text-v2:0}.
	 * @param dimension The width of the vectors the model returns.
	 */
	record SemanticEmbeddingModel(String modelId, String model, int dimension) {

		/**
		 * The compatibility stamp for an index built with this model, as {@code <model>/<dimension>}.
		 * It omits the model id so a re-registration of the same foundation model does not mark every
		 * index incompatible.
		 */
		public String spec() {
			return model + "/" + dimension;
		}
	}

	/**
	 * The model cached by the last {@link #bootstrapSemanticEmbedding()}, or empty when none is
	 * deployed or none has been resolved yet. Never calls the domain.
	 */
	Optional<SemanticEmbeddingModel> getModel();

	/**
	 * Re-resolve the deployed model and replace this node's cached one: picks up a model provisioned
	 * or re-registered since the last call, and clears the cache when the model is no longer deployed.
	 * A failed lookup keeps the cached model. Read-only on the domain, so every node calls it on its
	 * own schedule.
	 */
	void bootstrapSemanticEmbedding();
}
