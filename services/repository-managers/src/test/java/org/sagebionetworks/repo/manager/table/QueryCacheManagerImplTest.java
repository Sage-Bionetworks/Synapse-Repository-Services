package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.manager.config.ManagerConfiguration;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.QueryCacheHitEvent;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.TableIndexDAO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class QueryCacheManagerImplTest {

	@Mock
	private ObjectMapper mockObjectMapper;
	private ObjectMapper objectMapper = new ManagerConfiguration().jsonObjectMapper();
	@Mock
	private Clock mockClock;
	@Mock
	private RepositoryMessagePublisher mockPublisher;
	@Mock
	TableIndexDAO mockTableIndexDao;

	@InjectMocks
	@Spy
	private QueryCacheManagerImpl manager;

	@Test
	public void testGetQueryResultsWithCacheMiss() throws JsonProcessingException {
		CachedQueryRequest request = new CachedQueryRequest().setExpiresInSec(12).setIncludeEntityEtag(true)
				.setIncludesRowIdAndVersion(true).setOutputSQL("select * from syn123")
				.setParameters(Map.of("limit", 18)).setSelectColumns(List.of(new SelectColumn().setName("foo")))
				.setSingleTableId("syn123");

		when(mockTableIndexDao.getCachedQueryResults(any())).thenReturn(Optional.empty());
		String requestJson = objectMapper.writeValueAsString(request);
		when(mockObjectMapper.writeValueAsString(any())).thenReturn(requestJson);
		String hash = DigestUtils.sha256Hex(requestJson);

		RowSet toReturn = new RowSet().setTableId("123").setRows(List.of(new Row().setRowId(88L)));
		doReturn(toReturn).when(manager).executeQueryAndSaveToCache(any(), any(), any(), any(), anyInt());

		// call under test
		RowSet results = manager.getQueryResults(mockTableIndexDao, request);

		assertEquals(toReturn, results);
		verify(mockTableIndexDao).getCachedQueryResults(hash);
		verify(manager).executeQueryAndSaveToCache(mockTableIndexDao, request, requestJson, hash, 12);
		verify(mockObjectMapper).writeValueAsString(request);
		verifyZeroInteractions(mockPublisher);
	}

	@Test
	public void testGetQueryResultsWithCacheHit() throws JsonProcessingException, JSONObjectAdapterException {
		CachedQueryRequest request = new CachedQueryRequest().setExpiresInSec(12).setIncludeEntityEtag(true)
				.setIncludesRowIdAndVersion(true).setOutputSQL("select * from syn123")
				.setParameters(Map.of("limit", 18)).setSelectColumns(List.of(new SelectColumn().setName("foo")))
				.setSingleTableId("syn123");

		RowSet toReturn = new RowSet().setTableId("123").setRows(List.of(new Row().setRowId(88L)));
		String resultJson = EntityFactory.createJSONStringForEntity(toReturn);

		when(mockTableIndexDao.getCachedQueryResults(any())).thenReturn(Optional.of(resultJson));
		String requestJson = objectMapper.writeValueAsString(request);
		when(mockObjectMapper.writeValueAsString(any())).thenReturn(requestJson);
		String hash = DigestUtils.sha256Hex(requestJson);

		// call under test
		RowSet results = manager.getQueryResults(mockTableIndexDao, request);

		assertEquals(toReturn, results);
		verify(mockTableIndexDao).getCachedQueryResults(hash);
		verify(manager, never()).executeQueryAndSaveToCache(mockTableIndexDao, request, requestJson, hash, 12);
		verify(mockObjectMapper).writeValueAsString(request);
		verify(mockPublisher).fireLocalStackMessage(
				new QueryCacheHitEvent().setQueryRequestHash(hash).setObjectType(ObjectType.QUERY_CACHE_HIT));
	}

	@Test
	public void testRowSetToFromJSON() {
		RowSet toReturn = new RowSet().setTableId("123")
				.setRows(List.of(new Row().setRowId(88L).setValues(List.of("one", "two"))));
		// call under test
		String json = manager.rowSetToJson(toReturn);
		// call under test
		RowSet clone = manager.parseRowSet(json);
		assertEquals(toReturn, clone);
	}

	@Test
	public void testRequestToFromJSON() throws JsonProcessingException {
		CachedQueryRequest request = new CachedQueryRequest().setExpiresInSec(12).setIncludeEntityEtag(true)
				.setIncludesRowIdAndVersion(true).setOutputSQL("select * from syn123")
				.setParameters(Map.of("limit", 18, "offset", 0)).setSelectColumns(List.of(new SelectColumn().setName("foo")))
				.setSingleTableId("syn123");
		String originalJson = objectMapper.writeValueAsString(request);
		when(mockObjectMapper.writeValueAsString(any())).thenReturn(originalJson);
		when(mockObjectMapper.readValue(any(String.class), any(Class.class)))
				.thenReturn(objectMapper.readValue(originalJson, CachedQueryRequest.class));

		String json = manager.requestToJson(request);
		CachedQueryRequest clone = manager.parseRequestJson(json);
		assertEquals(request, clone);
	}
	
	@Test
	public void testRequestToFromJSONWithNulls() throws JsonProcessingException {
		CachedQueryRequest request = new CachedQueryRequest();
		String originalJson = objectMapper.writeValueAsString(request);
		when(mockObjectMapper.writeValueAsString(any())).thenReturn(originalJson);
		when(mockObjectMapper.readValue(any(String.class), any(Class.class)))
				.thenReturn(objectMapper.readValue(originalJson, CachedQueryRequest.class));

		String json = manager.requestToJson(request);
		CachedQueryRequest clone = manager.parseRequestJson(json);
		assertEquals(request, clone);
	}
	
	@Test
	public void testRefreshCachedQueryWithExpired() {
		String hash = "someHash";
		String requestJson = "request json";
		when(mockTableIndexDao.getExpiredCachedQueryRequest(any())).thenReturn(Optional.of(requestJson));
		doReturn(new RowSet()).when(manager).executeQueryAndSaveToCache(any(), any(), any(), any(), anyInt());
		CachedQueryRequest request = new CachedQueryRequest().setOutputSQL("select foo").setExpiresInSec(88);
		doReturn(request).when(manager).parseRequestJson(any());
		
		// call under test
		manager.refreshCachedQuery(mockTableIndexDao, hash);
		
		verify(mockTableIndexDao).getExpiredCachedQueryRequest(hash);
		verify(manager).executeQueryAndSaveToCache(mockTableIndexDao, request, requestJson, hash, 88);
		verify(manager).parseRequestJson(requestJson);
	}
	
	@Test
	public void testRefreshCachedQueryWithNotExpired() {
		String hash = "someHash";
		when(mockTableIndexDao.getExpiredCachedQueryRequest(any())).thenReturn(Optional.empty());
		
		// call under test
		manager.refreshCachedQuery(mockTableIndexDao, hash);
		
		verify(mockTableIndexDao).getExpiredCachedQueryRequest(hash);
		verify(manager, never()).executeQueryAndSaveToCache(any(), any(), any(), any(), anyInt());
		verify(manager, never()).parseRequestJson(any());
	}
	
	@Test
	public void testExecuteQueryAndSaveToCache() throws JsonProcessingException {
		String hash = "someHash";
		CachedQueryRequest request = new CachedQueryRequest().setExpiresInSec(12).setIncludeEntityEtag(true)
				.setIncludesRowIdAndVersion(true).setOutputSQL("select * from syn123")
				.setParameters(Map.of("limit", 18, "offset", 0)).setSelectColumns(List.of(new SelectColumn().setName("foo")))
				.setSingleTableId("syn123");
		String requestJson = objectMapper.writeValueAsString(request);
		int expiresInSec = 14;
		RowSet rowSet = new RowSet().setTableId("syn123");
		when(mockTableIndexDao.query(any())).thenReturn(rowSet);
		when(mockClock.currentTimeMillis()).thenReturn(1L, 8L);
		long runtime = 8-1;
		String resultJson = "resultJson";
		doReturn(resultJson).when(manager).rowSetToJson(any());
		
		// call under test
		RowSet result = manager.executeQueryAndSaveToCache(mockTableIndexDao, request, requestJson, hash, expiresInSec);
		assertEquals(rowSet, result);
		
		verify(mockTableIndexDao).query(request);
		verify(mockTableIndexDao).saveCachedQuery(hash, requestJson, resultJson, runtime, expiresInSec);
		verify(mockClock, times(2)).currentTimeMillis();
	}
}
