package org.sagebionetworks.web.client.widget.header;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationControllerImpl;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

@SuppressWarnings("unused")
public class HeaderViewImpl extends Composite implements HeaderView {

	public interface Binder extends UiBinder<Widget, HeaderViewImpl> {
	}
	
	@UiField
	LIElement navbarDatasets;
	@UiField
	LIElement navbarTools;
	@UiField
	LIElement navbarNetworks;
	@UiField
	LIElement navbarPeople;
	@UiField
	LIElement navbarProjects;
	@UiField
	Anchor searchAnchor;
	@UiField
	SpanElement userName;
	@UiField
	Hyperlink topRightLink1;
	@UiField
	Hyperlink topRightLink2;
		
	private Presenter presenter;
	private Map<MenuItems, Element> itemToElement;
	private AuthenticationController authenticationController;	
	private IconsImageBundle iconsImageBundle;
	
	@Inject
	public HeaderViewImpl(Binder binder, AuthenticationControllerImpl authenticationController, SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle) {
		this.initWidget(binder.createAndBindUi(this));
		this.iconsImageBundle = iconsImageBundle;
		this.authenticationController = authenticationController;
		
		itemToElement = new HashMap<Header.MenuItems, Element>();		
		itemToElement.put(MenuItems.DATASETS, navbarDatasets);
		itemToElement.put(MenuItems.TOOLS, navbarTools);
		itemToElement.put(MenuItems.NETWORKS, navbarNetworks);
		itemToElement.put(MenuItems.PEOPLE, navbarPeople);
		itemToElement.put(MenuItems.PROJECTS, navbarProjects);		

		// search button
		searchAnchor.setHTML(AbstractImagePrototype.create(sageImageBundle.searchButtonHeaderIcon()).getHTML());
		//searchAnchor.setStyleName("search_button");		
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		setUser(presenter.getUser());		
	}

	@Override
	public void setMenuItemActive(MenuItems menuItem) {
		if(itemToElement == null) loadMap();
		Element el = itemToElement.get(menuItem);
		if(el != null) {
			el.addClassName("active");
		}
	}

	@Override
	public void removeMenuItemActive(MenuItems menuItem) {
		if(itemToElement == null) loadMap();
		Element el = itemToElement.get(menuItem);
		if(el != null) {
			el.removeClassName("active");
		}
	}

	@Override
	public void refresh() {
		setUser(presenter.getUser());
	}

	
	/*
	 * Private Methods
	 */
	// load elements after page has rendered
	private void loadMap() {
	}
	
	private void setUser(UserData userData) {
		if(userData != null) {
			userName.setInnerHTML("Welcome " + userData.getUserName());			
			topRightLink1.setHTML("Logout");		
			topRightLink1.setTargetHistoryToken(LoginPlace.PLACE_STRING + ":" + LoginPlace.LOGOUT_TOKEN);			
			topRightLink2.setHTML("My Profile");
			topRightLink2.setTargetHistoryToken(Profile.PLACE_STRING + ":" + DisplayUtils.DEFAULT_PLACE_TOKEN);			
		} else {
			userName.setInnerHTML("");			
			topRightLink1.setHTML("Login to Synapse");		
			topRightLink1.setTargetHistoryToken(LoginPlace.PLACE_STRING + ":" + LoginPlace.LOGIN_TOKEN);
			
			topRightLink2.setHTML("Register");
			topRightLink2.setTargetHistoryToken(RegisterAccount.PLACE_STRING + ":" + DisplayUtils.DEFAULT_PLACE_TOKEN);
		}
	}
}


