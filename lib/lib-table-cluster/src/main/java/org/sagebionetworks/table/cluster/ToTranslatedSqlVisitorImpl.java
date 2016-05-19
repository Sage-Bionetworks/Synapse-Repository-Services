package org.sagebionetworks.table.cluster;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.BooleanFunction;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.util.TimeUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ToTranslatedSqlVisitorImpl extends ToTranslatedSqlVisitor {
	private final Map<String, Object> parameters;
	private final Map<String, ColumnModel> columnNameToModelMap;

	private ColumnModel columnModelLHS = null;
	private Set<String> asColumns = Sets.newHashSet();
	private String currentTableName = null;
	private LinkedList<SQLClause> currentClauses = Lists.newLinkedList();

	public ToTranslatedSqlVisitorImpl(Map<String, Object> parameters, Map<String, ColumnModel> columnNameToModelMap) {
		this.parameters = parameters;
		this.columnNameToModelMap = columnNameToModelMap;
	}

	/**
	 * Convert table name from sql to actual table name in index
	 * 
	 * @param tableName
	 */
	@Override
	public void convertTableName(String tableName) {
		Long tableId = KeyFactory.stringToKey(tableName);
		currentTableName = SQLUtils.TABLE_PREFIX + tableId;
		this.append(currentTableName);
	}

	/**
	 * Convert column name from sql to actual column name in index table
	 * 
	 * @param columnReference
	 */
	@Override
	public void convertColumn(ColumnReference columnReference) {
		String columnName = columnReference.getNameRHS().getFirstUnquotedValue();
		// Is this a reserved column name like ROW_ID or ROW_VERSION?
		if (TableConstants.isReservedColumnName(columnName)) {
			// use the returned reserve name in destination SQL.
			this.append(columnName.toUpperCase());
		} else {
			// Not a reserved column name.
			// Lookup the ID for this column
			columnName = columnName.trim();
			ColumnModel column = columnNameToModelMap.get(columnName);
			if (column == null) {
				if (asColumns.contains(columnName)) {
					this.append(columnName);
				} else {
					throw new IllegalArgumentException("Unknown column name: " + columnName);
				}
			} else {
				String subName = "";
				if (columnReference.getNameLHS() != null) {
					subName = columnReference.getNameLHS().getFirstUnquotedValue();
					// Remove double quotes if they are included.
					subName = subName.replaceAll("\"", "") + "_";
				}
				switch (column.getColumnType()) {
				case DOUBLE:
					SQLUtils.appendDoubleCase(column.getId(), subName, currentTableName, currentClauses.contains(SQLClause.SELECT),
							!currentClauses.contains(SQLClause.FUNCTION_PARAMETER), getBuilder());
					break;
				default:
					SQLUtils.appendColumnName(subName, column.getId(), getBuilder());
					break;
				}
			}
		}
	}

	/**
	 * Handle a boolean function on the current column
	 * 
	 * @param booleanFunction
	 * @param columnReference
	 */
	@Override
	public void handleFunction(BooleanFunction booleanFunction, ColumnReference columnReference) {
		String columnName = columnReference.getNameRHS().getFirstUnquotedValue();
		// Is this a reserved column name like ROW_ID or ROW_VERSION?
		if (TableConstants.isReservedColumnName(columnName)) {
			throw new IllegalArgumentException("Cannot apply " + booleanFunction + " on reserved column " + columnName);
		}

		// Not a reserved column name.
		// Lookup the ID for this column
		columnName = columnName.trim().toLowerCase();
		ColumnModel column = columnNameToModelMap.get(columnName);
		if (column == null) {
			throw new IllegalArgumentException("You can only apply " + booleanFunction + " on a column directly. " + columnName
					+ " is not a column");
		}

		String subName = "";
		if (columnReference.getNameLHS() != null) {
			subName = columnReference.getNameLHS().getFirstUnquotedValue();
			// Remove double quotes if they are included.
			subName = subName.replaceAll("\"", "") + "_";
		}
		switch (column.getColumnType()) {
		case DOUBLE:
			switch (booleanFunction) {
			case ISNAN:
				SQLUtils.appendIsNan(column.getId(), subName, getBuilder());
				break;
			case ISINFINITY:
				SQLUtils.appendIsInfinity(column.getId(), subName, getBuilder());
				break;
			default:
				throw new IllegalArgumentException("function " + booleanFunction + " not yet supported");
			}
			break;
		default:
			throw new IllegalArgumentException("Cannot apply " + booleanFunction + " on a column of type " + column.getColumnType());
		}
	}

	/**
	 * Set the lhs column if valid, so the rhs can know that type to convert to. Always set back to null when lhs goes
	 * out of scope
	 * 
	 * @param columnReferenceLHS
	 */
	@Override
	public void setLHSColumn(ColumnReference columnReferenceLHS) {
		if (columnReferenceLHS == null) {
			this.columnModelLHS = null;
		} else {
			String columnName = columnReferenceLHS.getNameRHS().getFirstUnquotedValue();
			// Is this a reserved column name like ROW_ID or ROW_VERSION?
			if (TableConstants.isReservedColumnName(columnName)) {
				this.columnModelLHS = null;
			} else {
				// Not a reserved column name.
				// Lookup the ID for this column
				this.columnModelLHS = columnNameToModelMap.get(columnName.trim());
			}
		}
	}

	/**
	 * New AS column alias encountered. Call this to notify converter that this new name now exists
	 * 
	 * @param columnName
	 */
	@Override
	public void addAsColumn(ColumnName columnName) {
		asColumns.add(columnName.getFirstUnquotedValue());
	}

	/**
	 * parameterize a number
	 * 
	 * @param param
	 */
	@Override
	public void convertParam(Number param) {
		String bindKey = "b" + parameters.size();
		this.append(":");
		this.append(bindKey);
		parameters.put(bindKey, param);
	}

	/**
	 * parameterize a param that is known to be a number
	 * 
	 * @param param
	 */
	@Override
	public void convertNumberParam(String param) {
		String bindKey = "b" + parameters.size();
		this.append(":");
		this.append(bindKey);
		parameters.put(bindKey, param);
	}

	/**
	 * parameterize a string and convert if lhs is known
	 * 
	 * @param signedNumericLiteral
	 */
	@Override
	public void convertParam(String value) {
		// don't convert to param if the value is in a select clause
		if (currentClauses.contains(SQLClause.SELECT)) {
			this.append("'");
			this.append(value.replaceAll("'", "''"));
			this.append("'");
		} else {
			if (columnModelLHS != null) {
				switch (columnModelLHS.getColumnType()) {
				case DATE:
					value = Long.toString(TimeUtils.parseSqlDate(value));
					break;
				default:
					break;
				}
			}
			String bindKey = "b" + parameters.size();
			this.append(":");
			this.append(bindKey);
			parameters.put(bindKey, value);
		}
	}

	/**
	 * Indicates that columns are now interpreted as part of specific clause/ Always pop when clause goes out of scope
	 * 
	 * @param currentClause
	 */
	@Override
	public void pushCurrentClause(SQLClause clause) {
		currentClauses.push(clause);
	}

	/**
	 * Indicates that columns are no longer interpreted as part of specific clause. Always match push goes out of scope
	 * 
	 * @param currentClause
	 */
	@Override
	public void popCurrentClause(SQLClause clause) {
		SQLClause lastPushedClause = currentClauses.pop();
		if (lastPushedClause != clause) {
			throw new IllegalStateException("Last pushed clause " + lastPushedClause + " is not the same as the one that was popped "
					+ clause);
		}
	}

}