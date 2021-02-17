package org.sagebionetworks.repo.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class DockerNameUtilTest {

	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^" + DockerNameUtil.domainName + "$");

	private static void assertIsHostName(String name) {
		assertTrue(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsNOTHostName(String name) {
		assertFalse(HOSTNAME_PATTERN.matcher(name).matches());
	}

	private static void assertIsRepoName(String name) {
		DockerNameUtil.validateName(name);
	}

	private static void assertIsNOTRepoName(String name) {
		assertThrows(IllegalArgumentException.class, () -> {
			DockerNameUtil.validateName(name);
		});
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
		assertIsNOTHostName("docker.syn-apse.org:500000000000000000000000000000000f");
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
		// another catastrophic backtracking case.
		assertIsNOTRepoName("docker.synapse.org/syn8119917/abcdefghijklmnopqurstuvwxyz1234-");
		assertIsNOTRepoName("abcdefghijklmnopqurstuvwxyz1234_");
	}

	@Test
	public void testGetRegistryHost() {
		assertEquals("docker.synapse.org", DockerNameUtil.getRegistryHost("docker.synapse.org/foo/bar"));
		assertEquals("docker.synapse.org:443", DockerNameUtil.getRegistryHost("docker.synapse.org:443/foo/bar"));
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

	@Test
	public void testGetParentIdFromRepositoryPathNoSynPrefix() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			DockerNameUtil.getParentIdFromRepositoryPath("123");
		}).getMessage();
		assertEquals("Repository path must start with 'syn'.", message);
	}

	@Test
	public void testGetParentIdFromRepositoryPathNoNumberSuffix() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			DockerNameUtil.getParentIdFromRepositoryPath("synARepo");
		}).getMessage();
		assertEquals("Repository path must start with project ID: 'syn', followed by a number.", message);
	}
	
	@Test
	public void testDomainName() {
		Pattern pattern = Pattern.compile(DockerNameUtil.domainName);
		assertTrue(pattern.matcher("a.b").matches());
		assertTrue(pattern.matcher("a.b.c").matches());
		assertTrue(pattern.matcher("a-a.bb.cc").matches());
		assertTrue(pattern.matcher("a-a.bb.cc:443").matches());
		assertFalse(pattern.matcher("abc.").matches());
		assertFalse(pattern.matcher("a-b.").matches());
		// Without Possessive quantifiers these two would cause catastrophic backtracking
		assertFalse(pattern.matcher("abcdefghijklmnopqurstuvwxyz1234.").matches());
		assertFalse(pattern.matcher("abcdefghijklmnopqurstuvwxyz1234-").matches());
	}
	
	@Test
	public void testNameComponent() {
		Pattern pattern = Pattern.compile(DockerNameUtil.nameComponentRegexp);
		assertTrue(pattern.matcher("a").matches());
		assertTrue(pattern.matcher("a.b").matches());
		assertTrue(pattern.matcher("aaa.bbb.ccc").matches());
		assertTrue(pattern.matcher("a-a.b-b.c-c").matches());
		assertTrue(pattern.matcher("a-b").matches());
		// one underscore is allowed
		assertTrue(pattern.matcher("a_b").matches());
		// two underscore is allowed
		assertTrue(pattern.matcher("a__b").matches());
		// more than two underscore is not allowed
		assertFalse(pattern.matcher("a___b").matches());
		assertTrue(pattern.matcher("a--b").matches());
		assertFalse(pattern.matcher("_a").matches());
		assertFalse(pattern.matcher("a_").matches());
		assertFalse(pattern.matcher(".a").matches());
		assertFalse(pattern.matcher("a.").matches());
		assertFalse(pattern.matcher("-a").matches());
		assertFalse(pattern.matcher("a-").matches());
		// Without Possessive quantifiers these would cause catastrophic backtracking
		assertFalse(pattern.matcher("abcdefghijklmnopqurstuvwxyz1234-").matches());
		assertFalse(pattern.matcher("abcdefghijklmnopqurstuvwxyz1234.").matches());
		assertFalse(pattern.matcher("abcdefghijklmnopqurstuvwxyz1234_").matches());
		assertFalse(pattern.matcher("----------------------------------a").matches());
	}

}
