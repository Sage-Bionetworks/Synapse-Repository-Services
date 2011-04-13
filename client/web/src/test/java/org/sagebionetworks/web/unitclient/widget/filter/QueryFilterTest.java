package org.sagebionetworks.web.unitclient.widget.filter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.filter.QueryFilterView;
import org.sagebionetworks.web.shared.DisplayableValue;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.test.helper.AsyncServiceRecorder;

public class QueryFilterTest {
	
	List<DisplayableValue> valuesOne;
	List<DisplayableValue> valuesTwo;
	List<FilterEnumeration> filterList;
	
	QueryFilter queryFilter;
	QueryFilterView mockView;
	SearchServiceAsync mockAsynch;
	SearchService mockService;
	AsyncServiceRecorder<SearchService, SearchServiceAsync> recorder;
	
	@Before
	public void setup(){
		
		valuesOne = new ArrayList<DisplayableValue>();
		valuesOne.add(new DisplayableValue("displayOne", "valueOne"));
		valuesOne.add(new DisplayableValue("displayTwo", "valueTwo"));
		valuesOne.add(new DisplayableValue("someXml:<>+='\"\\", "??/.,m{}[]1234567890-=!@#$%^&*()_+"));
		
		// Two
		valuesTwo = new ArrayList<DisplayableValue>();
		valuesTwo.add(new DisplayableValue(">displayTwoOne", "valueTwoOne"));
		valuesTwo.add(new DisplayableValue(null, "valueTwoTwo"));
		
		filterList = new ArrayList<FilterEnumeration>();
		filterList.add(new FilterEnumeration("column.id.one", "All One Values", WhereOperator.EQUALS, valuesOne));
		filterList.add(new FilterEnumeration("column.id.two", "All Two Values", WhereOperator.GREATER_THAN, valuesTwo));
		// Create the mock service
		mockService = Mockito.mock(SearchService.class);
		mockView = Mockito.mock(QueryFilterView.class);
		
		// Create the asynchronous service recorder
		recorder = new AsyncServiceRecorder<SearchService, SearchServiceAsync>(mockService, SearchServiceAsync.class);
		// Create the asynchronous proxy 
		mockAsynch = recorder.createAsyncProxyToRecord();
		queryFilter = new QueryFilter(mockView, mockAsynch);
	}
	
	@Test
	public void testInit(){
		
	}

}
