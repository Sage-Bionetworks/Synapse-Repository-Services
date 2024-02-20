package org.sagebionetworks.repo.manager.table.query;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;

@ExtendWith(MockitoExtension.class)
public class StreamingQueryExecutorTest {
	
	@Mock
	private RowHandler mockRowHandler;
	
	@InjectMocks
	private StreamingQueryExecutor queryExecutor;

	@Mock
	private TableIndexDAO mockIndexDao;

	@Mock
	private QueryTranslator mockQuery;
	
	@Test
	public void testExecuteQuery() {
		when(mockQuery.getSingleTableIdOptional()).thenReturn(Optional.of("syn123"));
		// Call under test
		queryExecutor.executeQuery(mockIndexDao, mockQuery);
		
		verify(mockIndexDao).queryAsStream(mockQuery, mockRowHandler);
		
	}

}
