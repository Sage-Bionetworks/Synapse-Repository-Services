package org.sagebionetworks.web.client.presenter.users;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.view.users.RegisterAccountView;
import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class RegisterAccountPresenter extends AbstractActivity implements RegisterAccountView.Presenter {
	public static final String KEY_DATASETS_SELECTED_COLUMNS_COOKIE = "org.sagebionetworks.selected.dataset.columns";
	
	private RegisterAccount place;
	private RegisterAccountView view;
	private CookieProvider cookieProvider;
	private PlaceController placeController;
	private UserAccountServiceAsync userService;
	private GlobalApplicationState globalApplicationState;
	
	@Inject
	public RegisterAccountPresenter(RegisterAccountView view, CookieProvider cookieProvider, UserAccountServiceAsync userService, GlobalApplicationState globalApplicationState){
		this.view = view;
		// Set the presenter on the view
		this.view.setPresenter(this);
		this.cookieProvider = cookieProvider;
		this.userService = userService;
		this.globalApplicationState = globalApplicationState;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
		this.placeController = globalApplicationState.getPlaceController();
	}

	@Override
	public void goTo(Place place) {
		this.placeController.goTo(place);
	}

	public void setPlace(RegisterAccount place) {
		this.place = place;
		view.clear();
		view.showDefault();
	}

	@Override
	public void registerUser(String email, String firstName, String lastName) {
		UserRegistration userInfo = new UserRegistration(email, firstName, lastName, firstName + " " + lastName);
		userService.createUser(userInfo, new AsyncCallback<Void>() {			
			@Override
			public void onSuccess(Void result) {
				view.showAccountCreated();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage(DisplayConstants.ERROR_USER_ALREADY_EXISTS);
				view.showAccountCreationFailed();
			}
		});
	}

}
