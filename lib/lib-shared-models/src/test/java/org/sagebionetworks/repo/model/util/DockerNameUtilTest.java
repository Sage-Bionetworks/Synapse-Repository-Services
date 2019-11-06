package org.sagebionetworks.repo.model.util;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

public class DockerNameUtilTest {

	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^"+DockerNameUtil.hostnameRegexp+"$");
	
	private static void assertIsHostName(String name) {
		assertTrue(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsNOTHostName(String name) {
		assertFalse(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsRepoName(String name) {
		try {
			DockerNameUtil.validateName(name);
			// pass
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

	private static void assertIsNOTRepoName(String name) {
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
		// names with no host
		assertIsRepoName("dockerhubuname/d0ckerhubrep0");
		assertIsRepoName("dockerhubuname/d0cker-hub_rep0");
		assertIsRepoName("a/path/with/lots/0f/parts");
		assertIsNOTRepoName("/apathwith/leadingslash");
		assertIsNOTRepoName("-apathwith/leadingdash");
		assertIsNOTRepoName("apathwith/trailingslash/");
		assertIsNOTRepoName("apathwith/UPPERcase");
		
		// names with host
		assertIsRepoName("docker.syn-apse.org/prject/repo");
		assertIsRepoName("docker.synapse.org:443/prject/repo");
		assertIsNOTRepoName("docker.synapse.org:443");
		assertIsNOTRepoName("docker.synapse.org:443/");
		assertIsNOTRepoName("https://docker.synapse.org:443/prject/repo");
		
		// black hole, as discovered in PLFM-4298
		assertIsNOTRepoName("docker.synapse.org/syn8119917/preprocess-reducedata-keras/");
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
	
	@Test
	public void testGetParentIdFromRepositoryPath() {
		assertEquals("syn123", DockerNameUtil.getParentIdFromRepositoryPath("syn123/my/repo"));
		assertEquals("syn123", DockerNameUtil.getParentIdFromRepositoryPath("syn123"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetParentIdFromRepositoryPathNoSynPrefix() {
		DockerNameUtil.getParentIdFromRepositoryPath("123");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetParentIdFromRepositoryPathNoNumberSuffix() {
		DockerNameUtil.getParentIdFromRepositoryPath("synARepo");
	}
	

}
