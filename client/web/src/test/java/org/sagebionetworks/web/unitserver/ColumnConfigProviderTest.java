package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;

public class ColumnConfigProviderTest {
	
	@Test
	public void testConfigXml(){
		// Test that we can load the configuration file used at runtime
		ColumnConfigProvider provider = new ColumnConfigProvider("ColumnConfigurationV2.xml");
		// Make sure we can get the name column\
		String key = "name";
		HeaderData header = provider.get(key);
		assertNotNull(header);
		assertTrue(header instanceof ColumnInfo);
		ColumnInfo nameInfo = (ColumnInfo)header;
		assertEquals(key, nameInfo.getId());
		// Make sure the rest of the fields are not null
		assertNotNull(nameInfo.getDescription());
		assertNotNull(nameInfo.getDisplayName());
		assertNotNull(nameInfo.getType());
	}

}
