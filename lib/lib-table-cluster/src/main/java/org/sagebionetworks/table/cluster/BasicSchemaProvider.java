package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;

public class BasicSchemaProvider implements SchemaProvider {

	private final List<ColumnModel> schema;
	private final TableType type;

	public BasicSchemaProvider(List<ColumnModel> schema, TableType type) {
		super();
		this.schema = schema;
		this.type = type;
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		return schema;
	}

	@Override
	public ColumnModel getColumnModel(String id) {
		return schema.stream().filter(c -> id.equals(c.getId())).findFirst().get();
	}

	@Override
	public TableType getTableType(IdAndVersion tableId) {
		return type;
	}

}
