package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchLoggerImplTest {

	@Mock
	AwsKinesisFirehoseLogger mockFirehoseLogger;

	@InjectMocks
	CloudSearchLoggerImpl logger;

	ChangeMessage changeOne;

	@Before
	public void before() {
		// start with an empty thread local.
		logger.pushAllRecordsAndReset();

		changeOne = new ChangeMessage();
		changeOne.setObjectId("syn123");
		changeOne.setChangeNumber(333L);
		changeOne.setChangeType(ChangeType.CREATE);
	}
	
	@After
	public void after() {
		logger.pushAllRecordsAndReset();
	}

	@Test
	public void testStartRecordForChangeMessage() {
		// call under test
		CloudSearchDocumentLogRecord record = logger.startRecordForChangeMessage(changeOne);
		assertNotNull(record);
		assertEquals(changeOne.getObjectId(), record.getObjectId());
		assertEquals(changeOne.getChangeNumber(), record.getChangeNumber());
		assertEquals(changeOne.getChangeType(), record.getChangeType());
		assertEquals(changeOne.getObjectType(), record.getObjectType());
		assertNotNull(record.getTimestamp());
	}

	@Test
	public void testCurrentBatchFinshedCreateOrUpdateAction() {
		// Start a record
		CloudSearchDocumentLogRecord record = logger.startRecordForChangeMessage(changeOne);
		record.withAction(DocumentAction.CREATE_OR_UPDATE);
		String statusString = "some status";
		// call under test
		logger.currentBatchFinshed(statusString);
		assertEquals(statusString, record.getDocumentBatchUpdateStatus());
		assertNotNull(record.getDocumentBatchUUID());
	}

	@Test
	public void testCurrentBatchFinshedDeleteAction() {
		// Start a record
		CloudSearchDocumentLogRecord record = logger.startRecordForChangeMessage(changeOne);
		record.withAction(DocumentAction.DELETE);
		String statusString = "some status";
		// call under test
		logger.currentBatchFinshed(statusString);
		assertEquals(statusString, record.getDocumentBatchUpdateStatus());
		assertNotNull(record.getDocumentBatchUUID());
	}

	@Test
	public void testCurrentBatchFinshedIgnoredAction() {
		// Start a record
		CloudSearchDocumentLogRecord record = logger.startRecordForChangeMessage(changeOne);
		record.withAction(DocumentAction.IGNORE);
		String statusString = "some status";
		// call under test
		logger.currentBatchFinshed(statusString);
		assertNull(record.getDocumentBatchUpdateStatus());
		assertNull(record.getDocumentBatchUUID());
	}

	@Test
	public void testPushAllRecordsAndReset() {
		// Start a record
		CloudSearchDocumentLogRecord record = logger.startRecordForChangeMessage(changeOne);
		// call under test
		logger.pushAllRecordsAndReset();
		logger.pushAllRecordsAndReset();
		// multiple calls should only push once
		verify(mockFirehoseLogger).logBatch(CloudSearchDocumentLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX,
				Lists.newArrayList(record));
	}

	@Test
	public void testPushAllRecordsAndResetNoRecords() {
		// call under test
		logger.pushAllRecordsAndReset();
		// with no records nothing should be pushed.
		verify(mockFirehoseLogger, never()).logBatch(any(String.class), any());
	}

}
