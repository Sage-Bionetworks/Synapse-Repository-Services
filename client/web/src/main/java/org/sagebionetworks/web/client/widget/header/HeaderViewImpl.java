package org.sagebionetworks.web.client.widget.header;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
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

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.SimplePanel;
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
	@UiField
	SimplePanel jumpToPanel;
		
	private Presenter presenter;
	private Map<MenuItems, Element> itemToElement;
	private AuthenticationController authenticationController;	
	private IconsImageBundle iconsImageBundle;
	private GlobalApplicationState globalApplicationState;
	private LayoutContainer jumpTo;
	private TextField<String> jumpToField;
	private Button goButton;
	
	
	@Inject
	public HeaderViewImpl(Binder binder, AuthenticationControllerImpl authenticationController, SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle, GlobalApplicationState globalApplicationState) {
		this.initWidget(binder.createAndBindUi(this));
		this.iconsImageBundle = iconsImageBundle;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		
		itemToElement = new HashMap<Header.MenuItems, Element>();		
		itemToElement.put(MenuItems.DATASETS, navbarDatasets);
		itemToElement.put(MenuItems.TOOLS, navbarTools);
		itemToElement.put(MenuItems.NETWORKS, navbarNetworks);
		itemToElement.put(MenuItems.PEOPLE, navbarPeople);
		itemToElement.put(MenuItems.PROJECTS, navbarProjects);		
	
		// setup header links
		datasetsLink.getElement().setId("navbar_datasets_a"); // for special first element style
		datasetsLink.setTargetHistoryToken(globalApplicationState.getAppPlaceHistoryMapper().getToken(new DatasetsHome(DisplayUtils.DEFAULT_PLACE_TOKEN)));
		toolsLink.setTargetHistoryToken(globalApplicationState.getAppPlaceHistoryMapper().getToken(new ComingSoon(DisplayUtils.DEFAULT_PLACE_TOKEN))); 
		networksLink.setTargetHistoryToken(globalApplicationState.getAppPlaceHistoryMapper().getToken(new ComingSoon(DisplayUtils.DEFAULT_PLACE_TOKEN)));
		if(DisplayConstants.showDemoHtml) {
			peopleLink.setHref("people.html");
			projectsLink.setHref("projects.html");
		} else {
			peopleLink.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new ComingSoon(DisplayUtils.DEFAULT_PLACE_TOKEN)));			
			projectsLink.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new ProjectsHome(DisplayUtils.DEFAULT_PLACE_TOKEN)));
		}
		
		// add jump to panel
		createJumpToPanel();
		jumpToPanel.clear();
		jumpToPanel.add(jumpTo);
		
		
	}

	private void createJumpToPanel() {
		if(jumpTo == null) {
			HBoxLayout layout = new HBoxLayout();	
			jumpTo = new LayoutContainer(layout);
			
			jumpTo.setWidth(152);
			
			jumpToField = new TextField<String>();
			jumpToField.setEmptyText(DisplayConstants.LABEL_GOTO_SYNAPSE_ID);
			jumpToField.setWidth(125);
			jumpTo.add(jumpToField);
			
			goButton = new Button();
			goButton.setText("Go");
			goButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
				
				@Override
				public void componentSelected(ButtonEvent ce) {
					presenter.lookupId(jumpToField.getValue());
					jumpToField.clear();
					jumpToField.setEmptyText(DisplayConstants.LABEL_GOTO_SYNAPSE_ID);
					jumpToField.repaint();
				}
			});
			jumpTo.add(goButton);
			
			// Enter key clicks go
			new KeyNav<ComponentEvent>(jumpToField) {
				@Override
				public void onEnter(ComponentEvent ce) {
					super.onEnter(ce);
					goButton.fireEvent(Events.Select);					
				}
			};
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
		
		jumpToField.clear();
		jumpToField.setEmptyText(DisplayConstants.LABEL_GOTO_SYNAPSE_ID);
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
			topRightLink1.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new Profile(DisplayUtils.DEFAULT_PLACE_TOKEN))); //demo
			if(DisplayConstants.showDemoHtml) {
				topRightLink1.setHref("edit_profile.html");
			}	

			userName.setInnerHTML("Welcome " + userData.getUserName());			
			topRightLink2.setHTML("Logout");		
			topRightLink2.setTargetHistoryToken(globalApplicationState.getAppPlaceHistoryMapper().getToken(new LoginPlace(LoginPlace.LOGOUT_TOKEN)));			
		} else {
			topRightLink1.setHTML("Register");
			//topRightLink1.setTargetHistoryToken(DisplayUtils.getDefaultHistoryTokenForPlace(RegisterAccount.class));			
			topRightLink1.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new RegisterAccount(DisplayUtils.DEFAULT_PLACE_TOKEN))); // demo

			userName.setInnerHTML("");			
			topRightLink2.setHTML("Login to Synapse");		
			topRightLink2.setTargetHistoryToken(globalApplicationState.getAppPlaceHistoryMapper().getToken(new LoginPlace(LoginPlace.LOGIN_TOKEN)));
			
		}

	}
}


