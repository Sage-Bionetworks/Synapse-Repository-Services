package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceReader;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

/**
 * Read-only view of the source schema for Phase 1 synchronization. The internal
 * column list is consumed as copy columns are matched via {@link #consume(String)};
 * after Phase 1, {@link #streamRemaining()} yields only the source columns that
 * were never matched to a copy column.
 */
public class SchemaSourceReader implements SourceReader<ColumnSourceItem> {

	private final List<ColumnSourceItem> schema;

	public SchemaSourceReader(SourceHandler handler) {
		this.schema = handler.getCurrentSourceSchema().stream().map(n -> new ColumnSourceItem().setColumnName(n))
				.collect(Collectors.toList());
	}

	@Override
	public Optional<ColumnSourceItem> consume(String key) {
		Optional<ColumnSourceItem> item = schema.stream().filter(i -> i.getColumnName().equals(key)).findFirst();
		item.ifPresent(schema::remove);
		return item;
	}

	@Override
	public Stream<ColumnSourceItem> streamRemaining() {
		return schema.stream();
	}

}
