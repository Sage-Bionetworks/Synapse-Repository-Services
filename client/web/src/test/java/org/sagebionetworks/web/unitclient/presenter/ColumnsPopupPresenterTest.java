package org.sagebionetworks.web.unitclient.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.presenter.ColumnSelectionChangeListener;
import org.sagebionetworks.web.client.presenter.ColumnsPopupPresenter;
import org.sagebionetworks.web.client.view.ColumnsPopupView;
import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.test.helper.AsyncServiceRecorder;

public class ColumnsPopupPresenterTest {
	
	AsyncServiceRecorder<SearchService, SearchServiceAsync> recorder;
	SearchService mockSearchService;
	SearchServiceAsync asynchProxy;
	ColumnsPopupView mockView;
	ColumnsPopupPresenter presenter;
	
	SampleHeaderData a;
	SampleHeaderData b;
	SampleHeaderData c;
	SampleHeaderData d;
	List<HeaderData> defaults;
	List<HeaderData> additional;
	ColumnsForType columnsForType;
	String type;
	
	ColumnSelectionChangeListener mockListener;
	
	@Before
	public void setup(){
		mockSearchService = Mockito.mock(SearchService.class);
		mockView = Mockito.mock(ColumnsPopupView.class);
		
		// Create the asynchronous service recorder
		recorder = new AsyncServiceRecorder<SearchService, SearchServiceAsync>(mockSearchService, SearchServiceAsync.class);
		// Create the asynchronous proxy 
		asynchProxy = recorder.createAsyncProxyToRecord();
		
		presenter = new ColumnsPopupPresenter(asynchProxy, mockView);
		// The view should have its presenter set in the constructor
		verify(mockView).setPresenter(presenter);
		
		a = new SampleHeaderData("dataset.a", "A", "A desc");
		b = new SampleHeaderData("dataset.b", "B", "B desc");
		c = new SampleHeaderData("dataset.c", "C", "C desc");
		d = new SampleHeaderData("dataset.d", "D", "D desc");
		defaults = new ArrayList<HeaderData>();
		defaults.add(b);
		defaults.add(d);
		additional = new ArrayList<HeaderData>();
		additional.add(c);
		additional.add(a);
		type = ObjectType.dataset.name();
		columnsForType = new ColumnsForType(type, defaults, additional);
		// We want the service to return the following when asked for the columns.
		when(mockSearchService.getColumnsForType(type)).thenReturn(columnsForType);
		// Mock listener
		mockListener =  Mockito.mock(ColumnSelectionChangeListener.class);
		
	}
	
	@Test
	public void testShowPopupFail() throws Exception{

		presenter.showPopup(type, null, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now Fire a failure
		String errorMessage = "some error";
		Throwable toThrow = new Throwable(errorMessage);
		recorder.playOnFailure(0, toThrow);
		// The view should show an error and then hide
		verify(mockView).showError(errorMessage);
		verify(mockView, atLeastOnce()).hide();
	}
	
	@Test
	public void testShowPopupSuccess() throws Exception{
		presenter.showPopup(type, null, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now fire an on Success
		recorder.playOnSuccess(0);
		assertNotNull(presenter.getDefaultColumns());
		assertNotNull(presenter.getAdditionalColumns());
		assertEquals(defaults, presenter.getDefaultColumns());
		assertEquals(additional, presenter.getAdditionalColumns());
		
		// since we passed a null selection list, then all of the defaulsts should
		// be selected and none of the additional should be selected.
		for(HeaderData header: defaults){
			assertTrue(presenter.isSelected(header.getId()));
		}
		for(HeaderData header: additional){
			assertFalse(presenter.isSelected(header.getId()));
		}
		
		// The view should have been passed both lists
		verify(mockView).setColumns(defaults, additional);
		verify(mockView, atLeastOnce()).show();
	}
	
	@Test
	public void testShowPopupSuccessNonNull() throws Exception{
		List<String> selection = new ArrayList<String>();
		// Select A & B
		selection.add(a.getId());
		selection.add(b.getId());
		presenter.showPopup(type, selection, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now fire an on Success
		recorder.playOnSuccess(0);
		// since we passed a null selection list, then all of the defaulsts should
		// be selected and none of the additional should be selected.
		assertTrue(presenter.isSelected(a.getId()));
		assertTrue(presenter.isSelected(b.getId()));
		// C & D were not selected
		assertFalse(presenter.isSelected(c.getId()));
		assertFalse(presenter.isSelected(d.getId()));

	}
	
	@Test
	public void testCancel() throws Exception{
		// Us a mock listener
		List<String> selection = new ArrayList<String>();
		// Select A & B
		selection.add(a.getId());
		selection.add(b.getId());
		presenter.showPopup(type, selection, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now fire an on Success
		recorder.playOnSuccess(0);
		// Toggle A & C
		presenter.setColumnSelected(a.getId(), false);
		presenter.setColumnSelected(c.getId(),  true);
		// C should be selected before the cancel
		assertTrue(presenter.isSelected(c.getId()));
		// Now cancel
		presenter.cancel();
		verify(mockView, atLeastOnce()).hide();
		// C should no longer be selected
		assertFalse(presenter.isSelected(c.getId()));
		// Apply should not be called
		verify(mockListener, never()).columnSelectionChanged(null);
	}
	
	@Test
	public void testApplyNoChange() throws Exception{
		// Us a mock listener
		List<String> selection = new ArrayList<String>();
		// Select A & B
		selection.add(a.getId());
		selection.add(b.getId());
		presenter.showPopup(type, selection, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now fire an on Success
		recorder.playOnSuccess(0);
		// Toggle A 
		presenter.setColumnSelected(a.getId(), false);
		presenter.setColumnSelected(a.getId(), true);
		// Now cancel
		presenter.apply();
		verify(mockView, atLeastOnce()).hide();
		// Since there was no change apply should not be called
		verify(mockListener, never()).columnSelectionChanged(null);
	}
	
	@Test
	public void testApplyRealChange() throws Exception{
		// Us a mock listener
		List<String> selection = new ArrayList<String>();
		// Select A & B
		selection.add(a.getId());
		selection.add(b.getId());
		presenter.showPopup(type, selection, mockListener);
		// The recored should have captured a call
		assertEquals(1, recorder.getRecoredCallCount()); 
		// Now fire an on Success
		recorder.playOnSuccess(0);
		// Toggle A 
		presenter.setColumnSelected(a.getId(), false);
		presenter.setColumnSelected(c.getId(), true);
		// Add back D
		presenter.setColumnSelected(d.getId(), true);
		// Now cancel
		presenter.apply();
		verify(mockView, atLeastOnce()).hide();
		// There was a real change this time
		List<String> expectedSelection = new ArrayList<String>();
		// They should be in the same order as defined
		expectedSelection.add(b.getId());
		expectedSelection.add(d.getId());
		expectedSelection.add(c.getId());
		verify(mockListener, atLeastOnce()).columnSelectionChanged(expectedSelection);
	}

}
