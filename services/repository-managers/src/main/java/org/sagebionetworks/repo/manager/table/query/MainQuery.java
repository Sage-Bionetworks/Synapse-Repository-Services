package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TranslationDependencies;

public class MainQuery {

	private final SqlQuery sqlQuery;

	private MainQuery(String startingSql, TranslationDependencies dependencies, Query query, Long maxBytesPerPage) {

		CombinedQuery combined = CombinedQuery.builder().setQuery(startingSql)
				.setSchemaProvider(dependencies.getSchemaProvider()).setOverrideOffset(query.getOffset())
				.setOverrideLimit(query.getLimit()).setSelectedFacets(query.getSelectedFacets())
				.setSortList(query.getSort()).setAdditionalFilters(query.getAdditionalFilters()).build();

		sqlQuery = new SqlQueryBuilder(combined.getCombinedSql(), dependencies).maxBytesPerPage(maxBytesPerPage)
				.includeEntityEtag(query.getIncludeEntityEtag()).build();
	}

	public SqlQuery getSqlQuery() {
		return sqlQuery;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String startingSql;
		private TranslationDependencies dependencies;
		private Query query;
		private Long maxBytesPerPage;

		/**
		 * @param startingSql the startingSql to set
		 */
		public Builder setStartingSql(String startingSql) {
			this.startingSql = startingSql;
			return this;
		}

		
		/**
		 * @param dependencies the dependencies to set
		 */
		public Builder setDependencies(TranslationDependencies dependencies) {
			this.dependencies = dependencies;
			return this;
		}


		/**
		 * @param query the query to set
		 */
		public Builder setQuery(Query query) {
			this.query = query;
			return this;
		}

		/**
		 * @param maxBytesPerPage the maxBytesPerPage to set
		 */
		public Builder setMaxBytesPerPage(Long maxBytesPerPage) {
			this.maxBytesPerPage = maxBytesPerPage;
			return this;
		}

		public MainQuery build() {
			return new MainQuery(startingSql, dependencies, query, maxBytesPerPage);
		}
	}

}
