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
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Maps the relationship between each table and its columns.
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
				sql.append(SqlElementUntils.wrapInDoubleQuotes(column.getName()));
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
		for(TableInfo table: tables) {
			Optional<ColumnTranslationReference> matchedRef = table.lookupColumnReference(columnReference);
			if(matchedRef.isPresent()) {
				return Optional.of(new ColumnReferenceMatch(table, matchedRef.get()));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Attempt to translate the given ColumnReference by matching it to a column
	 * from one of the tables. The resulting LHS will be the translated table alias
	 * and the RHS will be the translated column name. Optional.empty() returned if
	 * no match was found.
	 * 
	 * @param columnReference
	 * @return
	 */
	public Optional<ColumnReference> trasnalteColumnReference(ColumnReference columnReference) {
		Optional<ColumnReferenceMatch> optional = lookupColumnReferenceMatch(columnReference);
		if(!optional.isPresent()) {
			return Optional.empty();
		}
		ColumnReferenceMatch match = optional.get();
		StringBuilder builder = new StringBuilder();
		if(tables.size() > 1) {
			builder.append(match.getTableInfo().getTranslatedTableAlias());
			builder.append(".");
		}
		builder.append(match.getColumnTranslationReference().getTranslatedColumnName());
		try {
			return Optional.of(new TableQueryParser(builder.toString()).columnReference());
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}

}
