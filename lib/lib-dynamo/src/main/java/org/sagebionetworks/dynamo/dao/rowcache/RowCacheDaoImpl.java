package org.sagebionetworks.dynamo.dao.rowcache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.collections.Transform.TransformEntry;
import org.sagebionetworks.dynamo.Serializer;
import org.sagebionetworks.dynamo.config.DynamoConfig;
import org.sagebionetworks.dynamo.dao.DynamoDaoBaseImpl;
import org.sagebionetworks.repo.model.table.Row;

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
import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class RowCacheDaoImpl extends DynamoDaoBaseImpl implements RowCacheDao {
	private static final Comparator<DboRowCache> ROW_CACHE_COMPARATOR = new Comparator<DboRowCache>() {
		@Override
		public int compare(DboRowCache o1, DboRowCache o2) {
			return ComparisonChain.start().compare(o1.getHashKey(), o2.getHashKey()).compare(o1.getRangeKey(), o2.getRangeKey()).result();
		}
	};

	DynamoDBMapper mapper;

	public RowCacheDaoImpl(AmazonDynamoDB dynamoClient) {
		super(dynamoClient);
		mapper = new DynamoDBMapper(dynamoClient, DynamoConfig.getDynamoDBMapperConfigFor(DboRowCache.class));
	}

	private DboRowCache dboCreate(Long tableId, Long rowId, Long versionNumber, List<String> value) throws IOException {
		DboRowCache row = new DboRowCache();
		row.setHashKey(DboRowCache.createHashKey(tableId));
		row.setRangeKey(DboRowCache.createRangeKey(rowId, versionNumber));
		if (value != null) {
			byte[] serialized = Serializer.compressObject(value.toArray(new String[value.size()]));
			row.setValue(serialized);
		}
		return row;
	}

	private Row rowCreate(Long rowId, Long versionNumber, byte[] value) throws IOException {
		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(versionNumber);
		if (value != null) {
			String[] values = (String[]) Serializer.decompressedObject(value);
			row.setValues(Arrays.asList(values));
		}
		return row;
	}

	@Override
	public boolean isEnabled() {
		return isDynamoEnabled();
	}

	@Override
	public Row getRow(Long tableId, Long rowId, Long versionNumber) throws IOException {
		String hashKey = DboRowCache.createHashKey(tableId);
		String rangeKey = DboRowCache.createRangeKey(rowId, versionNumber);
		DboRowCache cachedRow = mapper.load(DboRowCache.class, hashKey, rangeKey);

		if (cachedRow == null) {
			return null;
		}

		return rowCreate(rowId, versionNumber, cachedRow.getValue());
	}

	@Override
	public Map<Long, Row> getRows(final Long tableId, Map<Long, Long> rowsToGet) throws IOException {
		List<KeyPair> keys = Transform.toList(rowsToGet.entrySet(), new Function<Map.Entry<Long, Long>, KeyPair>() {
			@Override
			public KeyPair apply(Map.Entry<Long, Long> ref) {
				return new KeyPair().withHashKey(DboRowCache.createHashKey(tableId)).withRangeKey(
						DboRowCache.createRangeKey(ref.getKey(), ref.getValue()));
			}
		});
		return getRows(keys);
	}

	@Override
	public Map<Long, Row> getRows(final Long tableId, final Long version, Iterable<Long> rowsToGet) throws IOException {
		List<KeyPair> keys = Transform.toList(rowsToGet, new Function<Long, KeyPair>() {
			@Override
			public KeyPair apply(Long rowId) {
				return new KeyPair().withHashKey(DboRowCache.createHashKey(tableId)).withRangeKey(DboRowCache.createRangeKey(rowId, version));
			}
		});
		return getRows(keys);
	}

	private Map<Long, Row> getRows(List<KeyPair> keys) throws IOException {
		Map<Class<?>, List<KeyPair>> toGet = Collections.<Class<?>, List<KeyPair>> singletonMap(DboRowCache.class, keys);
		Map<String, List<Object>> mappedResults = mapper.batchLoad(toGet);

		List<Object> results = Iterables.getOnlyElement(mappedResults.entrySet()).getValue();

		try {
			return Transform.toMap(results, new Function<Object, TransformEntry<Long, Row>>() {
				@Override
				public TransformEntry<Long, Row> apply(Object input) {
					DboRowCache cachedRow = (DboRowCache) input;
					try {
						Row row = rowCreate(cachedRow.getRowId(), cachedRow.getVersionNumber(), cachedRow.getValue());
						return new TransformEntry<Long, Row>(row.getRowId(), row);
					} catch (IOException e) {
						throw new UncheckedExecutionException(e);
					}
				}
			});
		} catch (UncheckedExecutionException e) {
			throw (IOException) e.getCause();
		}
	}

	@Override
	public void putRow(Long tableId, Row row) throws IOException {
		DboRowCache rowToCache = dboCreate(tableId, row.getRowId(), row.getVersionNumber(), row.getValues());
		mapper.save(rowToCache);
	}

	@Override
	public void putRows(final Long tableId, Iterable<Row> rowsToPut) throws IOException {
		List<DboRowCache> toUpdate;
		try {
			toUpdate = Transform.toList(rowsToPut, new Function<Row, DboRowCache>() {
				@Override
				public DboRowCache apply(Row row) {
					try {
						DboRowCache rowToCache = dboCreate(tableId, row.getRowId(), row.getVersionNumber(), row.getValues());
						return rowToCache;
					} catch (IOException e) {
						throw new UncheckedExecutionException(e);
					}
				}
			});
		} catch (UncheckedExecutionException e) {
			throw (IOException) e.getCause();
		}
		mapper.batchSave(toUpdate);
	}

	@Override
	public void deleteEntriesForTable(Long tableId) {
		AttributeValue hashKeyValue = new AttributeValue(DboRowCache.createHashKey(tableId));
		PaginatedQueryList<DboRowCache> results = mapper.query(DboRowCache.class, new DynamoDBQueryExpression(hashKeyValue));
		mapper.batchDelete(uniqueify(results, ROW_CACHE_COMPARATOR));
	}

	@Override
	public void truncateAllData() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		PaginatedScanList<DboRowCache> scanResult = mapper.scan(DboRowCache.class, scanExpression);
		mapper.batchDelete(uniqueify(scanResult, ROW_CACHE_COMPARATOR));
	}
}
