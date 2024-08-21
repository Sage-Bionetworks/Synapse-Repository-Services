package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProvider;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProviderImpl;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class MigrationTypeProviderImplTest {

	private DBOCredential credentialOne;
	private DBOCredential credentialTwo;
	private List<MigratableDatabaseObject<?, ?>> credentials;
	private MigrationTypeProvider typeProvider;

	@BeforeEach
	public void before() {
		credentialOne = new DBOCredential();
		credentialOne.setEtag("etag");
		credentialOne.setExpiresOn(new Date(10011));
		credentialOne.setPassHash("adminHash");
		credentialOne.setSecretKey("adminKey");
		credentialOne.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		credentialTwo = new DBOCredential();
		credentialTwo.setEtag("etag2");
		credentialTwo.setExpiresOn(new Date(20011));
		credentialTwo.setPassHash("hashTwo");
		credentialTwo.setSecretKey("keyTwo");
		credentialTwo.setPrincipalId(456L);
		credentials = Lists.newArrayList(credentialOne, credentialTwo);

		typeProvider = new MigrationTypeProviderImpl(List.of(new DBONode(), new DBORevision(),
				new DBOAccessControlList(), new DBOResourceAccess(), new DBOResourceAccessType(), new DBOCredential()));
	}

	@Test
	public void testWriteObjects() {
		StringWriter writer = new StringWriter();

		// call under test
		typeProvider.writeObjects(credentials, writer);
		assertEquals(
				"[{\"principalId\":1,\"etag\":\"etag\",\"expiresOn\":10011,\"passHash\":\"adminHash\",\"secretKey\":\"adminKey\"},{\"principalId\":456,\"etag\":\"etag2\",\"expiresOn\":20011,\"passHash\":\"hashTwo\",\"secretKey\":\"keyTwo\"}]",
				new JSONArray(writer.toString()).toString(0));
	}

	@Test
	public void testWriteObjectsWithEmptyList() {
		StringWriter writer = new StringWriter();

		// call under test
		typeProvider.writeObjects(Collections.emptyList(), writer);
		assertEquals("", writer.toString());
	}

	@Test
	public void testWriteObjectsWithEmptyObject() {
		StringWriter writer = new StringWriter();

		// call under test
		typeProvider.writeObjects(List.of(new DBOCredential(), credentialOne), writer);
		assertEquals(
				"[{\"principalId\":1,\"etag\":\"etag\",\"expiresOn\":10011,\"passHash\":\"adminHash\",\"secretKey\":\"adminKey\"}]",
				new JSONArray(writer.toString()).toString(0));
	}

	@Test
	public void testWriteObjectsWithAllEmptyObject() {
		StringWriter writer = new StringWriter();

		// call under test
		typeProvider.writeObjects(List.of(new DBOCredential(), new DBOCredential()), writer);
		assertEquals("", writer.toString());
	}
}
