package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableView.StaticSelectionMode;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteData;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

public class PhenotypeEditorViewImpl extends LayoutContainer implements PhenotypeEditorView {
	
	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private StaticTable staticTable;
	private static int CONTENT_WIDTH_PX = 1000;
	private static int CONTENT_HEIGHT_PX = 600;	
		
	@Inject
	public PhenotypeEditorViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle, StaticTable staticTable) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.staticTable = staticTable;
		
		this.setLayout(new FitLayout());
		this.setWidth(CONTENT_WIDTH_PX);
		this.setHeight(CONTENT_HEIGHT_PX);		
	}
	
	@Override
	public void generatePhenotypeEditor(List<String> columns, String identityColumn, List<Map<String, String>> phenoData, Collection<Ontology> ontologies, ColumnDefinitionEditor columnDefinitionEditor, ColumnMappingEditor columnMappingEditor) {
		this.removeAll();
		this.setLayout(new FitLayout());
		this.setWidth(CONTENT_WIDTH_PX);
		this.setHeight(CONTENT_HEIGHT_PX);

		// top view of existing data
		StaticTable dataPreviewTable = createDataPreviewGrid(columns, phenoData);

		// edit column mapping panel 
		columnMappingEditor.disable();
		
		// tie panels together
		ContentPanel phenotypeEditorPanel = createPhenotypeEditorPanel(dataPreviewTable, columnDefinitionEditor, columnMappingEditor);
				
		this.add(phenotypeEditorPanel);		
		this.layout(true);		
	}

	/**
	 * Creates the preview grid at the top of the page
	 * @param phenoData 
	 * @param columns 
	 * @return
	 */
	private StaticTable createDataPreviewGrid(List<String> columns, List<Map<String,String>> phenoData) {		
		staticTable.setColumnOrder(columns);
		staticTable.setData(phenoData);
		staticTable.setSelectionMode(StaticSelectionMode.CELL);
		staticTable.setDimensions(973, 196);
		return staticTable;
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showLoading() {
		this.removeAll();
		this.add(DisplayUtils.getLoadingWidget(sageImageBundle));
		this.layout(true);
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showInfo(String title, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showErrorMessage(String message) {
		// TODO Auto-generated method stub
		
	}

	
	/*
	 * Private Methods
	 */
	private ContentPanel createPhenotypeEditorPanel(
			StaticTable dataPreviewTable, ColumnDefinitionEditor columnDefinitionEditor, ColumnMappingEditor columnMappingEditor) {
		ContentPanel phenotypeEditorPanel = new ContentPanel();
		phenotypeEditorPanel.setHeading("Phenotype Editor");
		phenotypeEditorPanel.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
		phenotypeEditorPanel.setButtonAlign(HorizontalAlignment.RIGHT);
		phenotypeEditorPanel.addStyleName(DisplayConstants.STYLE_NAME_GXT_GREY_BACKGROUND);
		phenotypeEditorPanel.setWidth(CONTENT_WIDTH_PX);
		phenotypeEditorPanel.setHeight(CONTENT_HEIGHT_PX);
		phenotypeEditorPanel.setScrollMode(Scroll.AUTOY);		

		// add grid and panels 
		phenotypeEditorPanel.add(dataPreviewTable.asWidget());
		phenotypeEditorPanel.add(columnDefinitionEditor.asWidget());
		//phenotypeEditorPanel.add(columnMappingEditor.asWidget());		
		
		return phenotypeEditorPanel;
	}



}

