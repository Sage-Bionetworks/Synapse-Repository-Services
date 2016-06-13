package org.sagebionetworks.repo.model.util;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.DockerNameUtil;

public class DockerNameUtilTest {

	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^"+DockerNameUtil.hostnameRegexp+"$");
	
	private static void assertIsHostName(String name) {
		assertTrue(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsNOTHostName(String name) {
		assertFalse(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsRepoPath(String name) {
		try {
			DockerNameUtil.validateName(name);
			// pass
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

	private static void assertIsNOTRepoPath(String name) {
		try {
			DockerNameUtil.validateName(name);
			fail();
		} catch (IllegalArgumentException e) {
			// pass
		}
	}

	@Test
	public void testHostRegexp() {
		assertIsHostName("docker.syn-apse.org");
		assertIsHostName("docker.syn-apse.org:5000");
		assertIsHostName("127.0.0.1:443");
		assertIsHostName("localhost");
		assertIsHostName("www.dockerhub.com");
		assertIsHostName("syn-AP-se");
		assertIsHostName("syn.apse");
		assertIsNOTHostName("syn-");
		assertIsNOTHostName("https://www.dockerhub.com");
	}

	
	@Test
	public void testValidateName() {
		// paths with no host
		assertIsRepoPath("dockerhubuname/d0ckerhubrep0");
		assertIsRepoPath("dockerhubuname/d0cker-hub_rep0");
		assertIsRepoPath("a/path/with/lots/0f/parts");
		assertIsNOTRepoPath("/apathwith/leadingslash");
		assertIsNOTRepoPath("-apathwith/leadingdash");
		assertIsNOTRepoPath("apathwith/trailingslash/");
		assertIsNOTRepoPath("apathwith/UPPERcase");
		
		// paths with host
		assertIsRepoPath("docker.syn-apse.org/prject/repo");
		assertIsRepoPath("docker.synapse.org:443/prject/repo");
		assertIsNOTRepoPath("docker.synapse.org:443");
		assertIsNOTRepoPath("docker.synapse.org:443/");
		assertIsNOTRepoPath("https://docker.synapse.org:443/prject/repo");
	}
	
	@Test
	public void testGetRegistryHost() {
		assertEquals("docker.synapse.org", 
				DockerNameUtil.getRegistryHost("docker.synapse.org/foo/bar"));
		assertEquals("docker.synapse.org:443", 
				DockerNameUtil.getRegistryHost("docker.synapse.org:443/foo/bar"));
		assertNull(DockerNameUtil.getRegistryHost("foo/bar"));
	}

	@Test
	public void testGetRegistryHostSansPort() {
		assertEquals("docker.synapse.org", DockerNameUtil.getRegistryHostSansPort("docker.synapse.org:443"));
		assertEquals("docker.synapse.org", DockerNameUtil.getRegistryHostSansPort("docker.synapse.org"));
		assertNull(DockerNameUtil.getRegistryHostSansPort(null));
	}

}
