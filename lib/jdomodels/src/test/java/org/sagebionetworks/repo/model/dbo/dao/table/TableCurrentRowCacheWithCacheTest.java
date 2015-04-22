package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class TableCurrentRowCacheWithCacheTest extends TableCurrentRowCacheTest {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;

	private Object oldStackConfiguration;

	@Before
	public void enableCache() throws Exception {
		oldStackConfiguration = ReflectionStaticTestUtils.getField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"),
				"stackConfiguration");
		StackConfiguration mockStackConfiguration = mock(StackConfiguration.class);
		when(mockStackConfiguration.getTableEnabled()).thenReturn(true);
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				mockStackConfiguration);
		tableRowTruthDao.truncateAllRowData();
	}

	@After
	public void disableCache() throws Exception {
		tableRowTruthDao.truncateAllRowData();
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				oldStackConfiguration);
	}
}
