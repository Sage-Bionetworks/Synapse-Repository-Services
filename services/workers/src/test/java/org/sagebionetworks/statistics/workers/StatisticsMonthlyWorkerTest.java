package org.sagebionetworks.statistics.workers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.statistics.StatisticsProcessingException;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyWorkerTest {

	@Mock
	private StatisticsMonthlyManager mockManager;
	
	@Mock
	private WorkerLogger workerLogger;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@InjectMocks
	private StatisticsMonthlyWorker worker;
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		YearMonth month = YearMonth.of(2019, 8);
		
		String messageBody = StatisticsMonthlyUtils.buildNotificationBody(StatisticsObjectType.PROJECT, month);
		
		when(mockMessage.getBody()).thenReturn(messageBody);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processMonth(StatisticsObjectType.PROJECT, month, mockCallback);
	}
	
	@Test
	public void testRunWithFailure() throws RecoverableMessageException, Exception {
		YearMonth month = YearMonth.of(2019, 8);
		
		String messageBody = StatisticsMonthlyUtils.buildNotificationBody(StatisticsObjectType.PROJECT, month);
		
		when(mockMessage.getBody()).thenReturn(messageBody);
		
		StatisticsProcessingException ex = new StatisticsProcessingException();
		doThrow(ex).when(mockManager).processMonth(StatisticsObjectType.PROJECT, month, mockCallback);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processMonth(StatisticsObjectType.PROJECT, month, mockCallback);
		verify(workerLogger).logWorkerFailure(StatisticsMonthlyWorker.class.getName(), ex, false);
	}
}
