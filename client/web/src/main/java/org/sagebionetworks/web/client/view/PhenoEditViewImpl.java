package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PhenoEditViewImpl extends Composite implements PhenoEditView {

	public interface PhenoEditViewImplUiBinder extends UiBinder<Widget, PhenoEditViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel phenotypeEditorPanel;
	@UiField
	SpanElement breadcrumbDatasetSpan;
	@UiField
	SpanElement breadcrumbLayerSpan;
	@UiField
	SpanElement breadcrumbTitleSpan;

	private Presenter presenter;
	private IconsImageBundle icons;
	private Header headerWidget;
	private PhenotypeEditor phenotypeEditor;

	@Inject
	public PhenoEditViewImpl(PhenoEditViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle, PhenotypeEditor phenotypeEditor) {		
		initWidget(binder.createAndBindUi(this));
		this.icons = icons;
		this.headerWidget = headerWidget;
		this.phenotypeEditor = phenotypeEditor;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);

	}

	@Override
	public void setEditorDetails(String layerId, String layerName, String layerLink, String datasetLink) {
		breadcrumbLayerSpan.setInnerHTML(layerLink);
		breadcrumbDatasetSpan.setInnerHTML(datasetLink); 
		breadcrumbTitleSpan.setInnerText(layerName);		
		
		// setup and add phenotype editor
		phenotypeEditor.setLayerId(layerId);
		//phenotypeEditor.setPlaceChanger(presenter.getPlaceChanger());		
		phenotypeEditorPanel.clear();		
		phenotypeEditorPanel.add(phenotypeEditor.asWidget());		
	}
	
	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();				
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}
}
