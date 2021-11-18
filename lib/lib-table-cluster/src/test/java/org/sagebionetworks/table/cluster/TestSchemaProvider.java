package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Test helper that implements SchemaProvider using an input map;
 *
 */
public class TestSchemaProvider implements SchemaProvider {
	
	private final Map<IdAndVersion, List<ColumnModel> > map;
	
	public TestSchemaProvider(Map<IdAndVersion, List<ColumnModel>> map) {
		super();
		this.map = map;
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		return map.get(tableId);
	}

}
