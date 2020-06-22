package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class SqlQueryBuilder {

	QuerySpecification model;
	List<ColumnModel> tableSchema;
	Long overrideOffset;
	Long overrideLimit;
	Long maxBytesPerPage;
	List<SortItem> sortList;
	Boolean includeEntityEtag;
	Boolean includeRowIdAndRowVersion;
	EntityType tableType;
	List<FacetColumnRequest> selectedFacets;
	List<QueryFilter> additionalFilters;
	
	/**
	 * Start with the SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlQueryBuilder(String sql) throws ParseException{
		model = new TableQueryParser(sql).querySpecification();
	}
	
	public SqlQueryBuilder(String sql, List<ColumnModel> tableSchema) throws ParseException{
		this.model = new TableQueryParser(sql).querySpecification();
		this.tableSchema = tableSchema;
	}
	
	public SqlQueryBuilder(QuerySpecification model){
		this.model = model;
	}
	
	public SqlQueryBuilder(QuerySpecification model,
			List<ColumnModel> tableSchema, Long overideOffset,
			Long overideLimit, Long maxBytesPerPage) {
		this.model = model;
		this.tableSchema = tableSchema;
		this.overrideOffset = overideOffset;
		this.overrideLimit = overideLimit;
		this.maxBytesPerPage = maxBytesPerPage;
	}

	public SqlQueryBuilder tableSchema(List<ColumnModel> tableSchema) {
		this.tableSchema = tableSchema;
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
	
	public SqlQueryBuilder includeRowIdAndRowVersion(Boolean includeRowIdAndRowVersion) {
		this.includeRowIdAndRowVersion = includeRowIdAndRowVersion;
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

	public SqlQuery build(){
		return new SqlQuery(model, tableSchema, overrideOffset, overrideLimit, maxBytesPerPage, sortList,
				includeEntityEtag, tableType, selectedFacets, additionalFilters);
	}


}
