package org.sagebionetworks.table.query.model;


/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement {

	public interface ColumnConvertor {

		enum SQLClause {
			SELECT, ORDER_BY, GROUP_BY
		};

		/**
		 * Convert table name from sql to actual table name in index
		 * 
		 * @param tableName
		 * @param builder
		 */
		void convertTableName(String tableName, StringBuilder builder);

		/**
		 * Convert column name from sql to actual column name in index table
		 * 
		 * @param columnReference
		 * @param builder
		 */
		void convertColumn(ColumnReference columnReference, StringBuilder builder);

		/**
		 * Set the lhs column if valid, so the rhs can know that type to convert to. Always set back to null when lhs
		 * goes out of scope
		 * 
		 * @param columnReferenceLHS
		 */
		void setLHSColumn(ColumnReference columnReferenceLHS);

		/**
		 * New AS column alias encountered. Call this to notify convertor that this new name now exists
		 * 
		 * @param columnName
		 */
		void addAsColumn(ColumnName columnName);

		/**
		 * parameterize a number
		 * 
		 * @param param
		 * @param builder
		 */
		void convertParam(Number param, StringBuilder builder);

		/**
		 * parameterize a param that is known to be a number
		 * 
		 * @param param
		 * @param builder
		 */
		void convertNumberParam(String param, StringBuilder builder);

		/**
		 * parameterize a string and convert if lhs is known
		 * 
		 * @param signedNumericLiteral
		 * @param builder
		 */
		void convertParam(String signedNumericLiteral, StringBuilder builder);

		/**
		 * Indicates that columns are now interpreted as part of specific clause/ Always set back to null when clause
		 * goes out of scope
		 * 
		 * @param currentClause
		 */
		void setCurrentClause(SQLClause clause);
	}

	/**
	 * Write this element as SQL to the passed StringBuilder.
	 * 
	 * @param builder
	 */
	public abstract void toSQL(StringBuilder builder, ColumnConvertor columnConvertor);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toSQL(sb, null);
		return sb.toString();
	}
}
