package org.sagebionetworks.repo.manager.table;


import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsRequest;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadPFBRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableQueryUtilsTest {

	String tableId;
	String sql;
	DownloadFromTableRequest downloadRequest;
	QueryBundleRequest queryRequest;
	QueryNextPageToken nextPageToken;
	
	@BeforeEach
	public void before(){
		
		tableId = "syn123";
		sql = "select * from "+tableId;
		
		downloadRequest = new DownloadFromTableRequest();
		downloadRequest.setSql(sql);
		
		Query query = new Query();
		query.setSql(sql);
		queryRequest = new QueryBundleRequest();
		queryRequest.setQuery(query);
		
		nextPageToken = TableQueryUtils.createNextPageToken("select * from syn123", null, null, null, null, null, null);
	}

	@Test
	public void testCreateNextPageTokenWithFiltersAndCohorts() throws Exception {
		String sql = "select * from syn123 where pid in cohort(c1)";
		// The lists are mutable, as when parsed from a request; XStream rejects immutable list proxies.
		List<QueryFilter> filters = Lists.newArrayList(new ColumnCohortFilter().setColumnName("other").setCohortName("c2"));
		List<CohortDefinition> cohorts = Lists.newArrayList(
				new CohortDefinition().setName("c1").setQuery(new Query().setSql("select pid from syn456 where a = 'x'")),
				new CohortDefinition().setName("c2").setQuery(new Query().setSql("select other from syn789")
						.setAdditionalFilters(Lists.newArrayList(new ColumnSingleValueQueryFilter().setColumnName("b")
								.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(Lists.newArrayList("y"))))));
		// call under test
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(sql, null, 10L, 5L, null, filters, cohorts);
		Query query = TableQueryUtils.createQueryFromNextPageToken(token);
		assertEquals(new Query().setSql(sql).setOffset(10L).setLimit(5L).setAdditionalFilters(filters).setCohorts(cohorts),
				query);
	}

	@Test
	public void testHasCohortsWithQueryBundleRequest() {
		queryRequest.getQuery().setCohorts(List.of(new CohortDefinition().setName("c1")));
		// call under test
		assertTrue(TableQueryUtils.hasCohorts(queryRequest));
	}

	@Test
	public void testHasCohortsWithDownloadRequest() {
		downloadRequest.setCohorts(List.of(new CohortDefinition().setName("c1")));
		// call under test
		assertTrue(TableQueryUtils.hasCohorts(downloadRequest));
	}

	@Test
	public void testHasCohortsWithNextPageToken() {
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(sql, null, null, null, null, null,
				Lists.newArrayList(new CohortDefinition().setName("c1").setQuery(new Query().setSql("select a from syn1"))));
		// call under test
		assertTrue(TableQueryUtils.hasCohorts(token));
	}

	@Test
	public void testHasCohortsWithNoCohorts() {
		downloadRequest.setCohorts(Collections.emptyList());
		// call under test
		assertFalse(TableQueryUtils.hasCohorts(queryRequest));
		assertFalse(TableQueryUtils.hasCohorts(downloadRequest));
		assertFalse(TableQueryUtils.hasCohorts(nextPageToken));
	}

	@Test
	public void testCreateNextPageTokenEscapingSingle() throws Exception {
		String sql = "select \"i-0\" from syn123" ;
		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		List<SortItem> sortList= Lists.newArrayList(sort);
		FacetColumnRequest facet = new FacetColumnRangeRequest();
		facet.setColumnName("facetName");
		List<FacetColumnRequest> selectedFacets = Lists.newArrayList(facet);
		
		Long nextOffset = 10L;
		Long limit = 21L;
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(sql, sortList, nextOffset, limit, selectedFacets, null, null);
		Query query = TableQueryUtils.createQueryFromNextPageToken(token);
		assertEquals(sql, query.getSql());
		assertEquals(nextOffset, query.getOffset());
		assertEquals(limit, query.getLimit());
		assertEquals(sortList, query.getSort());
		assertEquals(selectedFacets, query.getSelectedFacets());
	}
	
	@Test
	public void testExtractTableIdFromSql(){
		String resultTableId = TableQueryUtils.extractTableIdFromSql(sql);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testExtractTableIdFromSqlWithJoin(){
		sql = "select * from syn123 join syn456";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}
	
	@Test
	public void testExtractTableIdFromSqlNull(){
		String sql = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		});
	}
	
	@Test
	public void testGetTableIdDownloadFromTableRequest(){
		String resultTableId = TableQueryUtils.getTableId(downloadRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testGetTableIdDownloadFromTableRequestNull(){
		DownloadFromTableRequest request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdQueryBundleRequest(){
		String resultTableId = TableQueryUtils.getTableId(queryRequest);
		assertEquals(tableId, resultTableId);
	}
	
	@Test
	public void testGetTableIdQueryBundleRequestNull(){
		QueryBundleRequest request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdQueryNextPageToken(){
		String requestTableId = TableQueryUtils.getTableId(nextPageToken);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdQueryNextPageTokenNull(){
		QueryNextPageToken request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableId(request);
		});
	}
	
	@Test
	public void testGetTableIdFromRequestBodydUnknownType(){
		CacheableRequestBody unknownType = Mockito.mock(CacheableRequestBody.class);
		assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.getTableIdFromRequestBody(unknownType);
		});
	}
	
	@Test
	public void testGetTableIdFromRequestBodyDownloadRequest(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(downloadRequest);
		assertEquals(tableId, requestTableId);
	}
	
	
	@Test
	public void testGetTableIdFromRequestBodyAsIdAndVersion(){
		queryRequest.getQuery().setSql("select * from syn123.456");
		IdAndVersion idandversion = TableQueryUtils.getTableIdFromRequestBodyAsIdAndVersion(queryRequest);
		assertEquals(IdAndVersion.parse("syn123.456"), idandversion);
	}
	
	
	@Test
	public void testGetTableIdFromRequestBodyQueryBundleRequest(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(queryRequest);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdFromRequestBodyQueryNextPageToken(){
		String requestTableId = TableQueryUtils.getTableIdFromRequestBody(nextPageToken);
		assertEquals(tableId, requestTableId);
	}
	
	@Test
	public void testGetTableIdFromRequestBodyWithDownloadPFBRequest() {
		DownloadPFBRequest request = new DownloadPFBRequest();
		request.setSql("select * from " + tableId);

		// call under test
		String requestTableId = TableQueryUtils
				.getTableIdFromRequestBody(request);
		assertEquals(tableId, requestTableId);
	}
	/**
	 * For PLFM-6027, a user's query contained an unknown character ("\u2018") resulting in a 
	 * org.sagebionetworks.table.query.TokenMgrError exception.  Since this exception was
	 * not mapped in the controllers, we returned a 500.
	 * 
	 * We expanded the parser to ignore characters in this range.
	 */
	@Test
	public void testPLFM_6027() {
		char unknownChar = 0x2018;
		String sql = "select * from syn123 "+unknownChar;
		String result = TableQueryUtils.extractTableIdFromSql(sql);
		assertEquals("syn123", result);
	}

	@Test
	/**
	 * PLFM-6392 Add a more informative message to the user for cases where keywords
	 * must be escaped.
	 */
	public void testExtractTableIdFromSqlWithParserException() {
		String sql = "select year from syn123 where timestamp = 1";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			TableQueryUtils.extractTableIdFromSql(sql);
		});
		assertTrue(exception.getMessage().startsWith("Encountered \" \"TIMESTAMP\" \"timestamp \"\" at line 1, column 31."));
		assertTrue(exception.getMessage().contains(TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE));
	}

	@Test
	public void testGetQueryFromRequestBodyWithQueryBundleRequest() {
		Query query = createAuditedQuery();

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new QueryBundleRequest().setEntityId("syn123").setQuery(query));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithNextPageToken() {
		Query query = createAuditedQuery().setOffset(10L).setLimit(10L);
		QueryNextPageToken token = TableQueryUtils.createNextPageToken(query.getSql(), null, 10L, 10L, null,
				query.getAdditionalFilters(), query.getCohorts());

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(token);

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithRequestThatExtendsQuery() {
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setEntityId("syn123");
		request.setSql("select * from syn123");
		request.setLimit(5L);
		request.setWriteHeader(true);

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(request);

		// Only the query's own parts are recorded, not the rest of the request.
		assertEquals(Query.class, result.get().getClass());
		assertEquals(Optional.of(new Query().setSql("select * from syn123").setLimit(5L)), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithEveryQueryField() throws Exception {
		Query query = createAuditedQuery()
				.setSelectedFacets(Lists.newArrayList(new FacetColumnRangeRequest().setColumnName("b").setMin("1").setMax("5")))
				.setIncludeEntityEtag(true)
				.setSelectFileColumn(11L)
				.setSelectFileVersionColumn(12L)
				.setOffset(10L)
				.setLimit(20L)
				.setSort(Lists.newArrayList(new SortItem().setColumn("b").setDirection(SortDirection.DESC)));
		// A field added to the Query schema must be set here too, so the copy is checked to keep it.
		assertEquals(getQuerySchemaProperties(), new JSONObject(EntityFactory.createJSONStringForEntity(query)).keySet());

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new QueryBundleRequest().setQuery(query));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithPFBRequest() {
		DownloadPFBRequest request = new DownloadPFBRequest();
		request.setSql("select * from syn123");

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(request);

		assertEquals(Optional.of(new Query().setSql("select * from syn123")), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithAddFileToDownloadListRequest() {
		Query query = createAuditedQuery();

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new AddFileToDownloadListRequest().setQuery(query));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithAddToDownloadListRequest() {
		Query query = createAuditedQuery();

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new AddToDownloadListRequest().setQuery(query));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithAddToDownloadListStatsRequest() {
		Query query = createAuditedQuery();

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(
				new AddToDownloadListStatsRequest().setRequest(new AddToDownloadListRequest().setQuery(query)));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithAddToDownloadListRequestWithoutQuery() {
		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new AddToDownloadListRequest().setParentId("syn456"));

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithAddToDownloadListStatsRequestWithoutRequest() {
		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new AddToDownloadListStatsRequest());

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithCreateGridRequest() {
		Query query = createAuditedQuery();

		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new CreateGridRequest().setInitialQuery(query));

		assertEquals(Optional.of(query), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithNoQuery() {
		// call under test
		Optional<Query> result = TableQueryUtils.getQueryFromRequestBody(new BulkFileDownloadRequest());

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testGetQueryFromRequestBodyWithNullBody() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			TableQueryUtils.getQueryFromRequestBody(null);
		});
	}

	private static Set<String> getQuerySchemaProperties() throws IOException {
		try (InputStream schema = Query.class.getClassLoader().getResourceAsStream("schema/org/sagebionetworks/repo/model/table/Query.json")) {
			return new JSONObject(IOUtils.toString(schema, StandardCharsets.UTF_8)).getJSONObject("properties").keySet();
		}
	}

	/**
	 * Mutable lists, as a deserialized request has, so the query survives the next page token's XML.
	 */
	private static Query createAuditedQuery() {
		return new Query().setSql("select * from syn123 where a in cohort(c1)")
				.setAdditionalFilters(Lists.newArrayList(new ColumnSingleValueQueryFilter().setColumnName("b")
						.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(Lists.newArrayList("x"))))
				.setCohorts(Lists.newArrayList(new CohortDefinition().setName("c1").setQuery(new Query().setSql("select a from syn456"))));
	}
}
