package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

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

public class AnalysesHomeViewImpl extends Composite implements AnalysesHomeView {

	public interface AnalysesHomeViewImplUiBinder extends UiBinder<Widget, AnalysesHomeViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel createAnalysisButtonPanel;
		
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private IconsImageBundle icons;
	private NodeEditor nodeEditor;
	private Header headerWidget;
	private Window startAnalysisWindow;

	private final int INITIAL_QUERY_TABLE_OFFSET = 0;
	private final int QUERY_TABLE_LENGTH = 20;
	
	@Inject
	public AnalysesHomeViewImpl(AnalysesHomeViewImplUiBinder binder,
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
				
		this.queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.analysis, true, 1000, 487, presenter.getPlaceChanger());		
		// Start on the first page and trigger a data fetch from the server
		queryServiceTable.pageTo(INITIAL_QUERY_TABLE_OFFSET, QUERY_TABLE_LENGTH);
		tablePanel.clear();
		tablePanel.add(queryServiceTable.asWidget());

				
		Button createAnalysisButton = new Button("Start a Analysis", AbstractImagePrototype.create(icons.addSquare16()));
		createAnalysisButton.addSelectionListener(new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {								
				startAnalysisWindow = new Window();  
				startAnalysisWindow.setSize(600, 240);
				startAnalysisWindow.setPlain(true);
				startAnalysisWindow.setModal(true);
				startAnalysisWindow.setBlinkModal(true);
				startAnalysisWindow.setHeading("Start a Analysis");
				startAnalysisWindow.setLayout(new FitLayout());								
				nodeEditor.addCancelHandler(new CancelHandler() {					
					@Override
					public void onCancel(CancelEvent event) {
						startAnalysisWindow.hide();
					}
				});
				nodeEditor.addPersistSuccessHandler(new EntityUpdatedHandler() {					
					@Override
					public void onPersistSuccess(EntityUpdatedEvent event) {
						startAnalysisWindow.hide();
						queryServiceTable.refreshFromServer();
					}
				});
				nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
				startAnalysisWindow.add(nodeEditor.asWidget(NodeType.ANALYSIS), new FitData(4));						
				startAnalysisWindow.show();			
			}
		});
		createAnalysisButtonPanel.clear();
		createAnalysisButtonPanel.add(createAnalysisButton);		
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
		if(startAnalysisWindow != null) startAnalysisWindow.hide();
	}

}
