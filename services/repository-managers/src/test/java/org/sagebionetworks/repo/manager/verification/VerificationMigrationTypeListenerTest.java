package org.sagebionetworks.repo.manager.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.verification.VerificationSubmissionHelper;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class VerificationMigrationTypeListenerTest {
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	
	@Mock
	private DBOBasicDao mockBasicDao;
	
	@InjectMocks
	private VerificationMigrationTypeListener listener;
	
	private static final String USER_ID = "111";
	private static final String EMAIL = "me@company.com";
	
	@Captor
	private ArgumentCaptor<DBOVerificationSubmission> dboCaptor;
	
	private DBOVerificationSubmission dbo;
	private List<DatabaseObject<?>> delta;
	
	@BeforeEach
	public void before() {
		dbo = new DBOVerificationSubmission();
		delta = Collections.singletonList(dbo);
		
	}
	
	private void setEmails(List<String> emails) {
		VerificationSubmission dto = new VerificationSubmission();
		dto.setCreatedBy(USER_ID);
		dto.setNotificationEmail(null); // it's the job of the migration listener to fill this in
		dto.setEmails(emails);
		assertNotNull(dbo);
		dbo.setSerialized(VerificationSubmissionHelper.serializeDTO(dto));		
	}
	
	private static String getNotificationEmail(DBOVerificationSubmission dbo) {
		byte[] serialized = dbo.getSerialized();
		VerificationSubmission dto = VerificationSubmissionHelper.deserializeDTO(serialized);
		return dto.getNotificationEmail();
	}

	@Test
	public void testJustOneEmail() {
		setEmails(Collections.singletonList(EMAIL)); // we captured just one email, so this is the notification email

		// method under test
		listener.afterCreateOrUpdate(MigrationType.VERIFICATION_SUBMISSION, delta);
		
		// no ambiguity, so never needed to check the current notification email address
		verify(mockNotificationEmailDao, never()).getNotificationEmailForPrincipal(eq(Long.parseLong(USER_ID)));
		
		verify(mockBasicDao).update(dboCaptor.capture());
		assertEquals(getNotificationEmail(dboCaptor.getValue()), EMAIL);
	}

	@Test
	public void testMultipleEmailsFoundNotification() {
		setEmails(ImmutableList.of("some@other.com", EMAIL)); 
		
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(USER_ID))).thenReturn(EMAIL);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.VERIFICATION_SUBMISSION, delta);
		
		// check the current notification email address
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(Long.parseLong(USER_ID));
		
		verify(mockBasicDao).update(dboCaptor.capture());
		assertEquals(getNotificationEmail(dboCaptor.getValue()), EMAIL);
	}

	@Test
	public void testMultipleEmailsNoNotification() {
		setEmails(ImmutableList.of(EMAIL, "some@other.com")); 
		
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(USER_ID))).thenReturn("yet@another.com");
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.VERIFICATION_SUBMISSION, delta);
		
		// check the current notification email address
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(Long.parseLong(USER_ID));
		
		verify(mockBasicDao).update(dboCaptor.capture());
		assertEquals(getNotificationEmail(dboCaptor.getValue()), EMAIL);
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
