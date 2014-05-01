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
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.datamodeling.KeyPair;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedScanList;
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

	private DboRowCache dboCreate(String tableId, Long rowId, Long versionNumber) {
		DboRowCache row = new DboRowCache();
		row.setHashKey(DboRowCache.createHashKey(tableId));
		row.setRangeKey(DboRowCache.createRangeKey(rowId, versionNumber));
		return row;
	}

	@Override
	public boolean isEnabled() {
		return isDynamoEnabled();
	}

	@Override
	public Row getRow(String tableId, Long rowId, Long versionNumber) throws IOException {
		String hashKey = DboRowCache.createHashKey(tableId);
		String rangeKey = DboRowCache.createRangeKey(rowId, versionNumber);
		DboRowCache cachedRow = mapper.load(DboRowCache.class, hashKey, rangeKey);

		if (cachedRow == null) {
			return null;
		}

		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(versionNumber);
		if (cachedRow.getValue() != null) {
			String[] values = (String[]) Serializer.decompressedObject(cachedRow.getValue());
			row.setValues(Arrays.asList(values));
		}
		return row;
	}

	@Override
	public Map<Long, Row> getRows(final String tableId, Map<Long, Long> rowsToGet) throws IOException {
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
	public Map<Long, Row> getRows(final String tableId, final Long version, Iterable<Long> rowsToGet) throws IOException {
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
					Row row = new Row();
					row.setRowId(cachedRow.getRowId());
					row.setVersionNumber(cachedRow.getVersionNumber());
					if (cachedRow.getValue() != null) {
						try {
							String[] values = (String[]) Serializer.decompressedObject(cachedRow.getValue());
							row.setValues(Arrays.asList(values));
						} catch (IOException e) {
							throw new UncheckedExecutionException(e);
						}
					}
					return new TransformEntry<Long, Row>(row.getRowId(), row);
				}
			});
		} catch (UncheckedExecutionException e) {
			throw (IOException) e.getCause();
		}
	}

	@Override
	public void putRow(String tableId, Row row) throws IOException {
		DboRowCache rowToCache = dboCreate(tableId, row.getRowId(), row.getVersionNumber());
		if (row.getValues() != null) {
			byte[] serialized = Serializer.compressObject(row.getValues().toArray(new String[row.getValues().size()]));
			rowToCache.setValue(serialized);
		}
		mapper.save(rowToCache);
	}

	@Override
	public void putRows(final String tableId, Iterable<Row> rowsToPut) throws IOException {
		List<DboRowCache> toUpdate;
		try {
			toUpdate = Transform.toList(rowsToPut, new Function<Row, DboRowCache>() {
				@Override
				public DboRowCache apply(Row row) {
					DboRowCache rowToCache = dboCreate(tableId, row.getRowId(), row.getVersionNumber());
					if (row.getValues() != null) {
						byte[] serialized;
						try {
							serialized = Serializer.compressObject(row.getValues().toArray(new String[row.getValues().size()]));
						} catch (IOException e) {
							throw new UncheckedExecutionException(e);
						}
						rowToCache.setValue(serialized);
					}
					return rowToCache;
				}
			});
		} catch (UncheckedExecutionException e) {
			throw (IOException) e.getCause();
		}
		mapper.batchSave(toUpdate);
	}

	@Override
	public void truncateAllData() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		PaginatedScanList<DboRowCache> scanResult = mapper.scan(DboRowCache.class, scanExpression);
		mapper.batchDelete(uniqueify(scanResult, ROW_CACHE_COMPARATOR));
	}
}
