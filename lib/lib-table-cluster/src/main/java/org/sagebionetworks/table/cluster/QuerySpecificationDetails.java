package org.sagebionetworks.table.cluster;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;

public class QuerySpecificationDetails {

	private final QuerySpecification querySpecification;
	private final TableAndColumnMapper tableAndColumnMapper;
	private final List<SelectColumn> selectColumns;
	private final int maxRowSizeBytes;
	private final List<ColumnModel> schemaOfSelect;

	public QuerySpecificationDetails(QuerySpecification querySpecification, SchemaProvider schemaProvider, SqlContext sqlContext) {
		super();
		this.querySpecification = querySpecification;
		this.tableAndColumnMapper = new TableAndColumnMapper(querySpecification, schemaProvider);
		
		if(this.tableAndColumnMapper.getTableIds().size() > 1 && !SqlContext.build.equals(sqlContext)) {
			throw new IllegalArgumentException(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
		}
		
		if(BooleanUtils.isTrue(querySpecification.getSelectList().getAsterisk())){
			querySpecification.getSelectList().replaceElement(tableAndColumnMapper.buildSelectAllColumns());
		}
		
		schemaOfSelect =  SQLTranslatorUtils.getSchemaOfSelect(querySpecification.getSelectList(), tableAndColumnMapper);

		LinkedHashMap<String, ColumnModel> columnNameToModelMap = TableModelUtils
				.createColumnNameToModelMap(tableAndColumnMapper.getUnionOfAllTableSchemas());

		this.selectColumns = SQLTranslatorUtils.getSelectColumns(querySpecification.getSelectList(),
				tableAndColumnMapper, querySpecification.isElementAggregate());
		this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, columnNameToModelMap);

	}

}
