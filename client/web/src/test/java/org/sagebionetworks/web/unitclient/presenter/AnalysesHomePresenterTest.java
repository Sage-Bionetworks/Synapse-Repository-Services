package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.AnalysesHome;
import org.sagebionetworks.web.client.presenter.AnalysesHomePresenter;
import org.sagebionetworks.web.client.view.AnalysesHomeView;

public class AnalysesHomePresenterTest {

	AnalysesHomePresenter analysesHomePresenter;
	AnalysesHomeView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	
	@Before
	public void setup(){
		mockView = mock(AnalysesHomeView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		analysesHomePresenter = new AnalysesHomePresenter(mockView, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(analysesHomePresenter);
	}	
	
	@Test
	public void testSetPlace() {
		AnalysesHome place = Mockito.mock(AnalysesHome.class);
		analysesHomePresenter.setPlace(place);
	}	
}
