package org.sagebionetworks.repo.model.query.entity;


public abstract class SqlElement {

	/**
	 * Write the SQL for this element to the passed builder.
	 * 
	 * @param builder
	 */
	public abstract void toSql(StringBuilder builder);
	
	/**
	 * Bind the parameters of this query.
	 * @param parameters
	 */
	public abstract void bindParameters(Parameters parameters);
	
	/**
	 * Write this element to SQL.
	 * @return
	 */
	public String toSql(){
		StringBuilder builder = new StringBuilder();
		toSql(builder);
		return builder.toString();
	}
	
	public String toString(){
		return toSql();
	}
}
