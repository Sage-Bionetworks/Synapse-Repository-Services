package org.sagebionetworks.table.query.model;

/**
 * Immutable parameters to guide writing elements to SQL.
 * 
 */
public class ToSqlParameters {

	private boolean includeQuotes;
	
	public ToSqlParameters(boolean includeQuotes) {
		super();
		this.includeQuotes = includeQuotes;
	}

	/**
	 * Should SQL element be included in quotes?
	 * @return
	 */
	public boolean includeQuotes(){
		return includeQuotes;
	}
}
