package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Immutable mapping for tables and columns from a query..
 *
 */
public class TableAndColumnMapper implements ColumnLookup {

	private final List<TableInfo> tables;

	public TableAndColumnMapper(QuerySpecification query, SchemaProvider schemaProvider) {
		ValidateArgument.required(query, "QuerySpecification");
		ValidateArgument.required(schemaProvider, "SchemaProvider");
		List<TableInfo> tables = new ArrayList<TableInfo>();
		int tableIndex = 0;
		// extract all of the table information from the SQL model.
		for (TableNameCorrelation table : query.createIterable(TableNameCorrelation.class)) {
			IdAndVersion id = IdAndVersion.parse(table.getTableName().toSql());
			List<ColumnModel> schema = schemaProvider.getTableSchema(id);
			if (schema.isEmpty()) {
				throw new IllegalArgumentException(String.format("Schema for %s is empty.", id));
			}
			TableInfo tableInfo = new TableInfo(table, tableIndex++, schema);
			tables.add(tableInfo);
		}
		this.tables = Collections.unmodifiableList(tables);
	}

	/**
	 * Get all of the table IDs referenced by this query.
	 * 
	 * @return
	 */
	public List<IdAndVersion> getTableIds() {
		return tables.stream().map(t -> t.getTableIdAndVersion()).collect(Collectors.toList());
	}
	
	/**
	 * Get the single IdAndVersion for the given query.  If this query includes more than one
	 * table an Optional.empty() will be returned.
	 * @return
	 */
	public Optional<IdAndVersion> getSingleTableId() {
		if(tables.size() == 1) {
			return Optional.of(tables.get(0).getTableIdAndVersion());
		}else {
			return Optional.empty();
		}
		
	}

	/**
	 * Get the union of the schemas for each table referenced in the query.
	 * 
	 * @return
	 */
	public List<ColumnModel> getUnionOfAllTableSchemas() {
		return tables.stream().flatMap(t -> t.getTableSchema().stream()).collect(Collectors.toList());
	}

	/**
	 * Build a full SelectList from all columns from all tables.
	 * 
	 * @return
	 * @throws ParseException
	 */
	public SelectList buildSelectAllColumns() {
		StringJoiner sqlJoiner = new StringJoiner(", ");
		for (TableInfo tableInfo : tables) {
			for (ColumnModel column : tableInfo.getTableSchema()) {
				StringBuilder sql = new StringBuilder();
				if (tableInfo.getTableAlias().isPresent()) {
					sql.append(tableInfo.getTableAlias().get());
					sql.append(".");
				} else if (tables.size() > 1) {
					sql.append(tableInfo.getOriginalTableName());
					sql.append(".");
				}
				sql.append(SqlElementUtils.wrapInDoubleQuotes(column.getName()));
				sqlJoiner.add(sql.toString());
			}
		}
		try {
			return new TableQueryParser(sqlJoiner.toString()).selectList();
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Lookup a ColumnReference using just the column name or alias
	 * @param columnName
	 * @return
	 */
	public Optional<ColumnTranslationReference> lookupColumnReference(String columnName) {
		if(columnName == null) {
			return Optional.empty();
		}
		try {
			return lookupColumnReference(new TableQueryParser(columnName).columnReference());
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Attempt to resolve the given ColumnReference to one of the columns one of the
	 * referenced tables. Optional.empty() returned if no match was found.
	 * 
	 * @param columnReference
	 * @return
	 */
	@Override
	public Optional<ColumnTranslationReference> lookupColumnReference(ColumnReference columnReference) {
		return lookupColumnReferenceMatch(columnReference).map(r -> r.getColumnTranslationReference());
	}

	/**
	 * Attempt to resolve the given ColumnReference to one of the columns one of the
	 * referenced tables. Optional.empty() returned if no match was found.
	 * 
	 * @param columnReference
	 * @return
	 */
	public Optional<ColumnReferenceMatch> lookupColumnReferenceMatch(ColumnReference columnReference) {
		if (columnReference == null) {
			return Optional.empty();
		}
		if (!columnReference.getNameLHS().isPresent() && tables.size() > 1) {
			throw new IllegalArgumentException(
					"Expected a table name or table alias for column: " + columnReference.toSql());
		}
		for (TableInfo table : tables) {
			Optional<ColumnTranslationReference> matchedRef = table.lookupColumnReference(columnReference);
			if (matchedRef.isPresent()) {
				return Optional.of(new ColumnReferenceMatch(table, matchedRef.get()));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Get the number of tables in the original query.
	 * @return
	 */
	public int getNumberOfTables() {
		return this.tables.size();
	}
	
	/**
	 * Lookup the TableInfo that matches the passed TableNameCorrelation. If no match is found
	 * an Optional.empty() will be returned.
	 * @param tableNameCorrelation
	 * @return
	 */
	public Optional<TableInfo> lookupTableNameCorrelation(TableNameCorrelation tableNameCorrelation){
		if(tableNameCorrelation == null) {
			return Optional.empty();
		}
		return tables.stream().filter(t -> t.isMatch(tableNameCorrelation)).findFirst();
	}
}
