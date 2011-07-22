package org.sagebionetworks.web.client.widget.header;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.place.ComingSoon;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.ProjectsHome;
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
	SpanElement userName;
	@UiField
	Anchor topRightLink1;
	@UiField
	Hyperlink topRightLink2;
	@UiField 
	Hyperlink datasetsLink;
	@UiField 
	Hyperlink toolsLink;
	@UiField 
	Hyperlink networksLink;
	@UiField
	Anchor peopleLink;	// TODO : change to Hyperlink post demo era
	@UiField 
	Anchor projectsLink; // TODO : change to Hyperlink post demo era
		
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
	
		// setup header links
		datasetsLink.getElement().setId("navbar_datasets_a"); // for special first element style
		datasetsLink.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(DatasetsHome.class));
		toolsLink.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(ComingSoon.class)); 
		networksLink.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(ComingSoon.class));
		if(DisplayConstants.showDemoHtml) {
			peopleLink.setHref("people.html");
			projectsLink.setHref("projects.html");
		} else {
			peopleLink.setHref("#" + DisplayUtils.getDefaultHistoryTokenForPlace(ComingSoon.class));			
			projectsLink.setHref("#" + DisplayUtils.getDefaultHistoryTokenForPlace(ProjectsHome.class));
		}
		
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
			topRightLink1.setHTML("My Profile");
			//topRightLink1.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(Profile.class));
			topRightLink1.setHref("#" + DisplayUtils.getDefaultHistoryTokenForPlace(Profile.class)); //demo
			if(DisplayConstants.showDemoHtml) {
				topRightLink1.setHref("edit_profile.html");
			}	

			userName.setInnerHTML("Welcome " + userData.getUserName());			
			topRightLink2.setHTML("Logout");		
			topRightLink2.setTargetHistoryToken(DisplayUtils.getHistoryTokenForPlace(LoginPlace.class, LoginPlace.LOGOUT_TOKEN));			
		} else {
			topRightLink1.setHTML("Register");
			//topRightLink1.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(RegisterAccount.class));			
			topRightLink1.setHref("#" + DisplayUtils.getDefaultHistoryTokenForPlace(RegisterAccount.class)); // demo

			userName.setInnerHTML("");			
			topRightLink2.setHTML("Login to Synapse");		
			topRightLink2.setTargetHistoryToken(DisplayUtils.getHistoryTokenForPlace(LoginPlace.class, LoginPlace.LOGIN_TOKEN));
			
		}

	}
}


