package org.sagebionetworks.snapshot.workers.writers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.snapshot.workers.writers.VerificationSubmissionObjectRecordWriter.LIMIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.verification.VerificationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class VerificationSubmissionObjectRecordWriterTest {

	@Mock
	private VerificationManager mockVerificationManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private AwsKinesisFirehoseLogger logger;
	@InjectMocks
	private VerificationSubmissionObjectRecordWriter writer;
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;
	
	private UserInfo admin = new UserInfo(true);
	private Long userId = 123L;

	@BeforeEach
	public void before() {
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(admin );
	}

	@Test
	public void deleteChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, "123", ObjectType.VERIFICATION_SUBMISSION, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verifyZeroInteractions(logger);
	}

	@Test
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ENTITY, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {			
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
	}

	@Test
	public void emptyRecords() throws IOException {
		VerificationPagedResults results = new VerificationPagedResults();
		results.setTotalNumberOfResults(0L);
		results.setResults(new ArrayList<VerificationSubmission>());
		when(mockVerificationManager.listVerificationSubmissions(admin, null, userId, LIMIT , 0L)).thenReturn(results);

		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.VERIFICATION_SUBMISSION, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verifyZeroInteractions(logger);
	}

	@Test
	public void onePageOfRecords() throws IOException {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		long timestamp = System.currentTimeMillis();
		
		VerificationPagedResults pageOne = new VerificationPagedResults();
		pageOne.setTotalNumberOfResults(1L);
		pageOne.setResults(Arrays.asList(verificationSubmission));
		when(mockVerificationManager.listVerificationSubmissions(admin, null, userId, LIMIT , 0L)).thenReturn(pageOne);
		when(mockVerificationManager.listVerificationSubmissions(admin, null, userId, LIMIT , 0L)).thenReturn(pageOne);

		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.VERIFICATION_SUBMISSION, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		KinesisObjectSnapshotRecord<?> expectedRecordOne =KinesisObjectSnapshotRecord.map(changeMessage, verificationSubmission);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(logger).logBatch(eq("verificationSubmissionSnapshots"), recordCaptor.capture());
		assertNotNull(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		expectedRecordOne.withSnapshotTimestamp(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		assertEquals(List.of(expectedRecordOne), recordCaptor.getValue());
	}

	@Test
	public void twoPagesOfRecords() throws IOException {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		long timestamp = System.currentTimeMillis();
		
		VerificationPagedResults pageOne = new VerificationPagedResults();
		pageOne.setTotalNumberOfResults(11L);
		pageOne.setResults(Arrays.asList(verificationSubmission));
		when(mockVerificationManager.listVerificationSubmissions(admin, null, userId, LIMIT , 0L)).thenReturn(pageOne);
		when(mockVerificationManager.listVerificationSubmissions(admin, null, userId, LIMIT , LIMIT)).thenReturn(pageOne);
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.VERIFICATION_SUBMISSION, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		KinesisObjectSnapshotRecord<?> expectedRecordOne =KinesisObjectSnapshotRecord.map(changeMessage, verificationSubmission);
		KinesisObjectSnapshotRecord<?> expectedRecordTwo =KinesisObjectSnapshotRecord.map(changeMessage, verificationSubmission);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(logger).logBatch(eq("verificationSubmissionSnapshots"), recordCaptor.capture());
		assertNotNull(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		expectedRecordOne.withSnapshotTimestamp(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		expectedRecordTwo.withSnapshotTimestamp(recordCaptor.getAllValues().get(0).get(1).getSnapshotTimestamp());
		assertEquals(List.of(expectedRecordOne, expectedRecordTwo), recordCaptor.getValue());
	}
}
