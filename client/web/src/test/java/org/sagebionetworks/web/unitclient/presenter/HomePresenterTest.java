package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.HomeView;

public class HomePresenterTest {

	HomePresenter homePresenter;
	CookieProvider cookieProvider;
	HomeView mockView;
	AuthenticationController mockAuthenticationController;
	
	@Before
	public void setup(){
		mockView = mock(HomeView.class);
		cookieProvider = mock(CookieProvider.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		homePresenter = new HomePresenter(mockView, cookieProvider, mockAuthenticationController);
		
		verify(mockView).setPresenter(homePresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Home place = Mockito.mock(Home.class);
		homePresenter.setPlace(place);
	}	
}
