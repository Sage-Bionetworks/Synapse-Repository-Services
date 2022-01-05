package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class SqlQueryBuilder {

	private QuerySpecification model;
	private SchemaProvider schemaProvider;
	private Long overrideOffset;
	private Long overrideLimit;
	private Long maxBytesPerPage;
	private List<SortItem> sortList;
	private Boolean includeEntityEtag;
	private EntityType tableType;
	private List<FacetColumnRequest> selectedFacets;
	private List<QueryFilter> additionalFilters;
	private Long userId;
	// Joins are now allowed by default.
	private boolean allowJoins = false;
	
	/**
	 * Start with the SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlQueryBuilder(String sql, Long userId) throws ParseException{
		model = new TableQueryParser(sql).querySpecification();
		this.userId = userId;
	}
	
	public SqlQueryBuilder(String sql, SchemaProvider schemaProvider, Long userId) throws ParseException{
		this.model = new TableQueryParser(sql).querySpecification();
		this.schemaProvider = schemaProvider;
		this.userId = userId;
	}
	
	public SqlQueryBuilder(QuerySpecification model){
		this.model = model;
	}
	
	public SqlQueryBuilder(QuerySpecification model, Long userId){
		this.model = model;
		this.userId = userId;
	}
	
	public SqlQueryBuilder(QuerySpecification model,
			SchemaProvider schemaProvider, Long overideOffset,
			Long overideLimit, Long maxBytesPerPage, Long userId) {
		this.model = model;
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
	
	public SqlQueryBuilder tableType(EntityType tableType) {
		this.tableType = tableType;
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
	
	/**
	 * AllowJoins is false by default. If allowJions is false and the SQL contains a
	 * JOIN, then an IllegalArgumentException will be thrown.
	 * 
	 * @param allowJoins
	 * @return
	 */
	public SqlQueryBuilder allowJoins(boolean allowJoins) {
		this.allowJoins = allowJoins;
		return this;
	}

	public SqlQuery build(){
		return new SqlQuery(model, schemaProvider, overrideOffset, overrideLimit, maxBytesPerPage, sortList,
				includeEntityEtag, tableType, selectedFacets, additionalFilters, userId, allowJoins);
	}


}
