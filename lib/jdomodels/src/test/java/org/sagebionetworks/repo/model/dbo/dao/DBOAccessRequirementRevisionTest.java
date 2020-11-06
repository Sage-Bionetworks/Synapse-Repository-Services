package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;

public class DBOAccessRequirementRevisionTest {
	
	@Test
	public void testCreateDatabaseObjectFromBackupWithNullIsIDURequired() {
		DBOAccessRequirementRevision backup = new DBOAccessRequirementRevision();
		
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setIsIDURequired(null);
		
		AccessRequirementUtils.copyToSerializedField(ar, backup);
		
		// Call under test
		backup = backup.getTranslator().createDatabaseObjectFromBackup(backup);
		
		ManagedACTAccessRequirement result = (ManagedACTAccessRequirement) AccessRequirementUtils.copyFromSerializedField(backup);
		
		assertTrue(result.getIsIDURequired());
	}

}
