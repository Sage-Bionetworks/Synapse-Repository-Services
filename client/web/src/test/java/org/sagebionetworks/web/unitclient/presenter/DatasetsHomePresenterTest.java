package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.presenter.ColumnsPopupPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.view.DatasetsHomeView;

public class DatasetsHomePresenterTest {

	DatasetsHomePresenter datasetsHomePresenter;
	DatasetsHomeView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	ColumnsPopupPresenter columnsPopupPresenter;
	CookieProvider cookieProvider;
	
	@Before
	public void setup(){
		mockView = mock(DatasetsHomeView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		datasetsHomePresenter = new DatasetsHomePresenter(mockView, columnsPopupPresenter, cookieProvider, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(datasetsHomePresenter);
	}
	
	@Test
	public void testSetPlace() {
		DatasetsHome place = Mockito.mock(DatasetsHome.class);
		datasetsHomePresenter.setPlace(place);		
	}
}
