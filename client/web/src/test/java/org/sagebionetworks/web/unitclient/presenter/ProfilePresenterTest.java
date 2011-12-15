package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.LinkedInServiceAsync;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.presenter.ProfilePresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.ProfileView;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.client.GlobalApplicationState;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class ProfilePresenterTest {
	
	ProfilePresenter profilePresenter;
	ProfileView mockView;
	AuthenticationController mockAuthenticationController;
	UserAccountServiceAsync mockUserService;
	LinkedInServiceAsync mockLinkedInService;
	GlobalApplicationState mockGlobalApplicationState;
	PlaceChanger mockPlaceChanger;	
	CookieProvider mockCookieProvider;
	Profile place = Mockito.mock(Profile.class);
	
	UserData testUser = new UserData("testuser@test.com", "tester", "token", false);
	String password = "password";
	
	@Before
	public void setup() {
		mockView = mock(ProfileView.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockUserService = mock(UserAccountServiceAsync.class);
		mockPlaceChanger = mock(PlaceChanger.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockLinkedInService = mock(LinkedInServiceAsync.class);
		mockCookieProvider = mock(CookieProvider.class);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		verify(mockView).setPresenter(profilePresenter);

		profilePresenter.setPlace(place);		
	}
	
	@Test
	public void testStart() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);

		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		profilePresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);
	}
	
	@Test
	public void testSetPlace() {
		Profile newPlace = Mockito.mock(Profile.class);
		profilePresenter.setPlace(newPlace);
	}
	
	@Test
	public void testResetPassword() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);
		
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(testUser);
		String newPassword = "otherpassword";
		
		profilePresenter.resetPassword(password, newPassword);
	}
	
	@Test
	public void testCreateSynapsePassword() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);

		when(mockAuthenticationController.getLoggedInUser()).thenReturn(testUser);
		
		profilePresenter.createSynapsePassword();
	}
	
	@Test
	public void testUpdateProfile() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);

		when(mockAuthenticationController.getLoggedInUser()).thenReturn(testUser);
		String firstName = "foo";
		String lastName = "bar";
		
		profilePresenter.updateProfile(firstName, lastName);
	}
	
	@Test
	public void testRedirectToLinkedIn() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);
	
		profilePresenter.redirectToLinkedIn();
	}
	
	@Test
	public void testUpdateProfileWithLinkedIn() {
		reset(mockView);
		reset(mockAuthenticationController);
		reset(mockUserService);
		reset(mockPlaceChanger);
		reset(mockGlobalApplicationState);
		reset(mockCookieProvider);
		profilePresenter = new ProfilePresenter(mockView, mockAuthenticationController, mockUserService, mockLinkedInService, mockGlobalApplicationState, mockCookieProvider);	
		profilePresenter.setPlace(place);

		when(mockCookieProvider.getCookie(CookieKeys.LINKEDIN)).thenReturn("secret");
		String requestToken = "token";
		String verifier = "12345";
		profilePresenter.updateProfileWithLinkedIn(requestToken, verifier);
	}
	
}