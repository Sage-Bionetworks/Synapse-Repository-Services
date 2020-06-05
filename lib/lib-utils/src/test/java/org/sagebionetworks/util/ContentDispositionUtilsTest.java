package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContentDispositionUtilsTest {

	@Test
	public void testGetContentDispositionValue(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo.txt");
		assertEquals("attachment; filename=\"foo.txt\"; filename*=utf-8''foo.txt", result);
	}

	@Test
	public void testGetContentDispositionValueWithSpaceCharacters(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo bar(baz).txt");
		assertEquals("attachment; filename=\"foo bar(baz).txt\"; filename*=utf-8''foo%20bar%28baz%29.txt", result);
	}

	@Test
	public void testGetContentDispositionValueWithCommaCharacters(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo,bar,(baz).txt");
		assertEquals("attachment; filename=\"foo,bar,(baz).txt\"; filename*=utf-8''foo%2Cbar%2C%28baz%29.txt", result);
	}

	@Test
	public void testGetContentDispositionValueWithPercentCharacters(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo%bar%baz.txt");
		assertEquals("attachment; filename=\"foo%bar%baz.txt\"; filename*=utf-8''foo%25bar%25baz.txt", result);
	}

	@Test
	public void testGetContentDispositionValueWithISO_8859_1(){
		String result = ContentDispositionUtils.getContentDispositionValue("fÖØ bær.txt");
		assertEquals("attachment; filename=\"fÖØ bær.txt\"; filename*=utf-8''f%C3%96%C3%98%20b%C3%A6r.txt", result);
	}
	// NOTE: at a higher level, we don't support filehandles w/ unicode character filenames, but if we were to lift that restriction,
	// uploads should still work
	@Test
	public void testGetContentDispositionValueWithUnicodeCharacters(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo文件bar.txt");
		assertEquals("attachment; filename=\"foo__bar.txt\"; filename*=utf-8''foo%E6%96%87%E4%BB%B6bar.txt", result);
	}
}

