package org.sagebionetworks.table.query.util;

/**
 * An immutable sample of SQL with a description.  This is used for query documentation and testing.
 * 
 * @author John
 *
 */
public class SQLExample {
	
	private String category;
	private String description;
	private String sql;
	/**
	 * Create a new Example
	 * @param category The category that this example belongs too.
	 * @param description
	 * @param sql
	 */
	public SQLExample(String category, String description, String sql) {
		super();
		this.category = category;
		this.description = description;
		this.sql = sql;
	}
	/**
	 * The category this example belongs to.
	 * @return
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * The description of this example.
	 * @return
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * The actual SQL example.
	 * @return
	 */
	public String getSql() {
		return sql;
	}
	
}
