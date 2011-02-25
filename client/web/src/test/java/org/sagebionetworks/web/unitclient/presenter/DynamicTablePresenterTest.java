package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieUtils;
import org.sagebionetworks.web.client.presenter.DynamicTablePresenter;
import org.sagebionetworks.web.client.view.DynamicTableView;
import org.sagebionetworks.web.client.view.RowData;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.SearchParameters.FromType;
import org.sagebionetworks.web.test.helper.AsyncServiceRecorder;
import org.sagebionetworks.web.test.helper.AsyncServiceRecorder.MethodCall;
import org.sagebionetworks.web.unitclient.cookie.StubCookieProvider;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class DynamicTablePresenterTest {
	
	AsyncServiceRecorder<SearchService, SearchServiceAsync> recorder;
	SearchService mockSearchService;
	SearchServiceAsync asynchProxy;
	DynamicTablePresenter presenter;
	DynamicTableView mockView;
	StubCookieProvider cookieProvider;
	
	@Before
	public void setup(){
		// Create the mock service
		mockSearchService = Mockito.mock(SearchService.class);
		mockView = Mockito.mock(DynamicTableView.class);
		
		// Create the asynchronous service recorder
		recorder = new AsyncServiceRecorder<SearchService, SearchServiceAsync>(mockSearchService, SearchServiceAsync.class);
		// Create the asynchronous proxy 
		asynchProxy = recorder.createAsyncProxyToRecord();
		// Using a stub cookie provider
		cookieProvider = new StubCookieProvider();
		// Create the presenter
		presenter = new DynamicTablePresenter(mockView, asynchProxy, cookieProvider);
		presenter.setType(FromType.dataset);
		// Make sure the view gets the presenter set
		verify(mockView).setPresenter(presenter);
	}
	
	@Test
	public void testNoColumnChange(){
		// First create a few Columns
		List<HeaderData> one = new ArrayList<HeaderData>();
		List<HeaderData> two = new ArrayList<HeaderData>();
		// Add a few values
		one.add(new SampleHeaderData("idOne", "dispalyOne", "descOne"));
		one.add(new SampleHeaderData("idTwo", "dispalyTwo", "descTwo"));
		one.add(new SampleHeaderData("idThree", "dispalyThree", "descThree"));
		// The presenter should not have any columns yet so this should trigger a view update
		presenter.setCurrentColumns(one);
		// List two has the same ids
		two.add(new SampleHeaderData("idOne", null, null));
		two.add(new SampleHeaderData("idTwo", null, null));
		two.add(new SampleHeaderData("idThree", null, null));
		// When we call it again with the same data is should not trigger another view update
		presenter.setCurrentColumns(two);
		// The view should have only been called once the first time.
		verify(mockView , times(1)).setColumns(one);
		verify(mockView , times(0)).setColumns(two);
	}
	
	@Test
	public void testSwapColumns(){
		// First create a few Columns
		List<HeaderData> one = new ArrayList<HeaderData>();
		List<HeaderData> two = new ArrayList<HeaderData>();
		// Add a few values
		one.add(new SampleHeaderData("idOne", "dispalyOne", "descOne"));
		one.add(new SampleHeaderData("idTwo", "dispalyTwo", "descTwo"));
		one.add(new SampleHeaderData("idThree", "dispalyThree", "descThree"));
		// The presenter should not have any columns yet so this should trigger a view update
		presenter.setCurrentColumns(one);
		// List two has swapped the last two
		two.add(new SampleHeaderData("idOne", null, null));
		two.add(new SampleHeaderData("idThree", null, null));
		two.add(new SampleHeaderData("idTwo", null, null));
		// When we call it again with the same data is should not trigger another view update
		presenter.setCurrentColumns(two);
		// It should have been called once with the first list and once with the second
		verify(mockView , times(1)).setColumns(one);
		verify(mockView , times(1)).setColumns(two);
	}
	
	@Test
	public void testGetDisplayColumns()	{
		// With no cookie data the presenter should return an empty list
		List<String> list = presenter.getDisplayColumns();
		assertNotNull(list);
		assertEquals(0, list.size());
		// Now set a cookie
		List<String> inList = new ArrayList<String>();
		inList.add("idOne");
		inList.add("idTwo");
		inList.add("idThree");
		String cookieString = CookieUtils.createStringFromList(inList);
		// Add this as a cookie
		cookieProvider.setCookie(CookieKeys.SELECTED_DATASETS_COLUMNS, cookieString);
		// Now get the list again
		List<String> second = presenter.getDisplayColumns();
		assertNotNull(second);
		// It should match the list used to create the cookie.
		assertEquals(inList, second);	
	}
	
	@Test
	public void testSetTableResults(){
		// Create a table
		int rows = 10;
		int cols = 5;
		int total = 50;
		TableResults table = createResults(cols, rows, total);
		// Now set this table
		presenter.setTableResults(table);
		SearchParameters curParams = presenter.getCurrentSearchParameters();
		// The view should get the columns set and the rows set
		verify(mockView, times(1)).setColumns(table.getColumnInfoList());
		RowData rowData = new RowData(table.getRows(), curParams.getOffset(), curParams.getLimit(), total, curParams.getSort(), curParams.isAscending());
		verify(mockView, times(1)).setRows(rowData);
	}
	
	@Test
	public void testRefreshFromServerFail() throws Exception{
		// Get the search parameters that will be used by the presenter
		SearchParameters currentParams = presenter.getCurrentSearchParameters();
		TableResults toReturn = createResults(6, currentParams.getLimit(), 10);
		// Starting the presenter should trigger a server refresh
		when(mockSearchService.executeSearch(currentParams)).thenReturn(toReturn);
		// The recorder should start with zero calls
		assertEquals(0, recorder.getRecoredCallCount());
		presenter.refreshFromServer();
		// The recorder should have recorder one call
		assertEquals(1, recorder.getRecoredCallCount());
		// now trigger a failure
		Throwable exception = new Throwable("Total failure");
		recorder.playOnFailure(0, exception);
		verify(mockView).showMessage(exception.getMessage());
	}
	
	@Test
	public void testRefreshFromServerSuccess() throws Exception{
		// Get the search parameters that will be used by the presenter
		SearchParameters curParams = presenter.getCurrentSearchParameters();
		int columnCount = 6;
		int totalCount = 100;
		TableResults toReturn = createResults(columnCount, curParams.getLimit(), totalCount);
		// Starting the presenter should trigger a server refresh
		when(mockSearchService.executeSearch(curParams)).thenReturn(toReturn);
		// The recorder should start with zero calls
		assertEquals(0, recorder.getRecoredCallCount());
		presenter.refreshFromServer();
		// The recorder should have recorder one call
		assertEquals(1, recorder.getRecoredCallCount());
		// now trigger a failure
		recorder.playOnSuccess(0);
		verify(mockView).setColumns(toReturn.getColumnInfoList());
		RowData rowData = new RowData(toReturn.getRows(), curParams.getOffset(), curParams.getLimit(), totalCount, curParams.getSort(), curParams.isAscending());
		verify(mockView).setRows(rowData);
	}
	
	@Test
	public void testStart(){
		EventBus mockBus = Mockito.mock(EventBus.class);
		AcceptsOneWidget mockWidget = Mockito.mock(AcceptsOneWidget.class);
		presenter.start(mockWidget, mockBus);
		// The recorder should have recorder one call
		assertEquals(1, recorder.getRecoredCallCount());
		MethodCall call = recorder.getCall(0);
		assertNotNull(call);
		assertEquals("executeSearch", call.getMethod().getName());
		verify(mockView).asWidget();
	}
	
	/**
	 * Helper to create a list of headers
	 * @param number
	 * @return
	 */
	private static List<HeaderData> createColumns(int number){
		List<HeaderData> results = new ArrayList<HeaderData>();
		// Create some sample columns
		for(int i=0; i<number; i++){
			results.add(new SampleHeaderData("id"+i, "name"+i, "desc"+i));
		}
		return results;
	}
	
	/**
	 * Helper method to create TableResults
	 * @param numberCols
	 * @param numberRows
	 * @return
	 */
	public  static TableResults createResults(int numberCols, int numberRows, int totalRows){
		// First create the columns
		List<HeaderData> headers = createColumns(numberCols);
		// Now fill in the rows
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		for(int row=0; row<numberRows; row++){
			Map<String, Object> thisRow = new TreeMap<String, Object>();
			// Fill in each cell
			for(int col=0; col<numberCols; col++){
				HeaderData header = headers.get(col);
				thisRow.put(header.getId(), "cell("+col+","+row+")");
			}
			rows.add(thisRow);
		}
		// Create the results
		TableResults table = new TableResults();
		table.setTotalNumberResults(totalRows);
		table.setColumnInfoList(headers);
		table.setRows(rows);
		return table;
	}

}
