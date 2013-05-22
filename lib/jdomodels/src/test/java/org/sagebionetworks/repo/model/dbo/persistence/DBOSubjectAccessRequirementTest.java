package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.migration.DBOSubjectAccessRequirementBackup;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;


public class DBOSubjectAccessRequirementTest {
	private static MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirementBackup> translator =
		( new DBOSubjectAccessRequirement()).getTranslator();

	/**
	 * Test restoration from Node
	 */
	@Test
	public void testTranslatorRoundtripNode() throws Exception { 
		DBOSubjectAccessRequirementBackup backup = new DBOSubjectAccessRequirementBackup();
		backup.setAccessRequirementId(101L);
		backup.setNodeId(987L);
		DBOSubjectAccessRequirement sar = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(RestrictableObjectType.ENTITY.toString(), sar.getSubjectType());
		assertEquals(backup.getNodeId(), sar.getSubjectId());
		assertEquals(backup.getAccessRequirementId(), sar.getAccessRequirementId());
		assertEquals(sar, translator.createDatabaseObjectFromBackup(
				translator.createBackupFromDatabaseObject(sar)));
	}

	/**
	 * Test restoration from SubjectID
	 */
	@Test
	public void testTranslatorRoundtripSubjectID() throws Exception { 
		DBOSubjectAccessRequirementBackup backup = new DBOSubjectAccessRequirementBackup();
		backup.setAccessRequirementId(101L);
		backup.setSubjectId(987L);
		backup.setSubjectType("EVALUATION");
		DBOSubjectAccessRequirement sar = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(RestrictableObjectType.EVALUATION.toString(), sar.getSubjectType());
		assertEquals(backup.getSubjectId(), sar.getSubjectId());
		assertEquals(backup.getAccessRequirementId(), sar.getAccessRequirementId());
		assertEquals(backup, translator.createBackupFromDatabaseObject(sar));
	}

}
