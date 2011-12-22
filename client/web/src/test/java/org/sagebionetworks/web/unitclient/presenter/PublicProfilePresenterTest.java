package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.LinkedInServiceAsync;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.PublicProfile;
import org.sagebionetworks.web.client.presenter.ProfilePresenter;
import org.sagebionetworks.web.client.presenter.PublicProfilePresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.ProfileView;
import org.sagebionetworks.web.client.view.PublicProfileView;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class PublicProfilePresenterTest {

	PublicProfilePresenter publicProfilePresenter;
	PublicProfileView mockView;
	UserAccountServiceAsync mockUserService;
	GlobalApplicationState mockGlobalApplicationState;
	PlaceChanger mockPlaceChanger;	
	CookieProvider mockCookieProvider;
	PublicProfile place = Mockito.mock(PublicProfile.class);

	@Before
	public void setup() {
		mockView = mock(PublicProfileView.class);
		mockUserService = mock(UserAccountServiceAsync.class);
		mockPlaceChanger = mock(PlaceChanger.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockCookieProvider = mock(CookieProvider.class);
		publicProfilePresenter = new PublicProfilePresenter(mockView, mockGlobalApplicationState, mockUserService, mockCookieProvider);	
		verify(mockView).setPresenter(publicProfilePresenter);

		publicProfilePresenter.setPlace(place);		
	}
	
	@Test
	public void testStart() {
		reset(mockView);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		publicProfilePresenter = new PublicProfilePresenter(mockView, mockGlobalApplicationState, mockUserService, mockCookieProvider);	
		publicProfilePresenter.setPlace(place);

		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		publicProfilePresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);
	}
	
	@Test
	public void testSetPlace() {
		PublicProfile newPlace = Mockito.mock(PublicProfile.class);
		publicProfilePresenter.setPlace(newPlace);
	}
	
	@Test
	public void testGetUserInfo() {
		reset(mockView);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		publicProfilePresenter = new PublicProfilePresenter(mockView, mockGlobalApplicationState, mockUserService, mockCookieProvider);	
		publicProfilePresenter.setPlace(place);

		publicProfilePresenter.getUserInfo();
	}

}
