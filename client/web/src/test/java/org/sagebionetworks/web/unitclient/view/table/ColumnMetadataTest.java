package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.shared.ColumnMetadata.RenderType;

public class ColumnMetadataTest {
	
	@Test
	public void testRenderTypeCounts(){
		// Links should have two columns
		assertEquals(2, RenderType.LINK.getKeyCount());
		assertEquals(1, RenderType.DATE.getKeyCount());
		assertEquals(1, RenderType.IMAGE.getKeyCount());
		assertEquals(1, RenderType.IMAGE_LIST.getKeyCount());
		assertEquals(1, RenderType.STRING.getKeyCount());		
	}

}
