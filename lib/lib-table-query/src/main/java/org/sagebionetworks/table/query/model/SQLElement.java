package org.sagebionetworks.table.query.model;


/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public interface SQLElement {
	
	
	/**
	 * Write this element as SQL to the passed StringBuilder.
	 * 
	 * @param builder
	 */
	public void toSQL(StringBuilder builder);

}
