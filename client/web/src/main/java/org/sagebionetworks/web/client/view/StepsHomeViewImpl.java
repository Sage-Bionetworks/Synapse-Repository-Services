package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class StepsHomeViewImpl extends Composite implements StepsHomeView {

	public interface StepsHomeViewImplUiBinder extends UiBinder<Widget, StepsHomeViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel createStepButtonPanel;
		
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private IconsImageBundle icons;
	private NodeEditor nodeEditor;
	private Header headerWidget;
	private Window startStepWindow;
	private String currentUserId = null;

	private final int INITIAL_QUERY_TABLE_OFFSET = 0;
	private final int QUERY_TABLE_LENGTH = 20;
	
	@Inject
	public StepsHomeViewImpl(StepsHomeViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			final NodeEditor nodeEditor) {		
		initWidget(binder.createAndBindUi(this));

		this.queryServiceTableResourceProvider = queryServiceTableResourceProvider;
		this.icons = icons;
		this.nodeEditor = nodeEditor;
		this.headerWidget = headerWidget;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);

	}


	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();
				
		this.queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.step, true, 1000, 487, presenter.getPlaceChanger());		

		if(null != currentUserId) {
			// If the user is logged in, show only that user's steps, most recent first
			List<WhereCondition> where = new ArrayList<WhereCondition>();
			where.add(new WhereCondition("step.createdBy", WhereOperator.EQUALS, currentUserId));
			queryServiceTable.setWhereCondition(where);
			queryServiceTable.toggleSort("step.createdOn");
		}
		
		// Start on the first page and trigger a data fetch from the server
		queryServiceTable.pageTo(INITIAL_QUERY_TABLE_OFFSET, QUERY_TABLE_LENGTH);
		tablePanel.clear();
		tablePanel.add(queryServiceTable.asWidget());

				
		Button createStepButton = new Button("Start a Step", AbstractImagePrototype.create(icons.addSquare16()));
		createStepButton.addSelectionListener(new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {								
				startStepWindow = new Window();  
				startStepWindow.setSize(600, 240);
				startStepWindow.setPlain(true);
				startStepWindow.setModal(true);
				startStepWindow.setBlinkModal(true);
				startStepWindow.setHeading("Start a Step");
				startStepWindow.setLayout(new FitLayout());								
				nodeEditor.addCancelHandler(new CancelHandler() {					
					@Override
					public void onCancel(CancelEvent event) {
						startStepWindow.hide();
					}
				});
				nodeEditor.addPersistSuccessHandler(new PersistSuccessHandler() {					
					@Override
					public void onPersistSuccess(PersistSuccessEvent event) {
						startStepWindow.hide();
						queryServiceTable.refreshFromServer();
					}
				});
				nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
				startStepWindow.add(nodeEditor.asWidget(NodeType.STEP), new FitData(4));						
				startStepWindow.show();			
			}
		});
		createStepButtonPanel.clear();
		createStepButtonPanel.add(createStepButton);		
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}


	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}


	@Override
	public void clear() {
		if(startStepWindow != null) startStepWindow.hide();
	}


	@Override
	public void setCurrentUserId(String userId) {
		this.currentUserId = userId;
	}

}
