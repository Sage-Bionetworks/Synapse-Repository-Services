package org.sagebionetworks.repo.manager.table.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A simple in-memory cache implementation of {@link SchemaProvider}.
 *
 */
public class CachingSchemaProvider implements SchemaProvider {

	private final SchemaProvider wrapped;
	private final Map<IdAndVersion, List<ColumnModel>> cache;
	private final Map<String, ColumnModel> modelcache;

	public CachingSchemaProvider(SchemaProvider toWrap) {
		ValidateArgument.required(toWrap, "toWrap");
		this.wrapped = toWrap;
		cache = new HashMap<>(1);
		modelcache = new HashMap<>(1);
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		return cache.computeIfAbsent(tableId, (key) -> {
			return wrapped.getTableSchema(key);
		});
	}

	@Override
	public ColumnModel getColumnModel(String id) {
		return modelcache.computeIfAbsent(id, (key) -> {
			return wrapped.getColumnModel(id);
		});
	}
}
