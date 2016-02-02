package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;

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
		proxyLocation.setProxyHost("host.org");
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
				"https://host.org/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=4047296002a817e44205bedf127bbc02da51f3a4",
				url);
	}

	@Test
	public void testGeneratePresignedUrlNameNull() {
		proxyHandle.setFileName(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/sftp/path/root/child"
				+ "?contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=e5f6f70ce59a16c9137702c54b7473b06ef689d0",
				url);
	}

	@Test
	public void testGeneratePresignedUrlContentTypeNull() {
		proxyHandle.setContentType(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=500ed37e40d9b10f6ae449ad735a67ea8ba979bc",
				url);
	}

	@Test
	public void testGeneratePresignedUrlMD5Null() {
		proxyHandle.setContentMd5(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=689eae59c63a8c9f083417697cdd502db90bc069",
				url);
	}

	@Test
	public void testGeneratePresignedUrlSizeNull() {
		proxyHandle.setContentSize(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&expiration=123"
				+ "&hmacSignature=8f199e9c0bc0470ef0ea8a5cd3f7a878373cb4a6",
				url);
	}
	
	@Test
	public void testGeneratePresignedPathNoSlash() {
		// path does not start with slash and needs a trim.
		proxyHandle.setFilePath(" path/root/child\n");;
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/sftp/path/root/child"
				+ "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5"
				+ "&contentSize=987"
				+ "&expiration=123"
				+ "&hmacSignature=4047296002a817e44205bedf127bbc02da51f3a4",
				url);
	}

}
