package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import static org.junit.jupiter.api.Assertions.*;

public class DBOSubmissionTest {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Submission.class).build();

	@Test
	public void testMigrationTranslator() throws Exception {
		ResearchProject researchProject = new ResearchProject();
		String id = "101";
		researchProject.setId(id);
		Submission submission = new Submission();
		submission.setResearchProjectSnapshot(researchProject);
		DBOSubmission backup = new DBOSubmission();
		backup.setAccessRequirementId(1L);
		backup.setCreatedBy(2L);
		backup.setCreatedOn(3L);
		backup.setDataAccessRequestId(4L);
		backup.setEtag("etag");
		backup.setId(5L);
		backup.setSubmissionSerialized(JDOSecondaryPropertyUtils.compressObject(X_STREAM, submission));
		
		// Note, we have NOT set the research project id in backup
		assertNull(backup.getResearchProjectId());
		
		DBOSubmission dbo = (new DBOSubmission()).getTranslator().createDatabaseObjectFromBackup(backup);
		
		assertEquals(Long.parseLong(id), dbo.getResearchProjectId().longValue());
		
		assertEquals(dbo, backup);
	}

}
