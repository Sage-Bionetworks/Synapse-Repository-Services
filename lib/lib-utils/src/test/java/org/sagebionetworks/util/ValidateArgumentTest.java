package org.sagebionetworks.util;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidateArgumentTest {

	@Test
	public void testRegularUrl() {
		ValidateArgument.validExternalUrl("http://somewhere.com/dir#task?arg=3");
	}

	@Test
	public void testSftpUrl() {
		ValidateArgument.validExternalUrl("sftp://now.somewhere.com/dir#task?arg=3");
	}

	@Test
	public void testFileUrl() {
		ValidateArgument.validExternalUrl("file:///dir1/dir#task?arg=3");
	}

	@Test
	public void testWindowsFileUrl() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// backslashes are not allowed in URLs. Microsoft of course flouts that rule.
			ValidateArgument.validExternalUrl("file:///c:\\users\\admini~1\\appdata\\local\\temp\\2\\tmp_bh7qh.txt");
		});
	}

	@Test
	public void testWindowsFileUrl2() {
		ValidateArgument.validExternalUrl("file:///c:/users/admini~1/appdata/local/temp/2/tmp_bh7qh.txt");
	}

	@Test
	public void testWindowsPLFM_3226() {
		ValidateArgument.validExternalUrl("ftp://anonymous:anonymous@ftp.ncbi.nih.gov/pub/geo/DATA/supplementary/series/fake/FAKE.tar");
	}

	private static class T1 {
	}

	private static class T2 extends T1 {
	}

	@Test
	public void testRequireTypeNull() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requireType(null, T1.class, "t");
		});
	}

	@Test
	public void testRequireTypeEquals() {
		ValidateArgument.requireType(new T1(), T1.class, "t");
	}

	@Test
	public void testRequireTypeSubClass() {
		ValidateArgument.requireType(new T2(), T1.class, "t");
	}

	@Test
	public void testRequireTypeSuperClass() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requireType(new T1(), T2.class, "t");
		});
	}

	@Test
	public void testRequireTypeFail() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requireType(new Integer(0), T2.class, "t");
		});
	}

	@Test
	public void testRequiredNotEmpty_nullString() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotEmpty((String) null, "myField");
		});
	}

	@Test
	public void testRequiredNotEmpty_nullCollection() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotEmpty((Collection<?>) null, "myField");
		});
	}

	@Test
	public void testRequiredNotEmpty_emptyString() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotEmpty("", "myField");
		});
	}

	@Test
	public void testRequiredNotEmpty_NonEmptyString() {
		ValidateArgument.requiredNotEmpty("a", "myField");
	}

	@Test
	public void testRequiredNotBlank_NonEmptyString() {
		ValidateArgument.requiredNotBlank("   a", "myField");
	}

	@Test
	public void testRequiredNotBlank_NullString() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotBlank(null, "myField");
		});
	}

	@Test
	public void testRequiredNotBlank_EmptyString() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotBlank("", "myField");
		});
	}

	@Test
	public void testRequiredNotBlank_BlankString() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotBlank("  ", "myField");
		});
	}

	@Test
	public void testRequiredNotEmpty_emptyCollection() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ValidateArgument.requiredNotEmpty(Collections.emptyList(), "myField");
		});
	}

	@Test
	public void testRequiredNotEmpty_NonEmptyCollection() {
		ValidateArgument.requiredNotEmpty(Collections.singleton("my value"), "myField");
	}
}
