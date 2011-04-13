package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfig;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.LinkColumnInfo;
import org.sagebionetworks.web.shared.UrlTemplate;

/**
 * Test the marshal/unmarshal of ColumnConfig from/to xml.
 * @author jmhill
 *
 */
public class ColumnConfigTest {
	
	@Test
	public void testRoundTrip() throws IOException{
		// Create a few test configs and write them to xml
		List<HeaderData> list = new ArrayList<HeaderData>();
		UrlTemplate one = new UrlTemplate();
		one.setDescription("Some Description");
		one.setDisplayName("Display name");
		one.setId("keyZero");
		one.setType(ColumnInfo.Type.String.name());
		one.setUrlTemplate("Dataset:{id}");
		list.add(one);
		
		// Add another column
		ColumnInfo two = new ColumnInfo();
		two.setDescription("Some Description Two");
		two.setDisplayName("Display name Two");
		two.setId("keyTwo");
		two.setType(ColumnInfo.Type.BooleanArray.name());
		list.add(two);
		
		// Add a link that references the other two columns
		LinkColumnInfo link = new LinkColumnInfo();
		link.setDisplay(two);
		link.setUrl(one);
		link.setId("linkIdOne");
		list.add(link);
		
		// This is the object that will be written to xml
		ColumnConfig config = new ColumnConfig();
		config.setColumns(list);
		
		// Now write it to XStream
		StringWriter writer = new StringWriter();
		// Marshal to xml
		// Note, the xml we write here is not excactly correct.
		// XStream will generate ids for our objects even though they already
		// have ids. The "ConfigXmlTest.xml" file shows how the xml should appear.
		ColumnConfig.toXml(config, writer);
		// Print it to the console
		String xmlString = writer.toString();
		System.out.println(xmlString);
		// Make sure we can create a copy from the xml

		// The stored xml file should create the same object.
		InputStream in = ColumnConfigTest.class.getClassLoader().getResourceAsStream("ConfigXmlTest.xml");
		try{
			InputStreamReader reader = new InputStreamReader(in);
			// Read the the stream
			ColumnConfig loaded = ColumnConfig.fromXml(reader);
			// The copy and the original should be the same
			assertEquals(config, loaded);
		}finally{
			if(in != null) in.close();
		}
	}
	

}
