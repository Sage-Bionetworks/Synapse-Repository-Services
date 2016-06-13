package org.sagebionetworks.repo.model.util;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.DockerNameUtil;

public class DockerNameUtilTest {

	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^"+DockerNameUtil.hostnameRegexp+"$");
	
	public static void assertIsHostName(String name) {
		assertTrue(HOSTNAME_PATTERN.matcher(name).matches());
	}

	public static void assertIsNOTHostName(String name) {
		assertFalse(HOSTNAME_PATTERN.matcher(name).matches());
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
	
	private static final Pattern REPO_PATH_PATTERN = Pattern.compile("^"+DockerNameUtil.hostnameRegexp+"$");
	
	public static void assertIsRepoPath(String name) {
		assertTrue(REPO_PATH_PATTERN.matcher(name).matches());
	}

	public static void assertIsNOTRepoPath(String name) {
		assertFalse(REPO_PATH_PATTERN.matcher(name).matches());
	}

	
	
	@Test
	public void testGetRegistryHostAndName() {
		assertEquals("docker.synapse.org:443", 
				DockerNameUtil.getRegistryHost("docker.synapse.org:443/foo/bar"));
		assertNull(DockerNameUtil.getRegistryHost("foo/bar"));
	}



}
