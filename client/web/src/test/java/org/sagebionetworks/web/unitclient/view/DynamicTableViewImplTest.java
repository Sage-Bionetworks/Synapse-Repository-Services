package org.sagebionetworks.web.unitclient.view;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.jukito.JukitoRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.presenter.DynamicTablePresenter;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl.Binder;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.util.MockitoMockFactory;

import com.google.gwt.junit.GWTMockUtilities;
import com.google.gwt.resources.client.ImageResource;
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
		view = new DynamicTableViewImpl(binder, prototype, factory);
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
