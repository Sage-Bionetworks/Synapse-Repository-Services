package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ProjectsHomeViewImpl extends Composite implements ProjectsHomeView {

	public interface ProjectsHomeViewImplUiBinder extends UiBinder<Widget, ProjectsHomeViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel createProjectButtonPanel;
		
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	
	@Inject
	public ProjectsHomeViewImpl(ProjectsHomeViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle icons, SageImageBundle imageBundle, QueryServiceTableResourceProvider queryServiceTableResourceProvider) {		
		initWidget(binder.createAndBindUi(this));

		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.dataset, true, 1000, 480);
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);
				
		Button createProjectButton = new Button("Start a Project", AbstractImagePrototype.create(icons.addSquare16()));
		createProjectButtonPanel.add(createProjectButton);
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}

}
