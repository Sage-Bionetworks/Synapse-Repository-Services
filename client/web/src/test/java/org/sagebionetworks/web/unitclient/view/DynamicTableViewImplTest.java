package org.sagebionetworks.web.unitclient.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.presenter.DynamicTablePresenter;
import org.sagebionetworks.web.client.view.CellTableProvider;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl.Binder;
import org.sagebionetworks.web.client.view.SortableHeader;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.util.MockitoMockFactory;

import com.google.gwt.junit.GWTMockUtilities;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.tester.MockingBinder;

public class DynamicTableViewImplTest {
	
	/**
	 * This binder will create mock objects for all GWT UI objects
	 * @author jmhill
	 *
	 */
	public static class TestBinder extends MockingBinder<Widget, DynamicTableViewImpl> implements Binder {
		public TestBinder(MockitoMockFactory mockFactory) {
			super(Widget.class, mockFactory);
		}
	}
	CellTableProvider tableProvider;
	DynamicTableViewImpl view;
	ColumnFactory factory;
	ImagePrototypeSingleton prototype;
	DynamicTablePresenter presenter;
	
	@Before
	public void setup(){
		GWTMockUtilities.disarm();
		TestBinder binder = new TestBinder(new MockitoMockFactory());
		// Mock the factory
		factory = Mockito.mock(ColumnFactory.class);
		prototype = Mockito.mock(ImagePrototypeSingleton.class);
		presenter = Mockito.mock(DynamicTablePresenter.class);
		tableProvider = new CellTableProvider() {
			@Override
			public SimplePager createPager() {
				// TODO Auto-generated method stub
				return Mockito.mock(SimplePager.class);
			}
			@Override
			public CellTable<Map<String, Object>> createNewTable() {
				// TODO Auto-generated method stub
				return Mockito.mock(CellTable.class);
			}
		};
		
		view = new DynamicTableViewImpl(binder, prototype, factory, tableProvider);
		// Set the presenter on the view
		view.setPresenter(presenter);
	}
	
	@After
	public void tearDown(){
		// Be kind to the next test
		GWTMockUtilities.restore();
	}
	
	@Test
	public void testViewSetColumns(){
		assertNotNull(view);
		List<HeaderData> list = createEachTypeHeader();
		// Set this on the view
		view.setColumns(list);
		// Validate the column count
		assertEquals(list.size(), view.getColumnCount());
		list.clear();
		view.setColumns(list);
		assertEquals(list.size(), view.getColumnCount());
	}
	
	@Test
	public void testSorting(){
		// Create a few Sorting rows
		String[] keys = new String[]{"one", "two", "three"};
		Map<String, SortableHeader> map = new TreeMap<String, SortableHeader>();
		for(int i=0; i<keys.length; i++){
			SortableHeader header = view.createHeader("Display "+keys[i], keys[i]);
			// Should start off not sorting
			assertTrue(!header.isSorting());
			map.put(keys[i], header);
		}
		// Now toggle the sort
		int sortIndex = 2;
		boolean ascending = true;
		view.updateSortColumns(keys[sortIndex], ascending);
		// validate the state
		validateSort(keys, map, sortIndex, ascending);
		// Try a differnt column
		sortIndex = 1;
		ascending = false;
		view.updateSortColumns(keys[sortIndex], ascending);
		// validate the state
		validateSort(keys, map, sortIndex, ascending);
	}
	
	/**
	 * Validates that only the column indicated by the sortIndex is sorting and the ascending bit is correct.
	 * @param keys
	 * @param map
	 * @param sortIndex
	 * @param ascending
	 */
	private void validateSort(String[] keys, Map<String, SortableHeader> map, int sortIndex, boolean ascending){
		for(int i=0; i<keys.length; i++){
			SortableHeader header = map.get(keys[i]);
			assertNotNull(header);
			if(i == sortIndex){
				assertTrue(header.isSorting());
				assertEquals(ascending, header.isSortAscending());
			}else{
				assertFalse(header.isSorting());
			}
		}
	}
	
	/**
	 * Helper to create on of each type of column.
	 * @return
	 */
	public static List<HeaderData> createEachTypeHeader(){
		List<HeaderData> list = new ArrayList<HeaderData>();
		ColumnInfo.Type[] types = ColumnInfo.Type.values();
		for(int i=0; i<types.length; i++){
			list.add(new ColumnInfo("id"+i,types[i].name(), "display"+i, "desc"+i));
		}
		return list;
	}

}
