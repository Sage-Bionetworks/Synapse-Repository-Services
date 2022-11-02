package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;

public class SqlQueryBuilder {

	private QueryExpression model;
	private SchemaProvider schemaProvider;
	private Long overrideOffset;
	private Long overrideLimit;
	private Long maxBytesPerPage;
	private List<SortItem> sortList;
	private Boolean includeEntityEtag;
	private List<FacetColumnRequest> selectedFacets;
	private List<QueryFilter> additionalFilters;
	private Long userId;
	private IndexDescription indexDescription;
	private SqlContext sqlContext;
	
	/**
	 * Start with the SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlQueryBuilder(String sql, Long userId) throws ParseException{
		model = new TableQueryParser(sql).queryExpression();
		this.userId = userId;
	}
	
	public SqlQueryBuilder(String sql, SchemaProvider schemaProvider, Long userId) throws ParseException{
		this.model = new TableQueryParser(sql).queryExpression();
		this.schemaProvider = schemaProvider;
		this.userId = userId;
	}
	
	public SqlQueryBuilder(QueryExpression model){
		this.model = model;
	}
	
	public SqlQueryBuilder(QuerySpecification spec, Long userId){
		this.model = SQLUtils.createQueryExpression(spec);
		this.userId = userId;
	}
	
	public SqlQueryBuilder(QuerySpecification spec,
			SchemaProvider schemaProvider, Long overideOffset,
			Long overideLimit, Long maxBytesPerPage, Long userId) {
		this.model = SQLUtils.createQueryExpression(spec);
		this.schemaProvider = schemaProvider;
		this.overrideOffset = overideOffset;
		this.overrideLimit = overideLimit;
		this.maxBytesPerPage = maxBytesPerPage;
		this.userId = userId;
	}

	public SqlQueryBuilder schemaProvider(SchemaProvider schemaProvider) {
		this.schemaProvider = schemaProvider;
		return this;
	}
	
	public SqlQueryBuilder overrideOffset(Long overrideOffset) {
		this.overrideOffset = overrideOffset;
		return this;
	}
	
	public SqlQueryBuilder overrideLimit(Long overrideLimit) {
		this.overrideLimit = overrideLimit;
		return this;
	}
	
	public SqlQueryBuilder maxBytesPerPage(Long maxBytesPerPage) {
		this.maxBytesPerPage = maxBytesPerPage;
		return this;
	}
	
	public SqlQueryBuilder sortList(List<SortItem> sortList) {
		this.sortList = sortList;
		return this;
	}
	
	public SqlQueryBuilder includeEntityEtag(Boolean includeEntityEtag) {
		this.includeEntityEtag = includeEntityEtag;
		return this;
	}
	
	public SqlQueryBuilder selectedFacets(List<FacetColumnRequest> selectedFacets) {
		this.selectedFacets = selectedFacets;
		return this;
	}

	public SqlQueryBuilder additionalFilters(List<QueryFilter> filters){
		this.additionalFilters = filters;
		return this;
	}
	
	public SqlQueryBuilder sqlContext(SqlContext sqlContext) {
		this.sqlContext = sqlContext;
		return this;
	}
	
	/**
	 * 
	 * @param indexDescription
	 * @return
	 */
	public SqlQueryBuilder indexDescription(IndexDescription indexDescription) {
		this.indexDescription = indexDescription;
		return this;
	}

	public SqlQuery build(){
		return new SqlQuery(model, schemaProvider, overrideOffset, overrideLimit, maxBytesPerPage, sortList, includeEntityEtag,
				selectedFacets, additionalFilters, userId, indexDescription, sqlContext);
	}


}
