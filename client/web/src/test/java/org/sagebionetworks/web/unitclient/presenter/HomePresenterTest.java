package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.LoginPresenter;
import org.sagebionetworks.web.client.view.HomeView;
import org.sagebionetworks.web.client.view.LoginView;

public class HomePresenterTest {

	HomePresenter homePresenter;
	CookieProvider cookieProvider;
	HomeView mockView;
	
	@Before
	public void setup(){
		mockView = mock(HomeView.class);
		cookieProvider = mock(CookieProvider.class);
		homePresenter = new HomePresenter(mockView, cookieProvider);
		
		verify(mockView).setPresenter(homePresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Home place = Mockito.mock(Home.class);
		homePresenter.setPlace(place);
	}	
}
