package org.sagebionetworks.table.worker;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.QueryCacheManager;
import org.sagebionetworks.repo.model.table.QueryCacheHitEvent;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class UpdateQueryCacheWorkerTest {

	@Mock
	private ConnectionFactory mockConnectionFactory;
	@Mock
	private QueryCacheManager mockQueryCacheManager;
	@Mock
	private TableIndexDAO mockIndexDao;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private Message mockMessage;
	
	@InjectMocks
	private UpdateQueryCacheWorker worker;
	
	
	@Test
	public void testRun() throws Exception {
		String requestHash = "someHash";
		QueryCacheHitEvent event = new QueryCacheHitEvent().setQueryRequestHash(requestHash);
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockIndexDao);
		
		// call under test
		worker.run(mockProgressCallback, mockMessage, event);
		
		verify(mockConnectionFactory).getFirstConnection();
		verify(mockQueryCacheManager).refreshCachedQuery(mockIndexDao, requestHash);
		
	}
}
