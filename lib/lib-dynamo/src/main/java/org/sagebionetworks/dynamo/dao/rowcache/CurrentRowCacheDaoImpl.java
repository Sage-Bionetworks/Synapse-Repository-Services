package org.sagebionetworks.dynamo.dao.rowcache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.collections.Transform.TransformEntry;
import org.sagebionetworks.dynamo.config.DynamoConfig;
import org.sagebionetworks.dynamo.dao.DynamoDaoBaseImpl;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.datamodeling.KeyPair;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CurrentRowCacheDaoImpl extends DynamoDaoBaseImpl implements CurrentRowCacheDao, InitializingBean {
	private static Logger log = LogManager.getLogger(CurrentRowCacheDaoImpl.class);

	private static final int THROUGHPUT_CHECK_INTERVAL_MS = 1000 * 60 * 20; // every twenty minutes

	private static final int MAX_RETRIES = 5;
	private static final long BACK_OFF_MS = 10 * 1000; // 5 seconds per retry squared
	private static final long SLEEP_BETWEEN_BATCHES = 1000;
	
	private final DynamoDBMapper mapper;
	private final DynamoDBMapper statusMapper;

	private final Random random = new Random();

	@Autowired
	private Clock clock;

	private Long writeThroughPut = 0L;
	private Long lastCheckForThroughputChange = 0L;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		tryUpdateThroughput();
	}

	private DboCurrentRowCache dboCreate(Long tableId, Long rowId, Long versionNumber) {
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
	public CurrentRowCacheStatus getLatestCurrentVersionNumber(Long tableId) {
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
	public void putCurrentVersion(Long tableId, Long rowId, Long versionNumber) {
		mapper.save(dboCreate(tableId, rowId, versionNumber));
	}

	@Override
	public void putCurrentVersions(final Long tableId, Map<Long, Long> rowsAndVersions, ProgressCallback<Long> progressCallback) {
		tryUpdateThroughput();

		List<DboCurrentRowCache> toUpdate = Transform.toList(rowsAndVersions.entrySet(),
				new Function<Map.Entry<Long, Long>, DboCurrentRowCache>() {
					@Override
					public DboCurrentRowCache apply(Map.Entry<Long, Long> entry) {
						return dboCreate(tableId, entry.getKey(), entry.getValue());
					}
				});
		// we are seeing problems with using batch save directly. There is no backoff between the (hardcoded) 25 rows
		// updates. We add a simple backoff at this level.
		// in this case, one batch of 25 is almost 3K of data, and throughput is calculated in 1K blocks
		long timeBetweenRequests = 1000 / writeThroughPut * 3 + 5;

		long count = 0;
		int batchingSleepCount = 0;
		for (List<DboCurrentRowCache> batch : Lists.partition(toUpdate, 25)) {
			clock.sleepNoInterrupt(timeBetweenRequests);
			int retries = 0;
			for (;;) {
				try {
					mapper.batchSave(batch);
					if (batchingSleepCount++ % 10 == 9) {
						// needed to give DynamoDB a chance to lower the throttle
						clock.sleepNoInterrupt(SLEEP_BETWEEN_BATCHES);
					}
					count += batch.size();
					if (progressCallback != null) {
						progressCallback.progressMade(count);
					}
					break;
				} catch (ProvisionedThroughputExceededException e) {
					if (retries > MAX_RETRIES) {
						throw e;
					}
					retries++;
					long backoff = BACK_OFF_MS * retries * retries;
					backoff = backoff + random.nextInt((int) backoff / 2);
					clock.sleepWithFrequentCallback(backoff, 30000, progressCallback);
				}
			}
		}
	}

	@Override
	public Long getCurrentVersion(Long tableId, Long rowId) {
		DboCurrentRowCache currentRow = mapper.load(DboCurrentRowCache.class, DboCurrentRowCache.createHashKey(tableId),
				DboCurrentRowCache.createRangeKey(rowId));
		if (currentRow != null) {
			return currentRow.getVersion();
		} else {
			return null;
		}
	}

	@Override
	public Map<Long, Long> getCurrentVersions(Long tableId, Iterable<Long> rowIds) {
		final SortedSet<Long> rowIdSet = Sets.newTreeSet(rowIds);
		if (rowIdSet.isEmpty()) {
			return Maps.newHashMap();
		}

		final String hashKey = DboCurrentRowCache.createHashKey(tableId);

		Iterable<DboCurrentRowCache> results;
		if (useQueryInsteadOfBatchLoad(rowIdSet)) {
			AttributeValue hashKeyValue = new AttributeValue(hashKey);

			// limit the query a bit by using the range of rowIds
			AttributeValue min = new AttributeValue().withN(rowIdSet.first().toString());
			AttributeValue max = new AttributeValue().withN(rowIdSet.last().toString());
			Condition condition = new Condition().withComparisonOperator(ComparisonOperator.BETWEEN).withAttributeValueList(min, max);

			PaginatedQueryList<DboCurrentRowCache> queryResults = mapper.query(DboCurrentRowCache.class, new DynamoDBQueryExpression(
					hashKeyValue).withRangeKeyCondition(condition));
			Iterable<DboCurrentRowCache> filteredResults = Iterables.filter(queryResults, new Predicate<DboCurrentRowCache>() {
				@Override
				public boolean apply(DboCurrentRowCache input) {
					return rowIdSet.contains(input.getRowId());
				}
			});
			results = filteredResults;
		} else {
			Map<String, List<Object>> batchLoad = mapper.batchLoad(Collections.<Class<?>, List<KeyPair>> singletonMap(DboCurrentRowCache.class,
					Transform.toList(rowIdSet, new Function<Long, KeyPair>() {
						@Override
						public KeyPair apply(Long rowId) {
							return new KeyPair().withHashKey(hashKey).withRangeKey(DboCurrentRowCache.createRangeKey(rowId));
						}
					})));
			results = Transform.castElements(Iterables.getOnlyElement(batchLoad.values()));
		}
		return Transform.toMap(results, new Function<DboCurrentRowCache, TransformEntry<Long, Long>>() {
			@Override
			public TransformEntry<Long, Long> apply(DboCurrentRowCache input) {
				return new TransformEntry<Long, Long>(input.getRowId(), input.getVersion());
			}
		});
	}

	@Override
	public Map<Long, Long> getCurrentVersions(Long tableId, final long rowIdOffset, long limit) {

		final String hashKey = DboCurrentRowCache.createHashKey(tableId);

		AttributeValue hashKeyValue = new AttributeValue(hashKey);

		if (limit < 1) {
			throw new IllegalArgumentException("limit should be >= 1");
		}

		// limit the query to the range of rowIds
		// DynamoDB between operator is inclusive, so make sure we handle that correctly
		AttributeValue min = new AttributeValue().withN(Long.toString(rowIdOffset));
		AttributeValue max = new AttributeValue().withN(Long.toString(rowIdOffset + limit - 1L));
		Condition condition = new Condition().withComparisonOperator(ComparisonOperator.BETWEEN).withAttributeValueList(min, max);

		PaginatedQueryList<DboCurrentRowCache> queryResults = mapper.query(DboCurrentRowCache.class,
				new DynamoDBQueryExpression(hashKeyValue).withRangeKeyCondition(condition));
		return Transform.toMap(queryResults, new Function<DboCurrentRowCache, TransformEntry<Long, Long>>() {
			@Override
			public TransformEntry<Long, Long> apply(DboCurrentRowCache input) {
				return new TransformEntry<Long, Long>(input.getRowId(), input.getVersion());
			}
		});
	}

	// should we use a range query (we get records we don't care about, but we add up all sizes for total result and
	// then round up to 4K) or a batch get (we only get the records we care about, but each record is separately
	// rounded up to 4K)
	private boolean useQueryInsteadOfBatchLoad(final SortedSet<Long> rowIdSet) {
		return (rowIdSet.last().longValue() - rowIdSet.first().longValue()) * DboCurrentRowCache.AVERAGE_RECORD_SIZE < rowIdSet.size() * 4096;
	}

	@Override
	public void deleteCurrentVersion(Long tableId, Long rowId) {
		mapper.delete(dboCreate(tableId, rowId, null));
	}

	@Override
	public void deleteCurrentVersions(final Long tableId, Iterable<Long> rowIds) {
		List<DboCurrentRowCache> toDelete = Transform.toList(rowIds, new Function<Long, DboCurrentRowCache>() {
			@Override
			public DboCurrentRowCache apply(Long rowId) {
				return dboCreate(tableId, rowId, null);
			}
		});
		mapper.batchDelete(toDelete);
	}

	@Override
	public void deleteCurrentTable(final Long tableId) {
		AttributeValue hashKeyValue = new AttributeValue(DboCurrentRowCache.createHashKey(tableId));
		PaginatedQueryList<DboCurrentRowCache> results = mapper.query(DboCurrentRowCache.class, new DynamoDBQueryExpression(hashKeyValue));
		mapper.batchDelete(uniqueify(results, CURRENT_ROW_CACHE_COMPARATOR));
	}

	@Override
	public void truncateAllData() {
		// scans are eventually consistent, so retry a few times for 3 seconds
		for (long start = clock.currentTimeMillis(); start + 3000 > clock.currentTimeMillis(); clock.sleepNoInterrupt(500)) {
			DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
			PaginatedScanList<DboCurrentRowCache> scanResult = mapper.scan(DboCurrentRowCache.class, scanExpression);
			mapper.batchDelete(uniqueify(scanResult, CURRENT_ROW_CACHE_COMPARATOR));
			PaginatedScanList<DboCurrentRowCacheStatus> scanResult2 = statusMapper.scan(DboCurrentRowCacheStatus.class, scanExpression);
			statusMapper.batchDelete(uniqueify(scanResult2, CURRENT_ROW_CACHE_STATUS_COMPARATOR));
		}
	}

	private void tryUpdateThroughput() {
		long writeThroughPutCopy = writeThroughPut;
		if (writeThroughPutCopy != 0 && clock.currentTimeMillis() < lastCheckForThroughputChange + THROUGHPUT_CHECK_INTERVAL_MS) {
			return;
		}
		try {
			DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(DynamoConfig
					.getDynamoDBMapperConfigFor(DboCurrentRowCache.class).getTableNameOverride().getTableName());
			DescribeTableResult describeTable = getDynamoClient().describeTable(describeTableRequest);
			writeThroughPutCopy = describeTable.getTable().getProvisionedThroughput().getWriteCapacityUnits().longValue();
		} catch (Throwable t) {
			log.error("Could not get throughput: " + t.getMessage(), t);
		}
		// change to some default if we failed to get a good number
		if (writeThroughPutCopy == 0) {
			writeThroughPutCopy = 75L;
		}
		writeThroughPut = writeThroughPutCopy;
		lastCheckForThroughputChange = clock.currentTimeMillis();
	}
}
