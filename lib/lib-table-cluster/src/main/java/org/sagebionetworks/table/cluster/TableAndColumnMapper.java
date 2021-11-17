package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
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
	
	private final List<IdAndVersion> tableIds;
	private final Map<IdAndVersion, List<ColumnModel>> tableToSchemaMap;
	private final Map<IdAndVersion, TableInfo> tableInfoMap;

	public TableAndColumnMapper(QuerySpecification query, SchemaProvider schemaProvider) {
		ValidateArgument.required(query, "QuerySpecification");
		ValidateArgument.required(schemaProvider, "SchemaProvider");
		List<IdAndVersion> tableIds = new ArrayList<IdAndVersion>();
		Map<IdAndVersion, List<ColumnModel>> tableToSchemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		Map<IdAndVersion, TableInfo> tableInfoMap = new LinkedHashMap<IdAndVersion, TableInfo>();
		// extract all of the table information from the SQL model.
		for(TableNameCorrelation table: query.createIterable(TableNameCorrelation.class)){
			TableInfo tableInfo = new TableInfo(table);
			IdAndVersion tableId = tableInfo.getTableIdAndVersion();
			tableInfoMap.put(tableId, tableInfo);
			tableIds.add(tableId);
			List<ColumnModel> schema = schemaProvider.getTableSchema(tableId);
			if(schema.isEmpty()) {
				throw new IllegalArgumentException(String.format("Schema for %s is empty.", tableId));
			}
			tableToSchemaMap.put(tableId, schema);
		}
		this.tableIds = Collections.unmodifiableList(tableIds);
		this.tableToSchemaMap = Collections.unmodifiableMap(tableToSchemaMap);
		this.tableInfoMap = Collections.unmodifiableMap(tableInfoMap);
	}
	
	/**
	 * Get all of the table IDs referenced by this query.
	 * @return
	 */
	public List<IdAndVersion> getTableIds(){
		return tableIds;
	}
	
	/**
	 * Get the union of the schemas for each table referenced in the query.
	 * @return
	 */
	public List<ColumnModel> getUnionOfAllTableSchemas(){
		List<ColumnModel> results = new ArrayList<ColumnModel>();
		for(List<ColumnModel> schema: tableToSchemaMap.values()) {
			results.addAll(schema);
		}
		return Collections.unmodifiableList(results);
	}

	/**
	 * Build a full SelectList from all columns from all tables.
	 * @return
	 * @throws ParseException
	 */
	public SelectList buildSelectAllColumns() {
		StringJoiner sqlJoiner = new StringJoiner(", ");
		for(IdAndVersion tableId: tableIds) {
			TableInfo tableInfo = tableInfoMap.get(tableId);
			List<ColumnModel> tableSchema = tableToSchemaMap.get(tableId);
			for(ColumnModel column: tableSchema) {
				StringBuilder sql = new StringBuilder();
				if(tableInfo.getTableAlias().isPresent()) {
					sql.append(tableInfo.getTableAlias().get());
					sql.append(".");
				}else if(tableIds.size() > 1) {
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
}
