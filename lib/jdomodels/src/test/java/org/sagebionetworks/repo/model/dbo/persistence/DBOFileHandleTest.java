package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.TemporaryCode;

class DBOFileHandleTest {


	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex_nullObject(){
		DBOFileHandle fileHandle = null;
		DBOFileHandle.base64MD5ToHex(fileHandle);
		assertNull(fileHandle);
	}


	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex_nullMd5(){
		DBOFileHandle fileHandle = new DBOFileHandle();
		fileHandle.setContentMD5(null);

		DBOFileHandle.base64MD5ToHex(fileHandle);

		assertNull(fileHandle.getContentMD5());
	}

	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex_lengthTooLong(){
		DBOFileHandle fileHandle = new DBOFileHandle();
		String hexMD5 = "1a79a4d60de6718e8e5b326e338ae533";
		fileHandle.setContentMD5(hexMD5);

		DBOFileHandle.base64MD5ToHex(fileHandle);

		assertEquals(hexMD5, fileHandle.getContentMD5());
	}

	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex_notBase64(){
		DBOFileHandle fileHandle = new DBOFileHandle();
		String notBase64 = "some string of length 24";
		fileHandle.setContentMD5(notBase64);

		DBOFileHandle.base64MD5ToHex(fileHandle);

		//nothing gets changed if not hex
		assertEquals(notBase64, fileHandle.getContentMD5());
	}

	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex_Base64butInvalidMd5(){
		DBOFileHandle fileHandle = new DBOFileHandle();
		//decodes into: asdfasdfasdf???qw
		String base64InvalidMd5 = "YXNkZmFzZGZhc2RmPz8/cXc=";
		fileHandle.setContentMD5(base64InvalidMd5);

		DBOFileHandle.base64MD5ToHex(fileHandle);

		//nothing gets changed if not hex
		assertEquals(base64InvalidMd5, fileHandle.getContentMD5());
	}


	@TemporaryCode(author = "zdong", comment = "Single stack migration change to be removed after stack 293")
	@Test
	public void testBase64MD5ToHex(){
		DBOFileHandle fileHandle = new DBOFileHandle();
		String base64MD5 = "Gnmk1g3mcY6OWzJuM4rlMw==";
		fileHandle.setContentMD5(base64MD5);


		DBOFileHandle.base64MD5ToHex(fileHandle);


		String expectedHexMD5 = "1a79a4d60de6718e8e5b326e338ae533";
		assertEquals(expectedHexMD5, fileHandle.getContentMD5());
	}

}