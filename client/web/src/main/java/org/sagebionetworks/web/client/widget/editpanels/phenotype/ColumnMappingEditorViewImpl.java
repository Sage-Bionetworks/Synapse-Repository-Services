package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collections;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;


public class ColumnMappingEditorViewImpl extends LayoutContainer implements ColumnMappingEditorView {


	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private static int CONTENT_WIDTH_PX = 973;
	private static int CONTENT_HEIGHT_PX = 150;	

	private Button deleteColumnMappingButton;


	@Inject
	public ColumnMappingEditorViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		
		this.setLayout(new FitLayout());
		this.setWidth(CONTENT_WIDTH_PX);
		this.setHeight(CONTENT_HEIGHT_PX);		
	}

	@Override
	public void createWidget() {
		this.removeAll(true);

		ContentPanel editColumnMappingPanel = new ContentPanel();
		editColumnMappingPanel.setHeading("Column Mapping Editor");
		
		EditorGrid<BaseModelData> editColumnMappingGrid = new EditorGrid<BaseModelData>(new ListStore<BaseModelData>(), new ColumnModel(Collections.<ColumnConfig>emptyList()));
		editColumnMappingGrid.setBorders(true);
		editColumnMappingPanel.setLayout(new FitLayout());
		editColumnMappingPanel.setSize(CONTENT_WIDTH_PX, CONTENT_HEIGHT_PX);
		ToolBar columnMappingToolBar = createColumnMappingToolbarAndButtons(editColumnMappingGrid);		
		
		editColumnMappingPanel.add(editColumnMappingGrid);
		editColumnMappingPanel.setBottomComponent(columnMappingToolBar);
		
		this.add(editColumnMappingPanel);
	}
	
	@Override
	public void showLoading() {
		// TODO Auto-generated method stub
		
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

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;		
	}
		
	
	/*
	 * Private Methods
	 */
	
	private ToolBar createColumnMappingToolbarAndButtons(EditorGrid<BaseModelData> editColumnMappingGrid) {
		// setup toolbar
		ToolBar toolBar = new ToolBar();
		Button addButton = new Button("Add Mapping");
		addButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
		addButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				// TODO : add column
			}
		});
		toolBar.add(addButton);
		
		deleteColumnMappingButton = new Button("Delete Mapping");
		deleteColumnMappingButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
		deleteColumnMappingButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
//				GridSelectionModel<EditableAnnotationModelData> selectionModel = grid.getSelectionModel();
//				EditableAnnotationModelData model = selectionModel.getSelectedItem();												
//				selectionModel.deselectAll();
				deleteColumnMappingButton.disable();
			}
		});
		deleteColumnMappingButton.disable();
		toolBar.add(deleteColumnMappingButton);
		return toolBar;
	}
	
}
