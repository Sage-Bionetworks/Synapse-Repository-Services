package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.sagebionetworks.web.client.view.table.column.provider.AbstractColumnProvider;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.ColumnInfo.Type;

import com.google.gwt.user.cellview.client.Column;

@SuppressWarnings({"unchecked"})
public class AbstractColumnProviderTest {
	
	/**
	 * A simple implementation of the AbstractColumnProvider
	 *
	 */
	public static class SimpleImpl extends AbstractColumnProvider<String>{
		@Override
		public Type[] supportedTypes() {
			return new Type[]{Type.String, Type.StringArray};
		}

		@Override
		public String valueToString(String value) {
			return value;
		}
	}
	
	@Test
	public void testIsCompatible(){
		SimpleImpl provider = new SimpleImpl();
		// all null data
		ColumnInfo nonString = new ColumnInfo();
		assertFalse(provider.isCompatible(nonString));
		// True non-string
		nonString = new ColumnInfo();
		nonString.setType(ColumnInfo.Type.Boolean.toString());
		assertFalse(provider.isCompatible(nonString));
		// A String
		ColumnInfo stringInfo = new ColumnInfo("someId", ColumnInfo.Type.String.name(), "some display", "desc");
		assertTrue(provider.isCompatible(stringInfo));
		// A String array
		stringInfo = new ColumnInfo("someId", ColumnInfo.Type.StringArray.name(), "some display", "desc");
		assertTrue(provider.isCompatible(stringInfo));
	}
	
	@Test
	public void testCreateStringColumn(){
		SimpleImpl provider = new SimpleImpl();
		ColumnInfo stringInfo = new ColumnInfo("someId", ColumnInfo.Type.String.name(), "some display", "desc");
		Column<Map<String, Object>, String> column = (Column<Map<String, Object>, String>) provider.createColumn(stringInfo);
		assertNotNull(column);
		// Validate that it is acting as expected
		Map<String, Object> row = new TreeMap<String, Object>();
		// Start with an empty row
		String value = column.getValue(row);
		assertNull(value);
		// Now add a value
		String expectedValue = "expected value";
		row.put(stringInfo.getId(), expectedValue);
		value = column.getValue(row);
		assertNotNull(value);
		assertEquals(expectedValue, value);
	}
	
	@Test
	public void testCreateStringArrayColumn(){
		SimpleImpl provider = new SimpleImpl();
		ColumnInfo stringInfo = new ColumnInfo("someId", ColumnInfo.Type.StringArray.name(), "some display", "desc");
		Column<Map<String, Object>, String> column = (Column<Map<String, Object>, String>) provider.createColumn(stringInfo);
		assertNotNull(column);
		// Validate that it is acting as expected
		Map<String, Object> row = new TreeMap<String, Object>();
		// Start with an empty row
		String value = column.getValue(row);
		assertNull(value);
		// Now add a value
		String[] array = new String[]{"one", "two"};
		String expectedValue = "one, two";
		row.put(stringInfo.getId(), array);
		value = column.getValue(row);
		assertNotNull(value);
		assertEquals(expectedValue, value);
	}
	
	@Test
	public void testCreateStringListColumn(){
		SimpleImpl provider = new SimpleImpl();
		ColumnInfo stringInfo = new ColumnInfo("someId", ColumnInfo.Type.StringArray.name(), "some display", "desc");
		Column<Map<String, Object>, String> column = (Column<Map<String, Object>, String>) provider.createColumn(stringInfo);
		assertNotNull(column);
		// Validate that it is acting as expected
		Map<String, Object> row = new TreeMap<String, Object>();
		// Start with an empty row
		String value = column.getValue(row);
		assertNull(value);
		// Now add a value
		List<String> list = new ArrayList<String>();
		list.add("one");
		list.add("two");
		String expectedValue = "one, two";
		row.put(stringInfo.getId(), list);
		value = column.getValue(row);
		assertNotNull(value);
		assertEquals(expectedValue, value);
	}

}
