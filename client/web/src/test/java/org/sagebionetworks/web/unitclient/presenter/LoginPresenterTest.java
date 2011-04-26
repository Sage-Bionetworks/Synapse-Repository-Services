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

public class LoginPresenterTest {
	
	LoginPresenter loginPresenter;
	LoginView mockView;
	
	@Before
	public void setup(){
		mockView = mock(LoginView.class);
		loginPresenter = new LoginPresenter(mockView);
		
		verify(mockView).setPresenter(loginPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		LoginPlace place = Mockito.mock(LoginPlace.class);
		loginPresenter.setPlace(place);
	}	
}
