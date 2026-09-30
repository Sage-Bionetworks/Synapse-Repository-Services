package org.sagebionetworks.table.cluster;

import org.sagebionetworks.table.cluster.description.QueryIndexDescription;

/**
 * Immutable and reusable container for dependencies needed to translate SQL
 * queries. Since this contain does not contain query specific details, it can
 * be shared to construct multiple SqlQuery translator.
 *
 */
public class TranslationDependencies {

	private final SchemaProvider schemaProvider;
	private final QueryIndexDescription indexDescription;
	private final Long userId;

	private TranslationDependencies(SchemaProvider schemaProvider, QueryIndexDescription indexDescription, Long userId) {
		super();
		this.schemaProvider = schemaProvider;
		this.indexDescription = indexDescription;
		this.userId = userId;
	}
	
	

	/**
	 * @return the schemaProvider
	 */
	public SchemaProvider getSchemaProvider() {
		return schemaProvider;
	}



	/**
	 * @return the indexDescription
	 */
	public QueryIndexDescription getIndexDescription() {
		return indexDescription;
	}



	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}



	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private SchemaProvider schemaProvider;
		private QueryIndexDescription indexDescription;
		private Long userId;

		/**
		 * @param schemaProvider the schemaProvider to set
		 */
		public Builder setSchemaProvider(SchemaProvider schemaProvider) {
			this.schemaProvider = schemaProvider;
			return this;
		}

		/**
		 * @param indexDescription the indexDescription to set
		 */
		public Builder setIndexDescription(QueryIndexDescription indexDescription) {
			this.indexDescription = indexDescription;
			return this;
		}

		/**
		 * @param userId the userId to set
		 */
		public Builder setUserId(Long userId) {
			this.userId = userId;
			return this;

		}

		public TranslationDependencies build() {
			return new TranslationDependencies(schemaProvider, indexDescription, userId);
		}
	}

}
