package org.sagebionetworks.util;

import org.junit.Test;

public class ValidateArgumentTest {

	@Test
	public void testRegularUrl() {
		ValidateArgument.validUrl("http://somewhere.com/dir#task?arg=3");
	}

	@Test
	public void testSftpUrl() {
		ValidateArgument.validUrl("sftp://now.somewhere.com/dir#task?arg=3");
	}

	@Test
	public void testFileUrl() {
		ValidateArgument.validUrl("file:///dir1/dir#task?arg=3");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWindowsFileUrl() {
		// backslashes are not allowed in URLs. Microsoft of course flouts that rule.
		ValidateArgument.validUrl("file:///c:\\users\\admini~1\\appdata\\local\\temp\\2\\tmp_bh7qh.txt");
	}

	@Test
	public void testWindowsFileUrl2() {
		ValidateArgument.validUrl("file:///c:/users/admini~1/appdata/local/temp/2/tmp_bh7qh.txt");
	}
	
	@Test
	public void testWindowsPLFM_3226() {
		ValidateArgument.validUrl("ftp://anonymous:anonymous@ftp.ncbi.nih.gov/pub/geo/DATA/supplementary/series/fake/FAKE.tar");
	}
}
