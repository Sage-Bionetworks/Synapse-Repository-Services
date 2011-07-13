package org.sagebionetworks.web.client.presenter.users;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.users.PasswordResetView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
	
	@Inject
	public PasswordResetPresenter(PasswordResetView view, CookieProvider cookieProvider, UserAccountServiceAsync userService, AuthenticationController authenticationController, SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle){
		this.view = view;
		this.userService = userService;
		this.authenticationController = authenticationController;
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		// Set the presenter on the view
		this.view.setPresenter(this);
		this.cookieProvider = cookieProvider;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
		this.placeController = DisplayUtils.placeController;
	}

	public void setPlace(PasswordReset place) {
		this.place = place;
						
//		// show proper view if token is present
//		if("0".equals(place.toToken())) {
//			view.showRequestForm();
//		} else {
//			view.showMessage(AbstractImagePrototype.create(sageImageBundle.loading16()).getHTML() + " Loading Password Reset...");
//			
//			// show same error if service fails as with an invalid token					
//			final String errorMessage = "Password reset period has expired. <a href=\"#PasswordReset:0\">Please request another Password Reset</a>.";
//			userService.isActivePasswordResetToken(place.toToken(), new AsyncCallback<Boolean>() {				
//				@Override
//				public void onSuccess(Boolean result) {
//					if(result) {
//						view.showResetForm();
//					} else {
//						view.showMessage(errorMessage);
//					}
//				}
//				
//				@Override
//				public void onFailure(Throwable caught) {
//					view.showMessage(errorMessage);
//				}
//			});		
//		}
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

//	@Override
//	public void resetPassword(final String newPassword) {		
//		userService.resetPassword(place.toToken(), newPassword, new AsyncCallback<String>() {			
//			@Override
//			public void onSuccess(String username) {				
//				view.showPasswordResetSuccess();
//				view.showInfo("Your password has been reset."); 
//				// log in user
//				try {
//					authenticationController.loginUser(username, newPassword);					 
//					placeController.goTo(new Home("0")); // redirect to home page
//				} catch (AuthenticationException e) {
//					// if login fails, just send the user to the login screen
//					placeController.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
//				}
//			}
//			
//			@Override
//			public void onFailure(Throwable caught) {
//				view.showErrorMessage("Password reset failed. Please try again.");				
//			}
//		});		
//	}

}
