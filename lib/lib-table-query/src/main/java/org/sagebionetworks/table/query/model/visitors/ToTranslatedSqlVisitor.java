package org.sagebionetworks.table.query.model.visitors;

import org.sagebionetworks.table.query.model.BooleanFunction;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;


public abstract class ToTranslatedSqlVisitor extends ToSimpleSqlVisitor {
	/**
	 * Convert table name from sql to actual table name in index
	 * 
	 * @param tableName
	 * @param builder
	 */
	public abstract void convertTableName(String tableName);

	/**
	 * Convert column name from sql to actual column name in index table
	 * 
	 * @param columnReference
	 * @param builder
	 */
	public abstract void convertColumn(ColumnReference columnReference);

	/**
	 * Handle a boolean function on the current column
	 * 
	 * @param booleanFunction
	 * @param columnReference
	 * @param builder
	 */
	public abstract void handleFunction(BooleanFunction booleanFunction, ColumnReference columnReference);

	/**
	 * Set the lhs column if valid, so the rhs can know that type to convert to. Always set back to null when lhs goes
	 * out of scope
	 * 
	 * @param columnReferenceLHS
	 */
	@Override
	public abstract void setLHSColumn(ColumnReference columnReferenceLHS);

	/**
	 * New AS column alias encountered. Call this to notify convertor that this new name now exists
	 * 
	 * @param columnName
	 */
	public abstract void addAsColumn(ColumnName columnName);

	/**
	 * parameterize a number
	 * 
	 * @param param
	 * @param builder
	 */
	public abstract void convertParam(Number param);

	/**
	 * parameterize a param that is known to be a number
	 * 
	 * @param param
	 * @param builder
	 */
	public abstract void convertNumberParam(String param);

	/**
	 * parameterize a string and convert if lhs is known
	 * 
	 * @param signedNumericLiteral
	 * @param builder
	 */
	public abstract void convertParam(String value);

	/**
	 * Indicates that columns are now interpreted as part of specific clause/ Always pop when clause goes out of scope
	 * 
	 * @param currentClause
	 */
	@Override
	public abstract void pushCurrentClause(SQLClause clause);

	/**
	 * Indicates that columns are no longer interpreted as part of specific clause. Always match push goes out of scope
	 * 
	 * @param currentClause
	 */
	@Override
	public abstract void popCurrentClause(SQLClause clause);
}