package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;

import com.google.common.collect.Lists;


public class PartialRowUtilsTest {
	
	@Test
	public void testWriteThenRead() throws IOException{
		// one
		PartialRow row1 = new PartialRow();
		row1.setRowId(0L);
		Map<String, String> map = new HashMap<String, String>();
		map.put("111", "foo");
		map.put("444", "123.4");
		row1.setValues(map);
		
		// two
		PartialRow row2 = new PartialRow();
		row2.setRowId(1L);
		map = new HashMap<String, String>();
		map.put("111", "bar");
		map.put("55", "true");
		row2.setValues(map);
		
		PartialRowSet set = new PartialRowSet();
		set.setTableId("syn123");
		set.setRows(Lists.newArrayList(row1, row2));
		// Write to the stream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PartialRowUtils.writePartialRows(set, out);
		// now read from the stream
		PartialRowSet results = PartialRowUtils.readPartialRows(new ByteArrayInputStream(out.toByteArray()));
		assertNotNull(results);
		assertEquals(set, results);
	}

}
