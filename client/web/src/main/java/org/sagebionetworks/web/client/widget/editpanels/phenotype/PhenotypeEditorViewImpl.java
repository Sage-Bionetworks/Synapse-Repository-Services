package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableView.StaticSelectionMode;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

public class PhenotypeEditorViewImpl extends LayoutContainer implements PhenotypeEditorView {
	
	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private StaticTable staticTable;
	private int contentWidthPx = 1000;
	private int contentHeightPx = 600;	
	private final static int INNER_PANEL_ADJUST_PX = 5;
	private final static int INNER_PANEL_PADDING_PX = 5;
	private final static int TOP_RIGHT_COMPONENT_WIDTH_PX = 400;
	private final static int TOP_ROW_HEIGHT_PX = 200;
	private final static int BOTTOM_ROW_HEIGHT_PX = 330;
	private boolean editViewShowing;
	private ToolBar toolbar;
		
	@Inject
	public PhenotypeEditorViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle, StaticTable staticTable) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.staticTable = staticTable;
		
		this.setLayout(new FitLayout());
		this.setWidth(contentWidthPx);
		this.setHeight(contentHeightPx);		
	}
	
	@Override
	public void generatePhenotypeEditor(List<String> columns,
			String identityColumn, List<Map<String, String>> phenoData,
			Collection<Ontology> ontologies,
			final ColumnDefinitionEditor columnDefinitionEditor,
			final ColumnMappingEditor columnMappingEditor,
			final PhenotypeMatrix phenotypeMatrix) {
		this.removeAll();
		this.setLayout(new FitLayout());
		this.setWidth(contentWidthPx);
		this.setHeight(contentHeightPx);

		// start with phenotype matrix
		editViewShowing = true;		
		
		// top view of existing data
		final StaticTable dataPreviewTable = createDataPreviewGrid(columns, phenoData);				
		// tie panels together
		final ContentPanel phenotypeEditorPanel = createPhenotypeEditorPanel(dataPreviewTable, columnDefinitionEditor, columnMappingEditor, phenotypeMatrix);		
		createToolbar(phenotypeEditorPanel, dataPreviewTable, columnDefinitionEditor, columnMappingEditor, phenotypeMatrix);
		phenotypeEditorPanel.setTopComponent(toolbar);				

		//		// start with phenoMatrix first
//		addMatrixToPanel(phenotypeEditorPanel, phenotypeMatrix);
		
		addEditToPanel(phenotypeEditorPanel, dataPreviewTable, columnDefinitionEditor, columnMappingEditor);		
		this.add(phenotypeEditorPanel);			
		this.layout(true);		
	}

	private void addMatrixToPanel(ContentPanel phenotypeEditorPanel, PhenotypeMatrix phenotypeMatrix) {
		phenotypeEditorPanel.removeAll();
		
		phenotypeEditorPanel.add(phenotypeMatrix.asWidget(), new MarginData(INNER_PANEL_PADDING_PX));		
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
		staticTable.setDimensions(contentWidthPx - INNER_PANEL_ADJUST_PX - 2*INNER_PANEL_PADDING_PX - TOP_RIGHT_COMPONENT_WIDTH_PX, TOP_ROW_HEIGHT_PX);
		staticTable.setTitle("Data Preivew");		
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
			StaticTable dataPreviewTable, ColumnDefinitionEditor columnDefinitionEditor, ColumnMappingEditor columnMappingEditor, PhenotypeMatrix phenotypeMatrix) {
		ContentPanel phenotypeEditorPanel = new ContentPanel(new FlowLayout());
		phenotypeEditorPanel.setHeading("Phenotype Editor");		
		phenotypeEditorPanel.setButtonAlign(HorizontalAlignment.RIGHT);		
		phenotypeEditorPanel.setWidth(contentWidthPx);
		phenotypeEditorPanel.setHeight(contentHeightPx);
		phenotypeEditorPanel.setScrollMode(Scroll.AUTO);		
		phenotypeEditorPanel.setBodyStyle("background-color: #e8e8e8");					
						
		return phenotypeEditorPanel;
	}

	private void addEditToPanel(ContentPanel phenotypeEditorPanel,
			StaticTable dataPreviewTable,
			ColumnDefinitionEditor columnDefinitionEditor,
			ColumnMappingEditor columnMappingEditor) {
		
		phenotypeEditorPanel.removeAll();
		
		// size widgets
		columnDefinitionEditor.setWidth(contentWidthPx - INNER_PANEL_ADJUST_PX - 2*INNER_PANEL_PADDING_PX);
		columnDefinitionEditor.setHeight(BOTTOM_ROW_HEIGHT_PX);
		columnMappingEditor.setWidth(TOP_RIGHT_COMPONENT_WIDTH_PX - INNER_PANEL_ADJUST_PX - INNER_PANEL_PADDING_PX);
		columnMappingEditor.setHeight(TOP_ROW_HEIGHT_PX);
				
		// add grid and panels 
		LayoutContainer topRow = new LayoutContainer();
		HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));  
		flex.setFlex(1);
		topRow.setWidth(contentWidthPx - INNER_PANEL_ADJUST_PX);
		HBoxLayout layout = new HBoxLayout();
		layout.setPadding(new Padding(0, INNER_PANEL_PADDING_PX, 0, 0));
		topRow.setLayout(layout);
		topRow.add(dataPreviewTable.asWidget(), flex);
		topRow.add(columnMappingEditor.asWidget(), flex);		
		phenotypeEditorPanel.add(topRow, new MarginData(INNER_PANEL_PADDING_PX));
		
		phenotypeEditorPanel.add(columnDefinitionEditor.asWidget(), new MarginData(INNER_PANEL_PADDING_PX));
	}

	private void createToolbar(final ContentPanel phenotypeEditorPanel, final StaticTable dataPreviewTable, final ColumnDefinitionEditor columnDefinitionEditor,
			final ColumnMappingEditor columnMappingEditor,
			final PhenotypeMatrix phenotypeMatrix) {
		// setup toolbar
		toolbar = new ToolBar();
		
		// TODO :REWORK THIS with more time
		
		final Button firstToolbarButton = new Button("Matrix View", AbstractImagePrototype.create(iconsImageBundle.applicationForm16()));
		firstToolbarButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {				
				if(editViewShowing) {
//					// fill panel with editors
//					addEditToPanel(phenotypeEditorPanel, dataPreviewTable, columnDefinitionEditor, columnMappingEditor);
//
//					firstToolbarButton.setTitle("Edit Column Definitions");
//					firstToolbarButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentEdit16()));
				} else {
//					// fill panel with matrix
//					addMatrixToPanel(phenotypeEditorPanel, phenotypeMatrix);
//					
//					firstToolbarButton.setTitle("Matrix View");
//					firstToolbarButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationForm16()));
				}
			}
		});

		toolbar.add(firstToolbarButton);		
	}
	
}

