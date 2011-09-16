package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collections;

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


public class PhenotypeMatrixViewImpl extends LayoutContainer implements PhenotypeMatrixView {


	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private int contentWidthPx = 1000; // defaults
	private int contentHeightPx = 600;	

	private Button deleteColumnMappingButton;
	private EditorGrid<BaseModelData> phenotypeMatrixGrid;


	@Inject
	public PhenotypeMatrixViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		
		this.setLayout(new FitLayout());
		this.setWidth(contentWidthPx);
		this.setHeight(contentHeightPx);		
	}

	@Override
	public void createWidget() {
		this.removeAll(true);

		ContentPanel matrixViewPanel = new ContentPanel();
		matrixViewPanel.setHeaderVisible(false);
		
		phenotypeMatrixGrid = new EditorGrid<BaseModelData>(new ListStore<BaseModelData>(), new ColumnModel(Collections.<ColumnConfig>emptyList()));
		phenotypeMatrixGrid.setBorders(true);
		matrixViewPanel.setLayout(new FitLayout());
		matrixViewPanel.setSize(contentWidthPx, contentHeightPx);
		
		matrixViewPanel.add(phenotypeMatrixGrid);
		
		this.add(matrixViewPanel);
	}
	
	@Override
	public void showLoading() {
		ContentPanel cp = DisplayUtils.getLoadingWidget(sageImageBundle);
		cp.setSize(contentWidthPx, contentHeightPx);		
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
		contentHeightPx = height;
		super.setHeight(height);
	}
	
	public void setWidth(int width) {
		contentWidthPx = width;		
		super.setWidth(width);
	}
	
	
	
	
	/*
	 * Private Methods
	 */	
	
}
