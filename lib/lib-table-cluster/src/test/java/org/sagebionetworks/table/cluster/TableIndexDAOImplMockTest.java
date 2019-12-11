package org.sagebionetworks.table.cluster;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
public class TableIndexDAOImplMockTest {

	@Mock
	private DataSourceTransactionManager transactionManager;
	@Mock
	private TransactionTemplate writeTransactionTemplate;
	@Mock
	private TransactionTemplate readTransactionTemplate;
	@Mock
	private JdbcTemplate template;

	@InjectMocks
	private TableIndexDAOImpl tableIndexDAO;

	IdAndVersion tableId;

	@BeforeEach
	public void setUp(){
		tableId = IdAndVersion.parse("syn123");
	}

	@Test
	public void testDeleteListColumnIndexTables_nullList(){
		tableIndexDAO.deleteListColumnIndexTables(tableId, null);
		verifyZeroInteractions(template);
	}

	@Test
	public void testDeleteListColumnIndexTables_emptyList(){
		tableIndexDAO.deleteListColumnIndexTables(tableId, Collections.emptyList());
		verifyZeroInteractions(template);
	}

	@Test
	public void testDeleteListColumnIndexTables_nonEmptyList(){
		tableIndexDAO.deleteListColumnIndexTables(tableId, Arrays.asList(123L,456L,789L));
		verify(template).update("DROP TABLE T123_INDEX_C123_,T123_INDEX_C456_,T123_INDEX_C789_");
	}
}

