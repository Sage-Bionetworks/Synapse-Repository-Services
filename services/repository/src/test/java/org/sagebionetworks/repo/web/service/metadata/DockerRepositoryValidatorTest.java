package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;

public class DockerRepositoryValidatorTest {

	DockerRepositoryValidator provider;
	
	public static boolean nameStartsWithHost(String name) {
		Pattern pattern = Pattern.compile("^"+DockerRepositoryValidator.hostnameRegexp);
		return pattern.matcher(name).matches();
	}

	@Test
	public void testHostRegexp() {
		assertTrue(nameStartsWithHost("docker.syn-apse.org"));
		assertTrue(nameStartsWithHost("docker.syn-apse.org:5000"));
		assertTrue(nameStartsWithHost("127.0.0.1:443"));
		assertTrue(nameStartsWithHost("localhost"));
		assertTrue(nameStartsWithHost("www.dockerhub.com"));
		assertTrue(nameStartsWithHost("syn-AP-se"));
		assertTrue(nameStartsWithHost("syn.apse"));
		
		assertFalse(nameStartsWithHost("syn-"));
		assertFalse(nameStartsWithHost("https://www.dockerhub.com"));
	}
	
	@Test
	public void testGetRegistryHostAndName() {
		assertEquals(Arrays.asList(new String[]{"docker.synapse.org:443", "foo/bar"}), 
				DockerRepositoryValidator.getRegistryHost("docker.synapse.org:443/foo/bar"));
		assertEquals(Arrays.asList(new String[]{null, "foo/bar"}), 
				DockerRepositoryValidator.getRegistryHost("foo/bar"));
	}

}
