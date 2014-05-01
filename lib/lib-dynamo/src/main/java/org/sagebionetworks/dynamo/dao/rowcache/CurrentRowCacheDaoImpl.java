package org.sagebionetworks.dynamo.dao.rowcache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.collections.Transform.TransformEntry;
import org.sagebionetworks.dynamo.config.DynamoConfig;
import org.sagebionetworks.dynamo.dao.DynamoDaoBaseImpl;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class CurrentRowCacheDaoImpl extends DynamoDaoBaseImpl implements CurrentRowCacheDao {

	DynamoDBMapper mapper;
	DynamoDBMapper statusMapper;

	private static final Comparator<DboCurrentRowCache> CURRENT_ROW_CACHE_COMPARATOR = new Comparator<DboCurrentRowCache>() {
		@Override
		public int compare(DboCurrentRowCache o1, DboCurrentRowCache o2) {
			return ComparisonChain.start().compare(o1.getHashKey(), o2.getHashKey()).compare(o1.getRangeKey(), o2.getRangeKey()).result();
		}
	};

	private static final Comparator<DboCurrentRowCacheStatus> CURRENT_ROW_CACHE_STATUS_COMPARATOR = new Comparator<DboCurrentRowCacheStatus>() {
		@Override
		public int compare(DboCurrentRowCacheStatus o1, DboCurrentRowCacheStatus o2) {
			return ComparisonChain.start().compare(o1.getHashKey(), o2.getHashKey()).result();
		}
	};

	public CurrentRowCacheDaoImpl(AmazonDynamoDB dynamoClient) {
		super(dynamoClient);
		mapper = new DynamoDBMapper(dynamoClient, DynamoConfig.getDynamoDBMapperConfigFor(DboCurrentRowCache.class));
		statusMapper = new DynamoDBMapper(dynamoClient, DynamoConfig.getDynamoDBMapperConfigFor(DboCurrentRowCacheStatus.class));
	}

	private DboCurrentRowCache dboCreate(String tableId, Long rowId, Long versionNumber) {
		DboCurrentRowCache currentRow = new DboCurrentRowCache();
		currentRow.setHashKey(DboCurrentRowCache.createHashKey(tableId));
		currentRow.setRangeKey(DboCurrentRowCache.createRangeKey(rowId));
		currentRow.setVersion(versionNumber);
		return currentRow;
	}

	@Override
	public boolean isEnabled() {
		return isDynamoEnabled();
	}

	@Override
	public CurrentRowCacheStatus getLatestCurrentVersionNumber(String tableId) {
		DboCurrentRowCacheStatus status = statusMapper.load(DboCurrentRowCacheStatus.class, DboCurrentRowCacheStatus.createHashKey(tableId));
		if (status != null) {
			return new CurrentRowCacheStatus(tableId, status.getLatestVersionNumber(), status.getRecordVersion());
		} else {
			return new CurrentRowCacheStatus(tableId, null, null);
		}
	}

	@Override
	public void setLatestCurrentVersionNumber(CurrentRowCacheStatus oldStatus, Long newLatestVersionNumber)
			throws ConditionalCheckFailedException {
		DboCurrentRowCacheStatus status = new DboCurrentRowCacheStatus();
		status.setHashKey(DboCurrentRowCacheStatus.createHashKey(oldStatus.getTableId()));
		status.setRecordVersion(oldStatus.getRecordVersion());
		status.setLatestVersionNumber(newLatestVersionNumber);
		statusMapper.save(status);
	}

	@Override
	public void putCurrentVersion(String tableId, Long rowId, Long versionNumber) {
		mapper.save(dboCreate(tableId, rowId, versionNumber));
	}

	@Override
	public void putCurrentVersions(final String tableId, Map<Long, Long> rowsAndVersions) {
		List<DboCurrentRowCache> toUpdate = Transform.toList(rowsAndVersions.entrySet(),
				new Function<Map.Entry<Long, Long>, DboCurrentRowCache>() {
					@Override
					public DboCurrentRowCache apply(Map.Entry<Long, Long> entry) {
						return dboCreate(tableId, entry.getKey(), entry.getValue());
					}
				});
		mapper.batchSave(toUpdate);
	}

	@Override
	public Long getCurrentVersion(String tableId, Long rowId) {
		DboCurrentRowCache currentRow = mapper.load(DboCurrentRowCache.class, DboCurrentRowCache.createHashKey(tableId),
				DboCurrentRowCache.createRangeKey(rowId));
		if (currentRow != null) {
			return currentRow.getVersion();
		} else {
			return null;
		}
	}

	@Override
	public Map<Long, Long> getCurrentVersions(String tableId, Iterable<Long> rowIds) {
		final SortedSet<Long> rowIdSet = Sets.newTreeSet(rowIds);
		if (rowIdSet.isEmpty()) {
			return Collections.emptyMap();
		}

		AttributeValue hashKeyValue = new AttributeValue(DboCurrentRowCache.createHashKey(tableId));

		// try limit the query a bit by using the range of rowIds
		AttributeValue min = new AttributeValue().withN(rowIdSet.first().toString());
		AttributeValue max = new AttributeValue().withN(rowIdSet.last().toString());
		Condition condition = new Condition().withComparisonOperator(ComparisonOperator.BETWEEN).withAttributeValueList(min, max);

		PaginatedQueryList<DboCurrentRowCache> results = mapper.query(DboCurrentRowCache.class,
				new DynamoDBQueryExpression(hashKeyValue).withRangeKeyCondition(condition));
		Iterable<DboCurrentRowCache> filteredResults = Iterables.filter(results, new Predicate<DboCurrentRowCache>() {
			@Override
			public boolean apply(DboCurrentRowCache input) {
				return rowIdSet.contains(input.getRowId());
			}
		});
		return Transform.toMap(filteredResults, new Function<DboCurrentRowCache, TransformEntry<Long, Long>>() {
			@Override
			public TransformEntry<Long, Long> apply(DboCurrentRowCache input) {
				return new TransformEntry<Long, Long>(input.getRowId(), input.getVersion());
			}
		});
	}

	@Override
	public void deleteCurrentVersion(String tableId, Long rowId) {
		mapper.delete(dboCreate(tableId, rowId, null));
	}

	@Override
	public void deleteCurrentVersions(final String tableId, Iterable<Long> rowIds) {
		List<DboCurrentRowCache> toDelete = Transform.toList(rowIds, new Function<Long, DboCurrentRowCache>() {
			@Override
			public DboCurrentRowCache apply(Long rowId) {
				return dboCreate(tableId, rowId, null);
			}
		});
		mapper.batchDelete(toDelete);
	}

	@Override
	public void deleteCurrentTable(final String tableId) {
		AttributeValue hashKeyValue = new AttributeValue(DboCurrentRowCache.createHashKey(tableId));
		PaginatedQueryList<DboCurrentRowCache> results = mapper.query(DboCurrentRowCache.class, new DynamoDBQueryExpression(hashKeyValue));
		mapper.batchDelete(uniqueify(results, CURRENT_ROW_CACHE_COMPARATOR));
	}

	@Override
	public void truncateAllData() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		PaginatedScanList<DboCurrentRowCache> scanResult = mapper.scan(DboCurrentRowCache.class, scanExpression);
		mapper.batchDelete(uniqueify(scanResult, CURRENT_ROW_CACHE_COMPARATOR));
		PaginatedScanList<DboCurrentRowCacheStatus> scanResult2 = statusMapper.scan(DboCurrentRowCacheStatus.class, scanExpression);
		statusMapper.batchDelete(uniqueify(scanResult2, CURRENT_ROW_CACHE_STATUS_COMPARATOR));
	}
}
