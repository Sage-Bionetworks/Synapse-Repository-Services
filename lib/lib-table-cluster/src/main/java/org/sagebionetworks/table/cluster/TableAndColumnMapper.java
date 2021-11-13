package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Maps the relationship between each table and its columns.
 *
 */
public class TableAndColumnMapper {
	
	private final List<IdAndVersion> tableIds;
	private final Map<IdAndVersion, List<ColumnModel>> tableToSchemaMap;

	public TableAndColumnMapper(QuerySpecification query, SchemaProvider schemaProvider) {
		ValidateArgument.required(query, "QuerySpecification");
		ValidateArgument.required(schemaProvider, "SchemaProvider");
		tableIds = new ArrayList<IdAndVersion>();
		tableToSchemaMap = new LinkedHashMap<IdAndVersion, List<ColumnModel>>();
		for(TableNameCorrelation table: query.createIterable(TableNameCorrelation.class)){
			IdAndVersion tableId = IdAndVersion.parse(table.getTableName().toSql());
			tableIds.add(tableId);
			List<ColumnModel> schema = schemaProvider.getTableSchema(tableId);
			if(schema.isEmpty()) {
				throw new IllegalArgumentException(String.format("Schema for %s is empty.", tableId));
			}
			tableToSchemaMap.put(tableId, schema);
		}
	}
	
	/**
	 * Get all of the table IDs referenced by this query.
	 * @return
	 */
	public List<IdAndVersion> getTableIds(){
		return Collections.unmodifiableList(tableIds);
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
}
