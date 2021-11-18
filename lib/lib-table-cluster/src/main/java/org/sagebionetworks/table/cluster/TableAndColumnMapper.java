package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class TableAndColumnMapper {

	private final List<TableInfo> tables;

	public TableAndColumnMapper(QuerySpecification query, SchemaProvider schemaProvider) {
		ValidateArgument.required(query, "QuerySpecification");
		ValidateArgument.required(schemaProvider, "SchemaProvider");
		List<TableInfo> tables = new ArrayList<TableInfo>();
		// extract all of the table information from the SQL model.
		for (TableNameCorrelation table : query.createIterable(TableNameCorrelation.class)) {
			IdAndVersion id = IdAndVersion.parse(table.getTableName().toSql());
			List<ColumnModel> schema = schemaProvider.getTableSchema(id);
			if (schema.isEmpty()) {
				throw new IllegalArgumentException(String.format("Schema for %s is empty.", id));
			}
			TableInfo tableInfo = new TableInfo(table, schema);
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
	
	public ColumnTranslationReference lookupColumnReference(ColumnReference columnReference) {
		
		
		return null;
	}
}
