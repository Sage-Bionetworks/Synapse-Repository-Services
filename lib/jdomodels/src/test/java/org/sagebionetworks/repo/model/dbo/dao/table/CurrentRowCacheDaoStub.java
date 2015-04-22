package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Map;

import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.table.ColumnMapper;

import com.google.common.collect.Maps;

public class CurrentRowCacheDaoStub implements CurrentRowCacheDao {

	public boolean isEnabled = true;

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public long getLatestCurrentRowVersionNumber(Long tableId) {
		return -1;
	}

	@Override
	public Map<Long, RowAccessor> getCurrentRows(Long tableId, Iterable<Long> rowIds, ColumnMapper mapper) {
		return Maps.newHashMap();
	}
}
