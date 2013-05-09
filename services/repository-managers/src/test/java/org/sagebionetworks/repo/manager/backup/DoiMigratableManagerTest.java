package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.DoiMigratableDao;
import org.sagebionetworks.repo.model.dbo.dao.DBODoiMigratableDaoImpl;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.springframework.test.util.ReflectionTestUtils;

public class DoiMigratableManagerTest {

	@Test
	public void testRoundtrip() throws Exception {

		final Long createdBy = 1L;
		final Timestamp createdOn = new Timestamp((new Date()).getTime());
		final DoiObjectType doiObjectType = DoiObjectType.ENTITY;
		final DoiStatus doiStatus = DoiStatus.CREATED;
		final String eTag = "eTag";
		final Long id = 2L;
		final Long objectId = 3L;
		final Long objectVersion = 4L;
		final Timestamp updatedOn = new Timestamp((new Date()).getTime());
		Doi doi = new Doi();
		doi.setCreatedBy(createdBy.toString());
		doi.setCreatedOn(createdOn);
		doi.setDoiObjectType(doiObjectType);
		doi.setDoiStatus(doiStatus);
		doi.setEtag(eTag);
		doi.setId(id.toString());
		doi.setObjectId(objectId.toString());
		doi.setObjectVersion(objectVersion);
		doi.setUpdatedOn(updatedOn);

		DoiMigratableDao mockDao = mock(DBODoiMigratableDaoImpl.class);
		when(mockDao.get(doi.getId())).thenReturn(doi);
		DoiMigratableManager mgr = new DoiMigratableManager();
		ReflectionTestUtils.setField(mgr, "doiMigratableDao", mockDao);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mgr.writeBackupToOutputStream(doi.getId(), out);
		String str = new String(out.toByteArray());
		out.close();

		InputStream in = new ByteArrayInputStream(str.getBytes());
		String idRestored = mgr.createOrUpdateFromBackupStream(in);
		assertEquals(id.toString(), idRestored);
	}
}
