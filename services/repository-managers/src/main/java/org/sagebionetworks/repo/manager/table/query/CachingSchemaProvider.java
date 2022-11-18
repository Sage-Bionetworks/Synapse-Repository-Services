package org.sagebionetworks.repo.manager.table.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.SchemaProvider;

/**
 * A simple in-memory cache implementation of {@link SchemaProvider}.
 *
 */
public class CachingSchemaProvider implements SchemaProvider {

	private final SchemaProvider wrapped;
	private final Map<IdAndVersion, List<ColumnModel>> cache;

	public CachingSchemaProvider(SchemaProvider toWrap) {
		this.wrapped = toWrap;
		cache = new HashMap<>(1);
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		return cache.computeIfAbsent(tableId, (key) -> {
			return wrapped.getTableSchema(key);
		});
	}

}
