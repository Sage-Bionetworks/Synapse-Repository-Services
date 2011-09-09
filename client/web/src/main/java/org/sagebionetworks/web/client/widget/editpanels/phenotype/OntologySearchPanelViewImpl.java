package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.SynapseWidgetView;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteData;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.inject.Inject;


public class OntologySearchPanelViewImpl extends LayoutContainer implements OntologySearchPanelView, SynapseWidgetView {

	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private static int CONTENT_WIDTH_PX = 550;
	private static int CONTENT_HEIGHT_PX = 325;	

	private ContentPanel ontologySearchPanel;
	private Grid<BaseModelData> searchResultsGrid;
	private ColumnModel columnModel;


	@Inject
	public OntologySearchPanelViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		
		this.setLayout(new FitLayout());
		this.setWidth(CONTENT_WIDTH_PX);
		this.setHeight(CONTENT_HEIGHT_PX);		
	}

	@Override
	public void createWidget() {
		ontologySearchPanel = new ContentPanel();
		ontologySearchPanel.setHeading("Ontology Selection");
		ontologySearchPanel.setCollapsible(true);
		ontologySearchPanel.setLayout(new AbsoluteLayout());
		ontologySearchPanel.setSize(550, 300);
		
		final TextField<String> searchField = new TextField<String>();
		searchField.setEmptyText("Enter term, e.g. Melanoma");
		ontologySearchPanel.add(searchField, new AbsoluteData(6, 32));
		searchField.setSize("270px", "22px");
		searchField.setFieldLabel("Search ");
		
		Text txtSearchAllOntologies = new Text("Search all ontologies");
		ontologySearchPanel.add(txtSearchAllOntologies, new AbsoluteData(6, 11));
		
		columnModel = new ColumnModel(createSearchResultConfigs());
		searchResultsGrid = new Grid<BaseModelData>(new ListStore<BaseModelData>(), columnModel);
		ontologySearchPanel.add(searchResultsGrid, new AbsoluteData(6, 89));
		searchResultsGrid.setSize("480px", "169px");
		searchResultsGrid.setBorders(true);		
		
		Button btnSearch = new Button("Search", AbstractImagePrototype.create(iconsImageBundle.magnify16()));
		ontologySearchPanel.add(btnSearch, new AbsoluteData(282, 32));
		btnSearch.setSize("68px", "22px");		
		btnSearch.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.executeSearch(searchField.getValue());											
			}
		});

		
		Text txtSearchResults = new Text("Search Results");
		ontologySearchPanel.add(txtSearchResults, new AbsoluteData(6, 67));
		add(ontologySearchPanel);

		

	}
	
	@Override
	public void showLoading() {
		this.clear();
		this.add(DisplayUtils.getLoadingWidget(sageImageBundle));
		this.layout(true);
	}

	@Override
	public void clear() {
		this.removeAll(true);
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
		
	@Override
	public void setSearchResults() {
		// TODO : this method will change to receive parameters of search results		
		searchResultsGrid.reconfigure(loadFakeSearchResults(), columnModel);
	}

	
	/*
	 * Private Methods
	 */
	private List<ColumnConfig> createSearchResultConfigs() {
		List<ColumnConfig> colConfigs = new ArrayList<ColumnConfig>();
        ColumnConfig colConfig;       

        colConfig = new ColumnConfig("select", "Select", 60);
        colConfig.setRenderer(createSelectRenderer(null));
        colConfigs.add(colConfig);

        colConfig = new ColumnConfig("term_name", "Term Name", 120);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig("ontology", "Ontology", 195);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig("details", "Details", 50);
        colConfig.setRenderer(createDetailsRenderer(null));
        colConfigs.add(colConfig);
                
        colConfig = new ColumnConfig("visualize", "Visualize", 50);
        colConfig.setRenderer(createVisualizeRenderer(null));
        colConfigs.add(colConfig);        
		
		return colConfigs;
	}	

	private GridCellRenderer<BaseModelData> createSelectRenderer(final Collection<Ontology> ontologies) {
		GridCellRenderer<BaseModelData> buttonRenderer = new GridCellRenderer<BaseModelData>() {
			@Override
			public Object render(final BaseModelData model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<BaseModelData> store,
					Grid<BaseModelData> grid) {
				final BaseModelData entry = store.getAt(rowIndex);
				Button selectButton = new Button("Select");					
				return selectButton;
			}
		};

		return buttonRenderer;
	}

	private GridCellRenderer<BaseModelData> createDetailsRenderer(final Collection<Ontology> ontologies) {
		GridCellRenderer<BaseModelData> buttonRenderer = new GridCellRenderer<BaseModelData>() {
			@Override
			public Object render(final BaseModelData model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<BaseModelData> store,
					Grid<BaseModelData> grid) {
				final BaseModelData entry = store.getAt(rowIndex);
				Anchor removeAnchor = new Anchor();
				removeAnchor.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.details16()));
				return removeAnchor;					
			}
		};

		return buttonRenderer;
	}

	private GridCellRenderer<BaseModelData> createVisualizeRenderer(final Collection<Ontology> ontologies) {
		GridCellRenderer<BaseModelData> buttonRenderer = new GridCellRenderer<BaseModelData>() {
			@Override
			public Object render(final BaseModelData model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<BaseModelData> store,
					Grid<BaseModelData> grid) {
				final BaseModelData entry = store.getAt(rowIndex);
				Anchor removeAnchor = new Anchor();
				removeAnchor.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.visualize16()));
				return removeAnchor;
			}
		};

		return buttonRenderer;
	}

	/*
	 * DEMO
	 */
	private ListStore<BaseModelData> loadFakeSearchResults() {
		ListStore<BaseModelData> store = new ListStore<BaseModelData>();
		BaseModelData data;
		
		data = new BaseModelData();
		data.set("term_name", "melanoma");
		data.set("ontology", "CRISP Thesaurus, 2006");
		store.add(data);

		data = new BaseModelData();
		data.set("term_name", "Melanoma");
		data.set("ontology", "MedDRA");
		store.add(data);

		data = new BaseModelData();
		data.set("term_name", "melanoma");
		data.set("ontology", "NCI Thesaurus");
		store.add(data);
		return store;

	}
	
}
