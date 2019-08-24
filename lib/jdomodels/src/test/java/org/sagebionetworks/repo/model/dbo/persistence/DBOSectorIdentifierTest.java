package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.sagebionetworks.EncryptionUtilsSingleton;
import org.sagebionetworks.StackEncrypter;

public class DBOSectorIdentifierTest {
	private static StackEncrypter STACK_ENCRYPTER = EncryptionUtilsSingleton.singleton();

	@Test
	public void testCreateDatabaseObjectFromBackup() throws Exception {
		DBOSectorIdentifier backup = new DBOSectorIdentifier();
		backup.setId(101L);
		String secret = UUID.randomUUID().toString();
		String encryptedSecret = STACK_ENCRYPTER.encryptAndBase64EncodeStringWithStackKey(secret);
		backup.setEncryptedSecret(encryptedSecret);
		
		// method under test
		DBOSectorIdentifier dbo = (new DBOSectorIdentifier()).getTranslator().createDatabaseObjectFromBackup(backup);
		
		assertEquals(new Long(101L), dbo.getId());
		assertEquals(encryptedSecret, dbo.getEncryptedSecret());
	}

}
