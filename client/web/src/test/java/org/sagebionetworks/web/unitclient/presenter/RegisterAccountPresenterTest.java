package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.presenter.users.RegisterAccountPresenter;
import org.sagebionetworks.web.client.view.users.RegisterAccountView;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class RegisterAccountPresenterTest {
	
	RegisterAccountPresenter registerAccountPresenter;
	RegisterAccountView mockView;
	CookieProvider mockCookieProvider;
	UserAccountServiceAsync mockUserService;
	GlobalApplicationState mockGlobalApplicationState;
	RegisterAccount place = Mockito.mock(RegisterAccount.class);
	
	@Before
	public void setup() {
		mockView = mock(RegisterAccountView.class);
		mockCookieProvider = mock(CookieProvider.class);
		mockUserService = mock(UserAccountServiceAsync.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		registerAccountPresenter = new RegisterAccountPresenter(mockView, mockCookieProvider, mockUserService, mockGlobalApplicationState);			
		verify(mockView).setPresenter(registerAccountPresenter);
	}
	
	@Test
	public void testStart() {
		reset(mockView);
		reset(mockCookieProvider);
		reset(mockUserService);
		reset(mockGlobalApplicationState);
		registerAccountPresenter = new RegisterAccountPresenter(mockView, mockCookieProvider, mockUserService, mockGlobalApplicationState);	
		registerAccountPresenter.setPlace(place);

		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		registerAccountPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);
	}
	
	@Test
	public void testSetPlace() {
		reset(mockView);
		RegisterAccount newPlace = Mockito.mock(RegisterAccount.class);
		registerAccountPresenter.setPlace(newPlace);
		
		verify(mockView).setPresenter(registerAccountPresenter);
	}
	
	@Test
	public void testRegisterUser() {
		reset(mockView);
		reset(mockCookieProvider);
		reset(mockUserService);
		reset(mockGlobalApplicationState);
		registerAccountPresenter = new RegisterAccountPresenter(mockView, mockCookieProvider, mockUserService, mockGlobalApplicationState);	
		registerAccountPresenter.setPlace(place);
		
		String email = "test@test.com";
		String firstName = "Hello";
		String lastName = "Goodbye";
		
		registerAccountPresenter.registerUser(email, firstName, lastName);
	}
}
