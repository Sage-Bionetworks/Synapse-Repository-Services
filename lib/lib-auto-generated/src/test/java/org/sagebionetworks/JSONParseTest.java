package org.sagebionetworks;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Validate all JSON schema files in lib-auto-generated
 * 
 * @author bkng
 *
 */
public class JSONParseTest {

	@Test
	public void testFile() throws IOException {
		File dir = new File("src/main/resources");
		Iterator<?> itr =  FileUtils.iterateFiles(dir, new String[]{"json"}, true);

		while (itr.hasNext()) {
		    File file = (File) itr.next();		    
		    try {
		    	String jstr = readFileToString(file.getAbsolutePath());
				JSONValidator.validateJSON(jstr);
		    } catch (JsonParseException jpe) {
		    	fail("Parse failed on file '" + file.getName() + "'\n" + jpe.getMessage());
		    }
		    // System.out.println("Parsing '" + file.getName() + "' was successful");
		}
	}
	
	private String readFileToString(String filePath) throws IOException {
	    BufferedReader reader = new BufferedReader(new FileReader(filePath));
	    try {
		    String line = null;
		    StringBuilder sb = new StringBuilder();
		    String ls = System.getProperty("line.separator");
		    while ((line = reader.readLine()) != null) {
		        sb.append(line);
		        sb.append(ls);
		    }	
		    return sb.toString();
	    } finally {
	    	reader.close();
	    }
	}

}
