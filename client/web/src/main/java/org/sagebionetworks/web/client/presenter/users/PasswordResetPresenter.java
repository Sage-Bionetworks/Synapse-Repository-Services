package org.sagebionetworks.web.client.presenter.users;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.client.view.users.PasswordResetView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

@SuppressWarnings("unused")
public class PasswordResetPresenter extends AbstractActivity implements PasswordResetView.Presenter {
	
	private PlaceController placeController;
	private PasswordReset place;	
	private PasswordResetView view;
	private CookieProvider cookieProvider;
	private UserAccountServiceAsync userService;
	private AuthenticationController authenticationController;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private GlobalApplicationState globalApplicationState;
	
	@Inject
	public PasswordResetPresenter(PasswordResetView view, CookieProvider cookieProvider, UserAccountServiceAsync userService, AuthenticationController authenticationController, SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle, GlobalApplicationState globalApplicationState){
		this.view = view;
		this.userService = userService;
		this.authenticationController = authenticationController;
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		// Set the presenter on the view
		this.view.setPresenter(this);
		this.cookieProvider = cookieProvider;
		this.globalApplicationState = globalApplicationState;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
		this.placeController = globalApplicationState.getPlaceController();
	}

	public void setPlace(PasswordReset place) {
		this.place = place;
					
		view.clear(); 
		
		// show proper view if token is present
		if(DisplayUtils.DEFAULT_PLACE_TOKEN.equals(place.toToken())) {
			view.showRequestForm();
		} else {
			// Show password reset form
			view.showMessage(AbstractImagePrototype.create(sageImageBundle.loading16()).getHTML() + " Loading Password Reset...");
			
			// show same error if service fails as with an invalid token					
			final String errorMessage = "Password reset period has expired. <a href=\"#PasswordReset:0\">Please request another Password Reset</a>.";
			String sessionToken = place.toToken();
			authenticationController.loginUser(sessionToken, new AsyncCallback<UserData>() {
				@Override
				public void onSuccess(UserData result) {
					if(result != null) {
						view.showResetForm();
					} else {
						view.showMessage(errorMessage);
					}
				}

				@Override
				public void onFailure(Throwable caught) {
					view.showMessage(errorMessage);
				}
			});
		}
	}

	@Override
	public void requestPasswordReset(String emailAddress) {
		userService.sendPasswordResetEmail(emailAddress, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				view.showRequestSentSuccess();
			}

			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured in sending your request. Please retry.");
			}
		});
		
	}

	@Override
	public void resetPassword(final String newPassword) {
		UserData currentUser = authenticationController.getLoggedInUser();
		if(currentUser != null) {
			userService.setPassword(currentUser.getEmail(), newPassword, new AsyncCallback<Void>() {			
				@Override
				public void onSuccess(Void result) {				
					view.showPasswordResetSuccess();
					view.showInfo("Your password has been reset."); 
					placeController.goTo(new Home(DisplayUtils.DEFAULT_PLACE_TOKEN)); // redirect to home page
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("Password reset failed. Please try again.");				
				}
			});		
		}
	}
}
