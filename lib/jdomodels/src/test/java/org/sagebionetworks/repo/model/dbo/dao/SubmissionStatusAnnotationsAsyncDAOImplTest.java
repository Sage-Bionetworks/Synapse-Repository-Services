package org.sagebionetworks.repo.model.dbo.dao;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SubmissionStatusAnnotationsAsyncDAOImplTest {
	
	private String subId = "123";

	private SubmissionStatusDAO mockSubmissionStatusDAO;
	private AnnotationsDAO mockSubStatusAnnoDAO;
	private SubmissionStatusAnnotationsAsyncDAOImpl testDao;
	private org.sagebionetworks.repo.model.annotation.Annotations ssAnnos;

	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{

		mockSubmissionStatusDAO = Mockito.mock(SubmissionStatusDAO.class);
		mockSubStatusAnnoDAO = Mockito.mock(AnnotationsDAO.class);
		
		// SubmissionStatus annos
		ssAnnos = new org.sagebionetworks.repo.model.annotation.Annotations();
		ssAnnos.setOwnerId(subId);
		ssAnnos.setOwnerParentId("456");
		SubmissionStatus status = new SubmissionStatus();
		status.setAnnotations(ssAnnos);
		status.setId(subId);
		when(mockSubmissionStatusDAO.get(Mockito.eq(subId))).thenReturn(status);
		
		testDao = new SubmissionStatusAnnotationsAsyncDAOImpl(
				mockSubmissionStatusDAO, mockSubStatusAnnoDAO);
	}
	
	@Test
	public void testUpdateSubmissionStatus() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		testDao.updateSubmissionStatus(subId);
		verify(mockSubStatusAnnoDAO).replaceAnnotations(Mockito.eq(ssAnnos));
	}
	
	@Test
	public void testDeleteSubmission() {
		testDao.deleteSubmission(subId);
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByOwnerId(Long.parseLong(subId));
	}
}
