package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;

public class SqlQueryBuilder {

	private QuerySpecification model;
	private SchemaProvider schemaProvider;
	private Long maxBytesPerPage;
	private Boolean includeEntityEtag;
	private Long userId;
	private IndexDescription indexDescription;
	private SqlContext sqlContext;
	private Pagination originalPagination;
	private List<FacetColumnRequest> selectedFacets;
	
	/**
	 * Start with the SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlQueryBuilder(String sql, Long userId){
		try {
			model = new TableQueryParser(sql).querySpecification();
			this.userId = userId;
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
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
			SchemaProvider schemaProvider, Long maxBytesPerPage, Long userId) {
		this.model = model;
		this.schemaProvider = schemaProvider;
		this.maxBytesPerPage = maxBytesPerPage;
		this.userId = userId;
	}

	public SqlQueryBuilder schemaProvider(SchemaProvider schemaProvider) {
		this.schemaProvider = schemaProvider;
		return this;
	}
	
	public SqlQueryBuilder maxBytesPerPage(Long maxBytesPerPage) {
		this.maxBytesPerPage = maxBytesPerPage;
		return this;
	}
	
	
	public SqlQueryBuilder includeEntityEtag(Boolean includeEntityEtag) {
		this.includeEntityEtag = includeEntityEtag;
		return this;
	}
	
	
	public SqlQueryBuilder sqlContext(SqlContext sqlContext) {
		this.sqlContext = sqlContext;
		return this;
	}
	
	public SqlQueryBuilder originalPagination(Pagination original) {
		this.originalPagination = original;
		return this;
	}
	
	public SqlQueryBuilder selectedFacets(List<FacetColumnRequest> selectedFacets) {
		this.selectedFacets = selectedFacets;
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

	public SqlQuery build() {
		return new SqlQuery(model, schemaProvider, maxBytesPerPage, includeEntityEtag, selectedFacets, userId, indexDescription,
				sqlContext, originalPagination);
	}


}
