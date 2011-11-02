package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Make sure we can read/write from the various characters sets.
 * @author John
 *
 */
@RunWith(Parameterized.class)
public class JSONEntityHttpMessageStreamReadWrite {
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				// Character sets to test
				{ "US-ASCII"},
				{ "ISO-8859-1"},
				{ "UTF-8"},
				{ "UTF-16BE"},
				{ "UTF-16LE"},
				{ "UTF-16"},
				{ null},
		});
	}
	Charset set;
	
	public JSONEntityHttpMessageStreamReadWrite(String charSet){
		if(charSet == null){
			set = null;
		}else{
			set = Charset.forName(charSet);
		}

	}
	
	@Test
	public void testReadAndWriteAllCharSets() throws IOException{
		System.out.println("Testing CharSet: "+set);
		String sample = "This is the string we will read/write";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JSONEntityHttpMessageConverter.writeToStream(sample, out, set);
		// Create the input stream
		ByteArrayInputStream in  = new ByteArrayInputStream(out.toByteArray());
		String back = JSONEntityHttpMessageConverter.readToString(in, set);
		assertEquals("CharSet: "+set, sample, back);
	}
	

}
