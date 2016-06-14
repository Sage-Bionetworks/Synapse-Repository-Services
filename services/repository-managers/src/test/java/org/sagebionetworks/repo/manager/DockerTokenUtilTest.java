package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

public class DockerTokenUtilTest {

	@Test
	public void testTokenGeneration() throws Exception {
		String userName = "userName";
		String type = "repository";
		String registry = "docker.synapse.org";
		String repository = "syn12345/myrepo";
		List<String> actions = Arrays.asList(new String[] {"push", "pull"});

		long now = 1465768785754L;
		String uuid = "8b263df7-dd04-4afe-8366-64f882e0942d";

		String token = DockerTokenUtil.createToken(userName, type, registry, repository, actions, now, uuid);

		// the 'expected' token was verified to work with the Docker registry
		// note: the token is dependent on the credentials in stack.properties
		// if those credentials are changed, then this test string must be regenerated
		String[] pieces = token.split("\\.");
		assertEquals(3, pieces.length);
		String expectedHeadersBase64 = "eyJ0eXAiOiJKV1QiLCJraWQiOiJGV09aOjZKTlk6T1VaNTpCSExBOllXSkk6UEtMNDpHNlFSOlhDTUs6M0JVNDpFSVhXOkwzUTc6Vk1JUiIsImFsZyI6IkVTMjU2In0";
		assertEquals(expectedHeadersBase64, pieces[0]);
		String expectedClaimSetBase64 = "eyJpc3MiOiJ3d3cuc3luYXBzZS5vcmciLCJhdWQiOiJkb2NrZXIuc3luYXBzZS5vcmciLCJleHAiOjE0NjU3Njg5MDUsIm5iZiI6MTQ2NTc2ODY2NSwiaWF0IjoxNDY1NzY4Nzg1LCJqdGkiOiI4YjI2M2RmNy1kZDA0LTRhZmUtODM2Ni02NGY4ODJlMDk0MmQiLCJzdWIiOiJ1c2VyTmFtZSIsImFjY2VzcyI6W3sibmFtZSI6InN5bjEyMzQ1L215cmVwbyIsInR5cGUiOiJyZXBvc2l0b3J5IiwiYWN0aW9ucyI6WyJwdXNoIiwicHVsbCJdfV19";
		assertEquals(expectedClaimSetBase64, pieces[1]);
		assertTrue(pieces[2].length()>0); // since signature changes every time, we can't hard code an 'expected' value
	}

}
