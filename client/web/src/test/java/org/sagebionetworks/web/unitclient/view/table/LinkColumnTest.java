package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.TreeMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.view.table.LinkColumn;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.LinkColumnInfo;
import org.sagebionetworks.web.shared.UrlTemplate;

import com.google.gwt.junit.GWTMockUtilities;
import com.google.gwt.user.client.ui.Hyperlink;


public class LinkColumnTest {
	
	@Before
	public void setup(){
		// We need to disarm GWT in order to create a mock GWT HyperLink.
		GWTMockUtilities.disarm();
	}
	
	@After
	public void tearDown(){
		// Be nice to the next test
		GWTMockUtilities.restore();
	}
	
	@Test
	public void testCreate() {
		// setup the link object with a mock link
		Hyperlink mock = Mockito.mock(Hyperlink.class);
		String hyperLinkToString = "HyperLink.toString()";
		when(mock.toString()).thenReturn(hyperLinkToString);
		LinkColumn column = new LinkColumn(mock);
		// The map holds cell data
		Map<String, Object> row = new TreeMap<String, Object>();
		String displayKey = "displayKey";
		String urlKey = "urlKey";
		// Set the two columns
		// Create the metadata for this
		LinkColumnInfo columnInfo = new LinkColumnInfo();
		columnInfo.setDisplay(new ColumnInfo(displayKey,ColumnInfo.Type.String.name(), "display", "desc"));
		columnInfo.setUrl(new UrlTemplate("Dataset:{id}", urlKey,ColumnInfo.Type.String.name(), "url display", "url desc" ));
		column.setLinkColumnInfo(columnInfo);
		String value = column.getValue(row);
		assertNotNull(value);
		// The value will be empty string since we have not set any values in
		// the map.
		assertTrue("".equals(value));
		// Now add one vaue to the map
		String display = "toDisplay";
		row.put(displayKey, display);
		// The value should still be empty since we only had one value
		value = column.getValue(row);
		assertNotNull(value);
		// The value will be empty string since we have not set any values in
		// the map.
		assertTrue("".equals(value));
		// Now set the second value
		String url = "dataset/one";
		row.put(urlKey, url);
		value = column.getValue(row);
		// We expect the text to be set on the hyperlink
		verify(mock).setText(display);
		verify(mock).setTargetHistoryToken(url);
		assertNotNull(value);
		System.out.println(value);
		assertEquals(hyperLinkToString, value);
	}

}
