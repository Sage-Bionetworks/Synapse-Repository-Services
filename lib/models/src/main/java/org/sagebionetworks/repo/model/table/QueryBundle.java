package org.sagebionetworks.repo.model.table;

public class QueryBundle {

	Query query;
	boolean runQuery;
	boolean runCount;
	boolean returnFacets;
	boolean runSumFileSizes;
	
	public Query getQuery() {
		return query;
	}
	public boolean isRunQuery() {
		return runQuery;
	}
	public boolean isRunCount() {
		return runCount;
	}
	public boolean isReturnFacets() {
		return returnFacets;
	}
	public boolean isRunSumFileSizes() {
		return runSumFileSizes;
	}
	
	public QueryBundle withQuery(Query query) {
		this.query = query;
		return this;
	}
	public QueryBundle withRunQuery(boolean runQuery) {
		this.runQuery = runQuery;
		return this;
	}
	public QueryBundle withRunCount(boolean runCount) {
		this.runCount = runCount;
		return this;
	}
	public QueryBundle withReturnFacets(boolean returnFacets) {
		this.returnFacets = returnFacets;
		return this;
	}
	public QueryBundle withRunSumFileSizes(boolean runSumFileSizes) {
		this.runSumFileSizes = runSumFileSizes;
		return this;
	}
	
	public QueryBundle withMask(Long partMaskIn) {
		final long partMask = partMaskIn != null? partMaskIn : -1L;// default all
		this.runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		boolean runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		boolean returnFacets = ((partMask & BUNDLE_MASK_QUERY_FACETS) != 0);
	}
	
	
}
