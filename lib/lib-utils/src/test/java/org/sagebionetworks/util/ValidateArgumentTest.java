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

	private static class T1 {
	}

	private static class T2 extends T1 {
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequireTypeNull() {
		ValidateArgument.requireType(null, T1.class, "t");
	}

	@Test
	public void testRequireTypeEquals(){
		ValidateArgument.requireType(new T1(), T1.class, "t");
	}

	@Test
	public void testRequireTypeSubClass() {
		ValidateArgument.requireType(new T2(), T1.class, "t");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequireTypeSuperClass() {
		ValidateArgument.requireType(new T1(), T2.class, "t");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequireTypeFail() {
		ValidateArgument.requireType(new Integer(0), T2.class, "t");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequiredNotEmpty_null(){
		ValidateArgument.requiredNotEmpty(null, "myField");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequiredNotEmpty_emptyString(){
		ValidateArgument.requiredNotEmpty("", "myField");
	}

	@Test
	public void testRequiredNotEmpty_NonEmptyString(){
		ValidateArgument.requiredNotEmpty("a", "myField");
	}
}
