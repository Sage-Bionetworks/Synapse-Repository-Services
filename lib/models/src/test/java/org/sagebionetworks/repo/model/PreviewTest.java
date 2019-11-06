package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class PreviewTest {
	
	@Test
	public void testRoundTripPreview() throws JSONObjectAdapterException {
		Preview p1 = new Preview();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();

		p1.setCreatedBy("createdBy");
		p1.setCreatedOn(d);
		p1.setDescription("description");
		p1.setEtag("1");
		p1.setId("1");
		p1.setModifiedBy("modifiedBy");
		p1.setModifiedOn(d);
		p1.setName("name");
		p1.setParentId("0");

		List<String> headers = new ArrayList<String>();
		headers.add("header1");
		headers.add("header2");
		p1.setHeaders(headers);
		
		p1.setPreviewString("previewString");
		
		List<Row> rows = new ArrayList<Row>();
		Row r = new Row();
		List<String> cells = new ArrayList<String>();
		cells.add("cell1");
		cells.add("cell2");
		r.setCells(cells);
		r.setCells(cells);
		rows.add(r);
		p1.setRows(rows);

		adapter1 = p1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Preview p2 = new Preview(adapter2);
		
		assertEquals(p1, p2);		return;
	}
}
