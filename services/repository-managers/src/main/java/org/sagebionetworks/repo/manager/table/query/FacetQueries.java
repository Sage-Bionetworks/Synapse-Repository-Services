package org.sagebionetworks.repo.manager.table.query;

import java.util.List;

import org.sagebionetworks.repo.manager.table.FacetModel;
import org.sagebionetworks.repo.manager.table.FacetTransformer;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.util.ValidateArgument;

public class FacetQueries {

	private final List<FacetTransformer> transformers;
	
	public FacetQueries(QueryExpansion expansion) {
		ValidateArgument.required(expansion, "expansion");
		try {

			CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
					.setSchemaProvider(expansion.getSchemaProvider())
					/*
					 * When the FacetModel builds the facet statistic queries for each facet, it
					 * will automatically include a sub-set of the selected facet to each resulting
					 * query. Therefore, we must nod include the selected facets in the sql passed
					 * to the FacetModel.
					 */
					.setSelectedFacets(null).setAdditionalFilters(expansion.getAdditionalFilters()).build();

			TableExpression originalQuery = new TableQueryParser(combined.getCombinedSql()).querySpecification()
					.getTableExpression();
			boolean returnFacets = true;
			TranslationDependencies deps = TranslationDependencies.builder()
					.setIndexDescription(expansion.getIndexDescription())
					.setSchemaProvider(expansion.getSchemaProvider()).setUserId(expansion.getUserId()).build();
			transformers = new FacetModel(expansion.getSelectedFacets(), originalQuery, deps, returnFacets)
					.getFacetInformationQueries();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public List<FacetTransformer> getFacetInformationQueries() {
		return transformers;
	}


}
