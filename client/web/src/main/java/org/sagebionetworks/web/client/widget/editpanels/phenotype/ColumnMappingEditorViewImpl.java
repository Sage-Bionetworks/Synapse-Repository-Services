package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
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


	private static final String COLUMN_COLUMN_VALUE = "columnValue";
	private static final String COLUMN_CONSTRAINT_VALUE = "constraintValue";
	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private static int CONTENT_WIDTH_PX = 973;
	private static int CONTENT_HEIGHT_PX = 150;	

	private Button deleteColumnMappingButton;
	private EditorGrid<BaseModelData> editColumnMappingGrid;
	private ListStore<BaseModelData> store;
	private ColumnModel columnModel;

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
		
		fillStoreFakeData();
		createColumnModelFakeData();
		editColumnMappingGrid = new EditorGrid<BaseModelData>(store, columnModel);
		editColumnMappingGrid.setBorders(true);
		editColumnMappingGrid.setAutoExpandColumn(COLUMN_CONSTRAINT_VALUE);
		editColumnMappingGrid.setStripeRows(true);
		editColumnMappingPanel.setLayout(new FitLayout());
		editColumnMappingPanel.setSize(CONTENT_WIDTH_PX, CONTENT_HEIGHT_PX);		
		ToolBar columnMappingToolBar = createColumnMappingToolbarAndButtons(editColumnMappingGrid);				
		editColumnMappingPanel.add(editColumnMappingGrid);
		editColumnMappingPanel.setBottomComponent(columnMappingToolBar);
		
		this.add(editColumnMappingPanel);
	}

	private void createColumnModelFakeData() {
		List<ColumnConfig> columnConfigs = new ArrayList<ColumnConfig>();
		ColumnConfig col;
		
		col = new ColumnConfig(COLUMN_COLUMN_VALUE, "Column Value", 180);
		columnConfigs.add(col);
		
		col = new ColumnConfig(COLUMN_CONSTRAINT_VALUE, "Constraint Value", 180);		
		columnConfigs.add(col);
		
		columnModel = new ColumnModel(columnConfigs);		
	}

	private void fillStoreFakeData() {
		store = new ListStore<BaseModelData>();
		BaseModelData model;
		
		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "A");
		model.set(COLUMN_CONSTRAINT_VALUE, "A");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "a");
		model.set(COLUMN_CONSTRAINT_VALUE, "A");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "c");
		model.set(COLUMN_CONSTRAINT_VALUE, "C");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "C");
		model.set(COLUMN_CONSTRAINT_VALUE, "C");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "A1");
		model.set(COLUMN_CONSTRAINT_VALUE, "A1");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "C1");
		model.set(COLUMN_CONSTRAINT_VALUE, "C1");		
		store.add(model);

		model = new BaseModelData();
		model.set(COLUMN_COLUMN_VALUE, "C2");
		model.set(COLUMN_CONSTRAINT_VALUE, "C2");		
		store.add(model);

	}
	
	@Override
	public void showLoading() {
		ContentPanel cp = DisplayUtils.getLoadingWidget(sageImageBundle);
		cp.setSize(CONTENT_WIDTH_PX, CONTENT_HEIGHT_PX);		
		this.add(cp);
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
	
	public void setHeight(int height) {
		CONTENT_HEIGHT_PX = height;
		super.setHeight(height);
	}
	
	public void setWidth(int width) {
		CONTENT_WIDTH_PX = width;		
		super.setWidth(width);
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
