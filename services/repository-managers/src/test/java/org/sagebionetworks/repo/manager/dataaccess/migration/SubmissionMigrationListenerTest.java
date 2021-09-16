package org.sagebionetworks.repo.manager.dataaccess.migration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;

@ExtendWith(MockitoExtension.class)
public class SubmissionMigrationListenerTest {

	@Mock
	private SubmissionDAO mockDao;
	
	@InjectMocks
	private SubmissionMigrationListener listener;

	@Test
	public void testAfterCreateOrUpdate() {
				
		Submission dto = new Submission();
		dto.setId("1");
		
		DBOSubmission dbo = new DBOSubmission();
		dbo.setId(1L);
		dbo.setSubmissionSerialized(SubmissionUtils.writeSerializedField(dto));
		
		// Call under test
		listener.afterCreateOrUpdate(Arrays.asList(dbo));
		
		verify(mockDao).backFillAccessorChangesIfNeeded(dto);
		
	}
	
	@Test
	public void testAfterCreateOrUpdateWithNull() {
		
		// Call under test
		listener.afterCreateOrUpdate(null);
		
		verifyZeroInteractions(mockDao);
	}
	
	@Test
	public void testAfterCreateOrUpdateWithEmpty() {
		
		// Call under test
		listener.afterCreateOrUpdate(Collections.emptyList());
		
		verifyZeroInteractions(mockDao);
	}
	
	
}
