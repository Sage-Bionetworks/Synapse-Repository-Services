package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
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
		proxyHandle.setProxyHost("host.org");
		proxyHandle.setContentType("text/plain; charset=us-ascii");
		proxyHandle.setContentMd5("md5");
		proxyHandle.setContentSize(987L);
		proxyHandle.setStorageLocationId(locationId);

		proxyLocation = new ProxyStorageLocationSettings();
		proxyLocation.setStorageLocationId(locationId);
		proxyLocation.setProxyHost(proxyHandle.getProxyHost());
		proxyLocation.setSecretKey("Super Secret key to sign URLs with.");

		expiration = new Date(123);
	}

	@Test
	public void testGeneratePresignedUrlAll() {
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/path/root/child" + "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5" + "&contentSize=987" + "&expiration=123"
				+ "&hmacSignature=1fbd9816a8a54f1db2e937a0c00a46051a1f38d6",
				url);
	}

	@Test
	public void testGeneratePresignedUrlNameNull() {
		proxyHandle.setFileName(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/path/root/child"
				+ "?contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5" + "&contentSize=987" + "&expiration=123"
				+ "&hmacSignature=bc6a45921db91baf5878a4d7437a8afbb9e11965",
				url);
	}

	@Test
	public void testGeneratePresignedUrlContentTypeNull() {
		proxyHandle.setContentType(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/path/root/child" + "?fileName=foo.txt"
				+ "&contentMD5=md5" + "&contentSize=987" + "&expiration=123"
				+ "&hmacSignature=ab10dd19178a980c0fccee0a31964a76e4862463",
				url);
	}

	@Test
	public void testGeneratePresignedUrlMD5Null() {
		proxyHandle.setContentMd5(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/path/root/child" + "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentSize=987" + "&expiration=123"
				+ "&hmacSignature=107f9824d1d1ca4db10e8cf9a6c4bf2c082a0729",
				url);
	}

	@Test
	public void testGeneratePresignedUrlSizeNull() {
		proxyHandle.setContentSize(null);
		// Call under test
		String url = ProxyUrlSignerUtils.generatePresignedUrl(proxyHandle,
				proxyLocation, expiration);
		assertEquals("https://host.org/path/root/child" + "?fileName=foo.txt"
				+ "&contentType=text%2Fplain%3B+charset%3Dus-ascii"
				+ "&contentMD5=md5" + "&expiration=123"
				+ "&hmacSignature=41b47c63904bf6b2287bd391cc057cea6de30df1",
				url);
	}

}
