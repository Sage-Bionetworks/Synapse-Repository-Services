package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils.writeSerializedField;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.Request;

public class DBORequestTest {

	@Test
	public void testCreateDatabaseObjectFromBackupWithNullColumnAndEnvelopeInBlob() {
		Request request = new Request();
		request.setEDucSignatureEnvelopeId("env-123");

		DBORequest dbo = new DBORequest();
		dbo.setId(1L);
		dbo.setRequestSerialized(writeSerializedField(request));
		dbo.setEDucEnvelopeId(null);

		// call under test
		DBORequest result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertEquals("env-123", result.getEDucEnvelopeId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithColumnAlreadySet() {
		Request request = new Request();
		request.setEDucSignatureEnvelopeId("env-123");

		DBORequest dbo = new DBORequest();
		dbo.setId(1L);
		dbo.setRequestSerialized(writeSerializedField(request));
		dbo.setEDucEnvelopeId("env-existing");

		// call under test
		DBORequest result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertEquals("env-existing", result.getEDucEnvelopeId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithNoEnvelopeInBlob() {
		Request request = new Request();

		DBORequest dbo = new DBORequest();
		dbo.setId(1L);
		dbo.setRequestSerialized(writeSerializedField(request));
		dbo.setEDucEnvelopeId(null);

		// call under test
		DBORequest result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertNull(result.getEDucEnvelopeId());
	}
}
