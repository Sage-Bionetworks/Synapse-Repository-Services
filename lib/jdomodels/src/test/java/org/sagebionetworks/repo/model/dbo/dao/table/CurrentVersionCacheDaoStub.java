package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sagebionetworks.collections.Maps2;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.collections.Transform.TransformEntry;
import org.sagebionetworks.repo.model.dao.table.CurrentVersionCacheDao;
import org.sagebionetworks.util.ProgressCallback;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CurrentVersionCacheDaoStub implements CurrentVersionCacheDao {

	public boolean isEnabled = false;

	public Map<Long, Map<Long, Long>> latestVersionNumbers = Maps2.createSupplierHashMap(new Supplier<Map<Long, Long>>() {
		@Override
		public Map<Long, Long> get() {
			return Maps.newHashMap();
		}

	});

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public long getLatestCurrentVersionNumber(Long tableId) {
		Collection<Long> versions = latestVersionNumbers.get(tableId).values();
		if (versions.isEmpty()) {
			return -1;
		}
		Long max = Collections.max(versions);
		return max;
	}

	@Override
	public void putCurrentVersion(Long tableId, Long rowId, Long versionNumber) {
		latestVersionNumbers.get(tableId).put(rowId, versionNumber);
	}

	@Override
	public void putCurrentVersions(Long tableId, Map<Long, Long> rowsAndVersions, ProgressCallback<Long> progressCallback) {
		latestVersionNumbers.get(tableId).putAll(rowsAndVersions);
	}

	@Override
	public Long getCurrentVersion(Long tableId, Long rowId) {
		return latestVersionNumbers.get(tableId).get(rowId);
	}

	@Override
	public Map<Long, Long> getCurrentVersions(Long tableId, Iterable<Long> rowIds) {
		final Set<Long> rowIdSet = Sets.newHashSet(rowIds);
		return Maps.newHashMap(Maps.filterKeys(latestVersionNumbers.get(tableId), new Predicate<Long>() {
			@Override
			public boolean apply(Long input) {
				return rowIdSet.contains(input);
			}
		}));
	}

	@Override
	public Map<Long, Long> getCurrentVersions(Long tableId, final long rowIdOffset, final long limit) {
		Map<Long, Long> versions = latestVersionNumbers.get(tableId);
		Iterable<Entry<Long, Long>> versionsInRange = Iterables.filter(versions.entrySet(), new Predicate<Entry<Long, Long>>() {
			@Override
			public boolean apply(Entry<Long, Long> input) {
				return input.getKey() >= rowIdOffset && input.getKey() < rowIdOffset + limit;
			}
		});
		return Transform.toMap(versionsInRange, new Function<Map.Entry<Long, Long>, Transform.TransformEntry<Long, Long>>() {
			@Override
			public TransformEntry<Long, Long> apply(Entry<Long, Long> input) {
				return new TransformEntry<Long, Long>(input.getKey(), input.getValue());
			}
		});
	}

	@Override
	public void deleteCurrentVersion(Long tableId, Long rowId) {
		latestVersionNumbers.get(tableId).remove(rowId);
	}

	@Override
	public void deleteCurrentVersions(Long tableId, Iterable<Long> rowIds) {
		for (Long rowId : rowIds) {
			latestVersionNumbers.get(tableId).remove(rowId);
		}
	}

	@Override
	public void deleteCurrentVersionTable(Long tableId) {
		latestVersionNumbers.remove(tableId);
	}

	@Override
	public void truncateAllData() {
		latestVersionNumbers.clear();
	}
}
