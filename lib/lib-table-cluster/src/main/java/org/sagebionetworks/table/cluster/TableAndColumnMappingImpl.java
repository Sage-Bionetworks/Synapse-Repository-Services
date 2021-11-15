package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class TableAndColumnMappingImpl implements TableAndColumnMapping {
	
	private final QuerySpecification queryModel;
	private final SchemaProvider schemaProvider;
	
	public TableAndColumnMappingImpl(QuerySpecification queryModel, SchemaProvider schemaProvider) {
		super();
		this.queryModel = queryModel;
		this.schemaProvider = schemaProvider;
	}

	@Override
	public List<ColumnModel> getUnionOfAllTableSchemas() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
