package org.sagebionetworks.table.query.model;


/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement {

	public interface ColumnConvertor {
		void convertTableName(String tableName, StringBuilder builder);

		void convertColumn(ColumnReference columnReference, StringBuilder builder);

		void setLHSColumn(ColumnReference columnReferenceLHS);

		void addAsColumn(ColumnName columnName);

		void convertParam(Number param, StringBuilder builder);

		void convertNumberParam(String param, StringBuilder builder);

		void convertParam(String signedNumericLiteral, StringBuilder builder);
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
