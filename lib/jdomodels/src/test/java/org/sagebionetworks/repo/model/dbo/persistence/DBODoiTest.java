package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
public class DBODoiTest {

	@Test
	public void setNullUpdatedByToCreatedBy() {
		DBODoi otherDbo = new DBODoi();
		otherDbo.setUpdatedBy(100000L);

		DBODoi doi = new DBODoi();
		doi.setId(1000L);
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedBy(null); // This should be converted to 'created by' when null
		doi.setCreatedBy(500L);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(-1L);

		// Method under test
		DBODoi newDoi = otherDbo.getTranslator().createDatabaseObjectFromBackup(doi);

		assertNotNull(newDoi.getUpdatedBy());
		assertNotNull(newDoi.getCreatedBy());
		assertEquals(doi.getCreatedBy(), newDoi.getUpdatedBy());

		assertEquals(doi.getObjectVersion(), newDoi.getObjectVersion());
		assertEquals(doi.getCreatedBy(), newDoi.getCreatedBy());
		assertEquals(doi.getObjectId(), newDoi.getObjectId());
		assertEquals(doi.getObjectType(), newDoi.getObjectType());
		assertEquals(doi.getETag(), newDoi.getETag());
		assertEquals(doi.getDoiStatus(), newDoi.getDoiStatus());
		assertEquals(doi.getCreatedOn(), newDoi.getCreatedOn());
		assertEquals(doi.getUpdatedOn(), newDoi.getUpdatedOn());
	}
}
