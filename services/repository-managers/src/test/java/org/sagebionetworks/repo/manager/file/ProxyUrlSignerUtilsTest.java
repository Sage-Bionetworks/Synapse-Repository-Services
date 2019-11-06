package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;

public class ProxyUrlSignerUtilsTest {

	ProxyFileHandle proxyHandle;
	ProxyStorageLocationSettings proxyLocation;
	Date expiration;

	@Before
	public void before() {
		Long locationId = 123L;
		proxyHandle = new ProxyFileHandle();
		proxyHandle.setFileName("foo.txt");
		proxyHandle.setFilePath("/path/root/child");
		proxyHandle.setContentType("text/plain; charset=us-ascii");
		proxyHandle.setContentMd5("md5");
		proxyHandle.setContentSize(987L);
		proxyHandle.setStorageLocationId(locationId);

		proxyLocation = new ProxyStorageLocationSettings();
		proxyLocation.setStorageLocationId(locationId);
		proxyLocation.setProxyUrl("https://host.org:8080/prefix");
		proxyLocation.setSecretKey("Super Secret key to sign URLs with.");
		proxyLocation.setUploadType(UploadType.SFTP);

		expiration = new Date(123);
	}

	@Test
	public void testGeneratePresignedUrlAll() {
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals(
				"https://host.org:8080/prefix/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=91c52bb9b92a9067ded0434e81768be2f7720b9d",
				url);
	}
	
	@Test
	public void testGeneratePresignedUrlWithSlash() {
		proxyLocation.setProxyUrl("http://host.org/prefix/");
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertTrue(url.startsWith("http://host.org/prefix/sftp/path/root/child"));
	}
	
	@Test
	public void testGeneratePresignedPathWithNoSlash() {
		proxyHandle.setFilePath("path/root/child");
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertTrue(url.startsWith("https://host.org:8080/prefix/sftp/path/root/child"));
	}

	@Test
	public void testGeneratePresignedUrlNameNull() {
		proxyHandle.setFileName(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals(
				"https://host.org:8080/prefix/sftp/path/root/child"
				+ "?contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=38f70cf072d3f237c97b33495eff3e99a8cc8b52",
				url);
	}

	@Test
	public void testGeneratePresignedUrlContentTypeNull() {
		proxyHandle.setContentType(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals(
				"https://host.org:8080/prefix/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=fbf220fb0aad4edc6b389776415684c3a3f5ab74",
				url);
	}

	@Test
	public void testGeneratePresignedUrlMD5Null() {
		proxyHandle.setContentMd5(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals(
				"https://host.org:8080/prefix/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=336a52d1cc801856b16b8074791efa0710bd3ae5",
				url);
	}

	@Test
	public void testGeneratePresignedUrlSizeNull() {
		proxyHandle.setContentSize(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals(
				"https://host.org:8080/prefix/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&expiration=123&hmacSignature=f9857d219d86cb98b505419f30b5bb4461377dd0",
				url);
	}
	
	@Test
	public void testGeneratePresignedPathNoSlash() {
		// path does not start with slash and needs a trim.
		proxyHandle.setFilePath(" path/root/child\n");
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertTrue(url.startsWith("https://host.org:8080/prefix/sftp/path/root/child"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePresignedHandleNull() {
		proxyHandle = null;
		// Call under test
		ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePresignedPathNull() {
		proxyHandle.setFilePath(null);
		// Call under test
		ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePresignedSettingsNull() {
		proxyLocation = null;
		// Call under test
		ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePresignedSettingsHostNull() {
		proxyLocation.setProxyUrl(null);
		// Call under test
		ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePresignedSettingsTypeNull() {
		proxyLocation.setUploadType(null);
		// Call under test
		ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
	}
	
	@Test
	public void testGeneratePresignedHostPort() {
		// path does not start with slash and needs a trim.
		proxyLocation.setProxyUrl("https://hocalhost:8080");
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertTrue(url.startsWith("https://hocalhost:8080/sftp/path/root/child"));
	}

}
