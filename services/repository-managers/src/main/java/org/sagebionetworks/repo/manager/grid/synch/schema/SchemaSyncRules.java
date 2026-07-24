package org.sagebionetworks.repo.manager.grid.synch.schema;

import org.sagebionetworks.repo.manager.grid.synch.core.SyncRules;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

import com.google.common.base.Objects;

/**
 * Keying and matching rules for Phase 1 schema synchronization. Columns are keyed
 * and matched by name; the source-specific reconciliation (which grid columns to
 * preserve, which source-column absences count as user deletions) is delegated to
 * the {@link SourceHandler}.
 */
public class SchemaSyncRules implements SyncRules<ColumnCopyItem, ColumnSourceItem> {

	private final SourceHandler handler;

	public SchemaSyncRules(SourceHandler handler) {
		this.handler = handler;
	}

	@Override
	public String getKey(ColumnCopyItem item) {
		return item.getColumnName();
	}

	@Override
	public boolean matches(ColumnCopyItem copyItem, ColumnSourceItem sourceItem) {
		return Objects.equal(copyItem.getColumnName(), sourceItem.getColumnName());
	}

	@Override
	public boolean isExcludedFromMatching(ColumnCopyItem copyItem, String key) {
		return handler.isColumnExcludedFromMatching(copyItem.getColumnName());
	}

	@Override
	public boolean wasDeletedByUser(ColumnSourceItem sourceItem) {
		return handler.isColumnDeletedByUser(sourceItem.getColumnName());
	}

}
