package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.button.Button;
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
	SimplePanel backToLayerButtonPanel;
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
		
		backToLayerButtonPanel.clear();
		backToLayerButtonPanel.add(createBackToLayerButton(layerId));
		
	}
	
	private Widget createBackToLayerButton(String layerId) {
		Button returnButton = new Button(DisplayConstants.BUTTON_RETURN_TO_LAYER, AbstractImagePrototype.create(icons.NavigateLeft16()));
		returnButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goBackToLayer();
			}
		});
		return returnButton;
	}

	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();				
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
	}
	
}
