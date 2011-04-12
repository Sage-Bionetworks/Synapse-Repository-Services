package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.filter.QueryFilter.SelectionListner;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetsHomeViewImpl extends Composite implements DatasetsHomeView {

	public interface DatasetsHomeViewImplUiBinder extends 	UiBinder<Widget, DatasetsHomeViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;	
	@UiField
	SimplePanel tablePanel;
	//ContentPanel tablePanel;
	@UiField
	Anchor addColumnsAnchor;
	@UiField
	SimplePanel filterPanel;
	
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	
	@Inject
	public DatasetsHomeViewImpl(DatasetsHomeViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle icons, QueryFilter filter, SageImageBundle imageBundle, QueryServiceTableResourceProvider queryServiceTableResourceProvider) {		
		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.dataset, true, 1000, 480);
		ImageResource searchIR = imageBundle.searchButtonIcon();
		initWidget(binder.createAndBindUi(this));

		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
				
		// Start on the first page and trigger a data fetch from the server
		queryServiceTable.pageTo(0, 10);

		// Add the table
		tablePanel.add(queryServiceTable.asWidget());
		// Add the filter
		filterPanel.add(filter);
		
		addColumnsAnchor.setHTML(AbstractImagePrototype.create(icons.tableInsertColumn16()).getHTML() + "&nbsp;<span class=\"add_remove\">Add / remove columns</span>");
		addColumnsAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				presenter.onEditColumns();
			}
		});		
				
		// We need to listen to filter changes
		filter.addSelectionListner(new SelectionListner() {
			
			@Override
			public void selectionChanged(List<WhereCondition> newConditions) {
				if(newConditions.size() < 1){
					queryServiceTable.setWhereCondition(null);
				}else{
					queryServiceTable.setWhereCondition(newConditions);
				}
			}
		});
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}


	@Override
	public void setVisibleColumns(List<String> visible) {
		this.queryServiceTable.setDispalyColumns(visible);
	}


	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}

}
