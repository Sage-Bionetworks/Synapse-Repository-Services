package org.sagebionetworks.repo.model.table;

public class QueryOptions {

	public static final long BUNDLE_MASK_QUERY_RESULTS = 0x1;
	public static final long BUNDLE_MASK_QUERY_COUNT = 0x2;
	public static final long BUNDLE_MASK_QUERY_SELECT_COLUMNS = 0x4;
	public static final long BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE = 0x8;
	public static final long BUNDLE_MASK_QUERY_COLUMN_MODELS = 0x10;
	public static final long BUNDLE_MASK_QUERY_FACETS = 0x20;
	public static final long BUNDLE_MASK_SUM_FILE_SIZES = 0x40;
	public static final long BUNDLE_MASK_LAST_UPDATED_ON = 0x80;
	public static final long BUNDLE_MASK_COMBINED_SQL = 0x100;

	boolean runQuery;
	boolean runCount;
	boolean returnSelectColumns;
	boolean returnMaxRowsPerPage;
	boolean returnColumnModels;
	boolean returnFacets;
	boolean runSumFileSizes;
	boolean returnLastUpdatedOn;
	boolean returnCombinedSql;
	
	public QueryOptions() {
		// all default to false
		this.runQuery = false;
		this.runCount = false;
		this.returnSelectColumns = false;
		this.returnMaxRowsPerPage = false;
		this.returnColumnModels = false;
		this.returnFacets = false;
		this.runSumFileSizes = false;
		this.returnLastUpdatedOn = false;
		this.returnCombinedSql = false;
	}

	public boolean runQuery() {
		return runQuery;
	}

	public boolean runCount() {
		return runCount;
	}

	public boolean returnFacets() {
		return returnFacets;
	}

	public boolean runSumFileSizes() {
		return runSumFileSizes;
	}

	public boolean returnSelectColumns() {
		return returnSelectColumns;
	}

	public boolean returnMaxRowsPerPage() {
		return returnMaxRowsPerPage;
	}

	public boolean returnColumnModels() {
		return returnColumnModels;
	}
	
	public boolean returnLastUpdatedOn() {
		return this.returnLastUpdatedOn;
	}

	public boolean returnCombinedSql() {
		return this.returnCombinedSql;
	}

	public QueryOptions withRunQuery(boolean runQuery) {
		this.runQuery = runQuery;
		return this;
	}

	public QueryOptions withRunCount(boolean runCount) {
		this.runCount = runCount;
		return this;
	}

	public QueryOptions withReturnFacets(boolean returnFacets) {
		this.returnFacets = returnFacets;
		return this;
	}

	public QueryOptions withRunSumFileSizes(boolean runSumFileSizes) {
		this.runSumFileSizes = runSumFileSizes;
		return this;
	}

	public QueryOptions withReturnSelectColumns(boolean returnSelectColumns) {
		this.returnSelectColumns = returnSelectColumns;
		return this;
	}
	
	public QueryOptions withReturnColumnModels(boolean returnColumnModels) {
		this.returnColumnModels = returnColumnModels;
		return this;
	}
	
	public QueryOptions withReturnMaxRowsPerPage(boolean returnMaxRowsPerPage) {
		this.returnMaxRowsPerPage = returnMaxRowsPerPage;
		return this;
	}
	
	public QueryOptions withReturnLastUpdatedOn(boolean returnLastUpdatedOn) {
		this.returnLastUpdatedOn = returnLastUpdatedOn;
		return this;
	}

	public QueryOptions withReturnCombinedSql(boolean returnCombinedSql) {
		this.returnCombinedSql = returnCombinedSql;
		return this;
	}

	public QueryOptions withMask(Long partMaskIn) {
		final long partMask = partMaskIn != null ? partMaskIn : -1L;// default all.
		this.runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		this.runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		this.returnSelectColumns = ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) != 0);
		this.returnMaxRowsPerPage = ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) != 0);
		this.returnColumnModels = ((partMask & BUNDLE_MASK_QUERY_COLUMN_MODELS) != 0);
		this.returnFacets = ((partMask & BUNDLE_MASK_QUERY_FACETS) != 0);
		this.runSumFileSizes = ((partMask & BUNDLE_MASK_SUM_FILE_SIZES) != 0);
		this.returnLastUpdatedOn = ((partMask & BUNDLE_MASK_LAST_UPDATED_ON) != 0);
		this.returnCombinedSql = ((partMask & BUNDLE_MASK_COMBINED_SQL) !=0 );
		return this;
	}
	
	/**
	 * Get the mask for this current options.
	 * 
	 * @return
	 */
	public long getPartMask() {
		long partMask = 0;
		if(this.runQuery) {
			partMask = partMask | BUNDLE_MASK_QUERY_RESULTS; 
		}
		if(this.runCount) {
			partMask = partMask | BUNDLE_MASK_QUERY_COUNT;
		}
		if(this.returnSelectColumns) {
			partMask = partMask | BUNDLE_MASK_QUERY_SELECT_COLUMNS;
		}
		if(this.returnMaxRowsPerPage) {
			partMask = partMask | BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE;
		}
		if(this.returnColumnModels) {
			partMask = partMask | BUNDLE_MASK_QUERY_COLUMN_MODELS;
		}
		if(this.returnFacets) {
			partMask = partMask | BUNDLE_MASK_QUERY_FACETS;
		}
		if(this.runSumFileSizes) {
			partMask = partMask | BUNDLE_MASK_SUM_FILE_SIZES;
		}
		if(this.returnLastUpdatedOn) {
			partMask = partMask | BUNDLE_MASK_LAST_UPDATED_ON;
		}
		if(this.returnCombinedSql) {
			partMask = partMask | BUNDLE_MASK_COMBINED_SQL;
		}
		return partMask;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (returnColumnModels ? 1231 : 1237);
		result = prime * result + (returnFacets ? 1231 : 1237);
		result = prime * result + (returnLastUpdatedOn ? 1231 : 1237);
		result = prime * result + (returnMaxRowsPerPage ? 1231 : 1237);
		result = prime * result + (returnSelectColumns ? 1231 : 1237);
		result = prime * result + (runCount ? 1231 : 1237);
		result = prime * result + (runQuery ? 1231 : 1237);
		result = prime * result + (runSumFileSizes ? 1231 : 1237);
		result = prime * result + (returnCombinedSql ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryOptions other = (QueryOptions) obj;
		if (returnColumnModels != other.returnColumnModels)
			return false;
		if (returnFacets != other.returnFacets)
			return false;
		if (returnLastUpdatedOn != other.returnLastUpdatedOn)
			return false;
		if (returnMaxRowsPerPage != other.returnMaxRowsPerPage)
			return false;
		if (returnSelectColumns != other.returnSelectColumns)
			return false;
		if (runCount != other.runCount)
			return false;
		if (runQuery != other.runQuery)
			return false;
		if (runSumFileSizes != other.runSumFileSizes)
			return false;
		if(returnCombinedSql != other.returnCombinedSql)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueryOptions [runQuery=" + runQuery + ", runCount=" + runCount + ", returnSelectColumns="
				+ returnSelectColumns + ", returnMaxRowsPerPage=" + returnMaxRowsPerPage + ", returnColumnModels="
				+ returnColumnModels + ", returnFacets=" + returnFacets + ", runSumFileSizes=" + runSumFileSizes
				+ ", returnLastUpdatedOn=" + returnLastUpdatedOn + ", returnCombinedSql=" + returnCombinedSql +"]";
	}

}
