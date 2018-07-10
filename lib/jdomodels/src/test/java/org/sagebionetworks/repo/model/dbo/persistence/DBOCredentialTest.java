package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;

public class DBOCredentialTest {

	MigratableTableTranslation<DBOCredential, DBOCredentialBackup> translator;

	@Before
	public void before() {
		translator = new DBOCredential().getTranslator();
	}

	@Test
	public void testIgnoreOnRestoreAdmin() {
		DBOCredentialBackup admin = new DBOCredentialBackup();
		admin.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// call under test
		assertTrue(translator.ignoreOnRestore(admin));
	}

	@Test
	public void testIgnoreOnRestoreNonAdmin() {
		DBOCredentialBackup admin = new DBOCredentialBackup();
		admin.setPrincipalId(new Long(123));
		// call under test
		assertFalse(translator.ignoreOnRestore(admin));
	}
}
