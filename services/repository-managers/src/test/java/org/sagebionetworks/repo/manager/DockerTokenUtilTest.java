package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;
import static org.sagebionetworks.repo.model.docker.RegistryEventAction.push;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.util.Base64;
import org.junit.Test;

public class DockerTokenUtilTest {
	
	@Test
	public void testTokenGeneration() throws Exception {
		String userName = "userName";
		String type = "repository";
		String registry = "docker.synapse.org";
		String repository1 = "syn12345/myrepo";
		String repository2 = "syn12345/myotherrepo";
		Set<String> pushAndPullActions = new HashSet<String>(Arrays.asList(new String[] {push.name(), pull.name()}));
		Set<String> pullActionOnly = new HashSet<String>(Arrays.asList(new String[] {pull.name()}));
		long now = 1465768785754L;
		long interval = 120;
		String uuid = "8b263df7-dd04-4afe-8366-64f882e0942d";
		
		List<DockerScopePermission> permissions = new ArrayList<DockerScopePermission>();
		permissions.add(new DockerScopePermission(type, repository1, pushAndPullActions));
		permissions.add(new DockerScopePermission(type, repository2, pullActionOnly));

		
		String token = DockerTokenUtil.createDockerAuthorizationToken(userName, registry, permissions, now, uuid);

		// the 'expected' token was verified to work with the Docker registry
		// note: the token is dependent on the credentials in stack.properties
		// if those credentials are changed, then this test string must be regenerated
		String[] pieces = token.split("\\.");
		assertEquals(3, pieces.length);
		
		String expectedHeaderString = "{\"typ\":\"JWT\",\"kid\":\""+DockerTokenUtil.DOCKER_AUTHORIZATION_PUBLIC_KEY_ID+"\",\"alg\":\"ES256\"}";
		String expectedHeadersBase64 = Base64.encodeBase64URLSafeString( expectedHeaderString.getBytes());
		assertEquals(expectedHeadersBase64, pieces[0]);
		
		StringBuilder expectedClaimSetStringBuilder = new StringBuilder("{\"iss\":\"www.synapse.org\",\"aud\":\"docker.synapse.org\",\"exp\":");
		expectedClaimSetStringBuilder.append(now / 1000 + interval);
		expectedClaimSetStringBuilder.append(",\"nbf\":");
		expectedClaimSetStringBuilder.append(now / 1000 - interval);
		expectedClaimSetStringBuilder.append(",\"iat\":");
		expectedClaimSetStringBuilder.append(now / 1000);
		expectedClaimSetStringBuilder.append(",\"jti\":\"");
		expectedClaimSetStringBuilder.append(uuid);
		expectedClaimSetStringBuilder.append("\",\"sub\":\"");
		expectedClaimSetStringBuilder.append(userName);
		expectedClaimSetStringBuilder.append("\",\"access\":[{\"name\":\"");
		expectedClaimSetStringBuilder.append(repository1);
		expectedClaimSetStringBuilder.append("\",\"type\":\"");
		expectedClaimSetStringBuilder.append(type);
		expectedClaimSetStringBuilder.append("\",\"actions\":[\"pull\",\"push\"]},{\"name\":\"");
		expectedClaimSetStringBuilder.append(repository2);
		expectedClaimSetStringBuilder.append("\",\"type\":\"");
		expectedClaimSetStringBuilder.append(type);
		expectedClaimSetStringBuilder.append("\",\"actions\":[\"pull\"]}]}");
		
		String expectedClaimSetBase64 = Base64.encodeBase64URLSafeString( expectedClaimSetStringBuilder.toString().getBytes());
		assertEquals(expectedClaimSetBase64, pieces[1]);
		assertTrue(pieces[2].length()>0); // since signature changes every time, we can't hard code an 'expected' value
	}

}
