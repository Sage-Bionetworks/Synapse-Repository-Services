package org.sagebionetworks.repo.manager.table;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.QueryCacheHitEvent;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QueryCacheManagerImpl implements QueryCacheManager {

	private ObjectMapper objectMapper;
	private Clock clock;
	private RepositoryMessagePublisher publisher;

	@Autowired
	public QueryCacheManagerImpl(ObjectMapper objectMapper, Clock clock, RepositoryMessagePublisher publisher) {
		super();
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.publisher = publisher;
	}

	@Override
	public RowSet getQueryResults(TableIndexDAO indexDao, CachedQueryRequest request) {
		ValidateArgument.required(indexDao, "TableIndexDAO");
		ValidateArgument.required(request, "CachedQueryRequest");

		String requestJson = requestToJson(request);
		String hash = DigestUtils.sha256Hex(requestJson);

		Optional<String> optional = indexDao.getCachedQueryResults(hash);
		if (optional.isPresent()) {
			RowSet results = parseRowSet(optional.get());
			publisher.fireLocalStackMessage(
					new QueryCacheHitEvent().setQueryRequestHash(hash).setObjectType(ObjectType.QUERY_CACHE_HIT));
			return results;
		} else {
			return executeQueryAndSaveToCache(indexDao, request, requestJson, hash, request.getExpiresInSec());
		}
	}

	RowSet executeQueryAndSaveToCache(TableIndexDAO indexDao, CachedQueryRequest request, String requestJson,
			String hash, int expiresInSec) {
		long start = clock.currentTimeMillis();
		RowSet results = indexDao.query(request);
		long runtimeMS = clock.currentTimeMillis() - start;
		indexDao.saveCachedQuery(hash, requestJson, rowSetToJson(results), runtimeMS, expiresInSec);
		return results;
	}

	String rowSetToJson(RowSet rowSet) {
		try {
			return EntityFactory.createJSONStringForEntity(rowSet);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	RowSet parseRowSet(String json) {
		try {
			return EntityFactory.createEntityFromJSONString(json, RowSet.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	String requestToJson(CachedQueryRequest request) {
		try {
			return objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	CachedQueryRequest parseRequestJson(String requestJson) {
		try {
			return objectMapper.readValue(requestJson, CachedQueryRequest.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void refreshCachedQuery(TableIndexDAO indexDao, String requestHash) {
		ValidateArgument.required(indexDao, "TableIndexDAO");
		ValidateArgument.required(requestHash, "requestHash");
		indexDao.getExpiredCachedQueryRequest(requestHash).ifPresent((requestJson) -> {
			CachedQueryRequest request = parseRequestJson(requestJson);
			executeQueryAndSaveToCache(indexDao, request, requestJson, requestHash, request.getExpiresInSec());
		});
	}
}
