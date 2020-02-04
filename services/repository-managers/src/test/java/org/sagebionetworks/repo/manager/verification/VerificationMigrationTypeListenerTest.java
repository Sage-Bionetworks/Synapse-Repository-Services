package org.sagebionetworks.repo.manager.verification;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.verification.VerificationSubmissionHelper;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

@RunWith(MockitoJUnitRunner.class)
public class VerificationMigrationTypeListenerTest {
	
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	
	@Mock
	private DBOBasicDao mockBasicDao;
	
	@InjectMocks
	private VerificationMigrationTypeListener listener;
	
	private static final String EMAIL = "me@company.com";
	
	@Captor
	ArgumentCaptor<DBOVerificationSubmission> dboCaptor;


	@Test
	public void testJustOneEmail() {
		MigrationType type = MigrationType.VERIFICATION_SUBMISSION;
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		List<DatabaseObject<?>> delta = Collections.singletonList(dbo);
		
		VerificationSubmission dto = new VerificationSubmission();
		dto.setNotificationEmail(null); // it's the job of the migration listener to fill this in
		dto.setEmails(Collections.singletonList(EMAIL)); // we captured just one email, so this is the notification email
		dbo.setSerialized(VerificationSubmissionHelper.serializeDTO(dto));
		
		// method under test
		listener.afterCreateOrUpdate(type, delta);
		
		verify(mockBasicDao).update(dboCaptor.capture());
		byte[] serialized = dboCaptor.getValue().getSerialized();
		VerificationSubmission updated = VerificationSubmissionHelper.deserializeDTO(serialized);
		assertEquals(updated.getNotificationEmail(), EMAIL);
	}

	@Test
	public void testOtherType() {
		MigrationType type = MigrationType.NODE;
		DBONode dbo = new DBONode();
		List<DBONode> delta = Collections.singletonList(dbo);
		// method under test
		listener.afterCreateOrUpdate(type, delta);
		
		// nothing to update
		verify(mockBasicDao, never()).update((DatabaseObject)any());
	}

}
