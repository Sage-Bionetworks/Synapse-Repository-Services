package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.table.QueryCacheManager;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UpdateQueryCacheWorkerIntegrationTest {

	public static final Long MAX_WAIT_MS = 20_000L;

	@Autowired
	private QueryCacheManager queryCacheManager;
	@Autowired
	private ConnectionFactory connectionFactory;

	@Test
	public void testRefreshQuery() throws Exception {
		TableIndexDAO indexDao = connectionFactory.getFirstConnection();
		indexDao.truncateIndex();

		indexDao.update("DROP TABLE IF EXISTS SOME_DATA", null);
		indexDao.update("CREATE TABLE IF NOT EXISTS SOME_DATA (	ID BIGINT NOT NULL,	PRIMARY KEY(ID))", null);
		indexDao.update("INSERT INTO SOME_DATA () VALUES (1)", null);

		List<SelectColumn> selectColumns = List
				.of(new SelectColumn().setColumnType(ColumnType.INTEGER).setName("count"));

		CachedQueryRequest query = new CachedQueryRequest().setOutputSQL("select count(*) from SOME_DATA limit :limit")
				.setSelectColumns(selectColumns).setSingleTableId("syn123").setParameters(Map.of("limit", 100L))
				.setExpiresInSec(2);

		RowSet results = queryCacheManager.getQueryResults(indexDao, query);
		RowSet expected = new RowSet().setTableId("syn123").setHeaders(selectColumns)
				.setRows(List.of(new Row().setValues(List.of("1"))));
		assertEquals(expected, results);

		indexDao.update("INSERT INTO SOME_DATA () VALUES (2)", null);

		RowSet updatedExpected = new RowSet().setTableId("syn123").setHeaders(selectColumns)
				.setRows(List.of(new Row().setValues(List.of("2"))));

		TimeUtils.waitFor(MAX_WAIT_MS, 1000, () -> {
			// call under test
			RowSet current = queryCacheManager.getQueryResults(indexDao, query);
			System.out.println("Waiting for cache worker to update the value");
			return new Pair<Boolean, Void>(current.equals(updatedExpected), null);

		});
	}

}
