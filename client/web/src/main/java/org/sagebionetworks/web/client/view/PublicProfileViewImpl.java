package org.sagebionetworks.web.client.view;

import java.util.ArrayList;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.view.ProfileView.Presenter;
import org.sagebionetworks.web.client.view.ProfileViewImpl.ProfileViewImplUiBinder;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PublicProfileViewImpl extends Composite implements PublicProfileView {

	public interface PublicProfileViewImplUiBinder extends UiBinder<Widget, PublicProfileViewImpl> {}
	
	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel userInfoPanel;

	private Presenter presenter;
	private Header headerWidget;
	private IconsImageBundle iconsImageBundle;
	private NodeEditor nodeEditor;
	private SageImageBundle sageImageBundle;

	@Inject
	public PublicProfileViewImpl(PublicProfileViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle, final NodeEditor nodeEditor, SageImageBundle sageImageBundle) {		
		initWidget(binder.createAndBindUi(this));

		this.iconsImageBundle = icons;
		this.nodeEditor = nodeEditor;
		this.headerWidget = headerWidget;
		this.sageImageBundle = sageImageBundle;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);
	}
	
	@Override	
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();				
	}		
	
	@Override
	public void render() {
		presenter.getUserInfo();
	}

	@Override
	public void showLoading() {	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
		userInfoPanel.clear();
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}	
	
	@Override
	public void updateWithUserInfo(String name, ArrayList<String> userInfo) {
		// User's name
		String panelHtml = "<h2>" + name + "</h2> <ul>";
		
		// Rest of user's info
		for(int i = 0; i < userInfo.size(); i++) {
			panelHtml += "<li>" + userInfo.get(i) + "</li>";
		}
		
		panelHtml += "</ul>";
		userInfoPanel.add(new Html(panelHtml));
	}
	
	/*
	 * Private Methods
	 */	

	private void createUserInfoPanel() {
		userInfoPanel = new SimplePanel();
		userInfoPanel.clear();
	}
	
}