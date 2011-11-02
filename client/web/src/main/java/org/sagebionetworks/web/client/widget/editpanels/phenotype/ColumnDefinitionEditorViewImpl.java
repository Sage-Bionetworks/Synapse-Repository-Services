package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Enumeration;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteData;
import com.extjs.gxt.ui.client.widget.layout.AbsoluteLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.inject.Inject;


public class ColumnDefinitionEditorViewImpl extends LayoutContainer implements ColumnDefinitionEditorView {

	private static final String COLUMN_DEF_EDIT_MAPPING_KEY = "editMapping";
	private static final String COLUMN_DEF_TYPE_KEY = "columnOntology";
	private static final String COLUMN_DEF_CONSTRAINT_KEY = "columnConstraint";
	private static final String COLUMN_DEF_UNITS_KEY = "columnUnits";
	private static final String COLUMN_DEF_DESCRIPTION_KEY = "columnDescription";
	private static final String COLUMN_DEF_COLUMN_NAME_KEY = "columnName";
	private static final String COLUMN_REMOVE_COLUMN_KEY = "remove";
	private static final String TYPE_COMBO_ONTOLOGY = "Ontology";
	private static final String TYPE_COMBO_FREETEXT = "Free Text";
	private static final String TYPE_COMBO_ENUM = "Enumeration";
	private static final String TYPE_COMBO_NUMBER = "Number";
	private static final String TYPE_COMBO_INTEGER = "Integer";
	private static final String TYPE_COMBO_BOOLEAN = "Boolean";


	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private StaticTable staticTable;
	private static int CONTENT_WIDTH_PX = 973;
	private static int CONTENT_HEIGHT_PX = 298;	

	private ColumnModel columnDefColumnModel;
	private ListStore<ColumnDefinitionTableModel> columnDefGridStore;
	private EditorGrid<ColumnDefinitionTableModel> columnDefGrid;
	private OntologySearchPanel ontologySearchPanel;

	private Grid<BaseModelData> fakeSearchResultsGrid;
	
	@Inject
	public ColumnDefinitionEditorViewImpl(IconsImageBundle iconsImageBundle, SageImageBundle sageImageBundle, StaticTable staticTable, OntologySearchPanel ontologySearchPanel) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.staticTable = staticTable;
		this.ontologySearchPanel = ontologySearchPanel;
		
		this.setLayout(new FitLayout());
		this.setWidth(CONTENT_WIDTH_PX);
		this.setHeight(CONTENT_HEIGHT_PX);		
	}

	@Override
	public void createWidget(List<String> columns, String identityColumn, Map<String, String> columnToOntology, Collection<Enumeration> ontologies) {
		this.removeAll(true);
		
		ContentPanel columnDefPanel = new ContentPanel();
		columnDefPanel.setHeading("Column Definitions");
		
		// cell renderes
		GridCellRenderer<ColumnDefinitionTableModel> ontologyRenderer = createOntologyRenderer(ontologies);
		GridCellRenderer<ColumnDefinitionTableModel> constraintRenderer = createConstraintRenderer(ontologies);
		GridCellRenderer<ColumnDefinitionTableModel> unitsRenderer = createUnitsRenderer(ontologies);
		GridCellRenderer<ColumnDefinitionTableModel> descriptionRenderer = createDescriptionRenderer(ontologies);
		GridCellRenderer<ColumnDefinitionTableModel> removeRenderer = createRemoveRenderer();						   

		// build column model & fill store
        List<ColumnConfig> colConfigs = createColumnDefConfigs(ontologyRenderer, removeRenderer, constraintRenderer, unitsRenderer, descriptionRenderer);          
		columnDefColumnModel = new ColumnModel(colConfigs);
		fillColumnDefGridStore(columns, identityColumn);
		
		// create grid
		columnDefGrid = new EditorGrid<ColumnDefinitionTableModel>(columnDefGridStore, columnDefColumnModel);
		columnDefGrid.setBorders(true);
		columnDefGrid.setStripeRows(true);
		columnDefGrid.setAutoExpandColumn(COLUMN_DEF_COLUMN_NAME_KEY);
		GridSelectionModel<ColumnDefinitionTableModel> selectionModel = new GridSelectionModel<ColumnDefinitionTableModel>();
		columnDefGrid.setSelectionModel(new GridSelectionModel<ColumnDefinitionTableModel>()); // row select
		
		columnDefPanel.setLayout(new FitLayout());
		columnDefPanel.setSize(CONTENT_WIDTH_PX, CONTENT_HEIGHT_PX);
		ToolBar idSelectorToolBar = createIdSelectorToolBar(columns);
		ToolBar columnDefToolBar = createColumnDefToolbarAndButtons(columnDefGrid);		
				
		// add to panel
		columnDefPanel.setTopComponent(idSelectorToolBar);
		columnDefPanel.add(columnDefGrid);
		columnDefPanel.setBottomComponent(columnDefToolBar);
		
		this.add(columnDefPanel);
		
		// configure the search panel
		ontologySearchPanel.setResources();		
	}

	@Override
	public void refresh(List<String> columns, String identityColumn,
			Map<String, String> columnToOntology) {
		// TODO Auto-generated method stub
		
	}

	public void addColumnDefinitionSelectionListener(@SuppressWarnings("rawtypes") SelectionChangedListener listener) {
		columnDefGrid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<ColumnDefinitionEditorViewImpl.ColumnDefinitionTableModel>() {

			@Override
			public void selectionChanged(SelectionChangedEvent<ColumnDefinitionTableModel> se) {
				
			}
		});
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
	private List<ColumnConfig> createColumnDefConfigs(
			GridCellRenderer<ColumnDefinitionTableModel> ontologyRenderer,
			GridCellRenderer<ColumnDefinitionTableModel> removeRenderer, GridCellRenderer<ColumnDefinitionTableModel> constraintRenderer, GridCellRenderer<ColumnDefinitionTableModel> unitsRenderer, GridCellRenderer<ColumnDefinitionTableModel> descriptionRenderer) {
		List<ColumnConfig> colConfigs = new ArrayList<ColumnConfig>();
        ColumnConfig colConfig;

        colConfig = new ColumnConfig(COLUMN_DEF_COLUMN_NAME_KEY, "Column Name", 170);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig(COLUMN_DEF_TYPE_KEY, "Type", 120);
        colConfig.setRenderer(ontologyRenderer);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig(COLUMN_DEF_CONSTRAINT_KEY, "Constraint", 195);
        colConfig.setRenderer(constraintRenderer);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig(COLUMN_DEF_UNITS_KEY, "Units", 100);
        colConfig.setRenderer(unitsRenderer);
        colConfigs.add(colConfig);
        
        colConfig = new ColumnConfig(COLUMN_DEF_DESCRIPTION_KEY, "Description", 205);
        colConfig.setRenderer(descriptionRenderer);
        colConfigs.add(colConfig);
        
        // delete button
        colConfig = new ColumnConfig();  
        colConfig.setId(COLUMN_REMOVE_COLUMN_KEY);  
        colConfig.setHeader("");  
        colConfig.setAlignment(HorizontalAlignment.RIGHT);  
        colConfig.setWidth(25);  
        colConfig.setRenderer(removeRenderer);  
		colConfigs.add(colConfig);
		
		return colConfigs;
	}	
	
	private void fillColumnDefGridStore(List<String> columns, String identityColumn) {
		columnDefGridStore = new ListStore<ColumnDefinitionTableModel>();		
		// add columns to grid store

		// TODO : remove this demo stuff:
		int i=0;
		for(String colName : columns) {
			ColumnDefinitionTableModel row = new ColumnDefinitionTableModel(colName, "", false);
			if(colName.equals(identityColumn)) {
				row.setIdColumn(true);
			}
			
			if(i==0) {
				row.set(COLUMN_DEF_TYPE_KEY, "Enumeration");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "Yes,No");
			}
						
			if(i==1) {
				row.set(COLUMN_DEF_TYPE_KEY, "Enumeration");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "Yes,No");
			}

			if(i==2) {
				row.set(COLUMN_DEF_TYPE_KEY, "Enumeration");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "A,C,A1,C1,A2");
			}
						
			if(i==3) {
				row.set(COLUMN_DEF_TYPE_KEY, "Ontology");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "NCI Metathesaurus");
			}
					
			if(i==4) {
				row.set(COLUMN_DEF_TYPE_KEY, "Enumeration");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "Adenoma,Carcinoma");
			}
			
			if(i==5) {
				row.set(COLUMN_DEF_TYPE_KEY, "Ontology");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "NCI Thesaurus");
			}
			
			if(i==6) {
				row.set(COLUMN_DEF_TYPE_KEY, "Number");
				row.set(COLUMN_DEF_CONSTRAINT_KEY, "0-100");
				row.set(COLUMN_DEF_UNITS_KEY, "mm");
			}
			
			columnDefGridStore.add(row);
			i++;
		}
		
		

		
	}
	
	private ToolBar createIdSelectorToolBar(List<String> columns) {
		// setup toolbar
		ToolBar toolBar = new ToolBar();
		SimpleComboBox<String> combo = new SimpleComboBox<String>();
		if(columns != null && columns.size() > 0) {
			combo.add(columns);
			combo.setSimpleValue(columns.get(0));
			presenter.setIdentityColumn(columns.get(0));
		}
		combo.disableTextSelection(true);
		combo.setEditable(false);
		combo.setTypeAhead(false);
		combo.setTriggerAction(TriggerAction.ALL);
		combo.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				SimpleComboValue<String> selected = se.getSelectedItem();
				if(selected != null && selected.getValue() != null)
				presenter.setIdentityColumn(selected.getValue());
			}
		});
		toolBar.add(new Label(DisplayUtils.getIconHtml(iconsImageBundle.tableInsertColumn16()) + "&nbsp;Select Identity Column :&nbsp;&nbsp;"));
		toolBar.add(combo);
		return toolBar;
	}
	
	private ToolBar createColumnDefToolbarAndButtons(final EditorGrid<ColumnDefinitionTableModel> grid) {
		// setup toolbar
		ToolBar toolBar = new ToolBar();
		Button addButton = new Button("Add Column");
		addButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
		addButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				// TODO : add column
			}
		});
		toolBar.add(addButton);
		
		return toolBar;
	}
	
	private GridCellRenderer<ColumnDefinitionTableModel> createOntologyRenderer(final Collection<Enumeration> ontologies) {
		GridCellRenderer<ColumnDefinitionTableModel> buttonRenderer = new GridCellRenderer<ColumnDefinitionTableModel>() {
			private boolean init;

			@Override
			public Object render(final ColumnDefinitionTableModel model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<ColumnDefinitionTableModel> store,
					Grid<ColumnDefinitionTableModel> grid) {
				final ColumnDefinitionTableModel entry = store.getAt(rowIndex);
				if (!init) {
					init = true;
					grid.addListener(Events.ColumnResize, new Listener<GridEvent<ColumnDefinitionTableModel>>() {
						public void handleEvent(GridEvent<ColumnDefinitionTableModel> be) {
							for (int i = 0; i < be.getGrid().getStore().getCount(); i++) {
								if (be.getGrid().getView().getWidget(i, be.getColIndex()) != null
										&& be.getGrid().getView().getWidget(i, be.getColIndex()) instanceof BoxComponent) {
									((BoxComponent) be.getGrid().getView().getWidget(i, be.getColIndex())).setWidth(be.getWidth() - 10);
								}
							}
						}
					});
				}
				if (entry.isIdColumn()) {
					return getIdentityColumnLabel();					
				} else {
					SimpleComboBox<String> comboBox = new SimpleComboBox<String>();
					comboBox.setTriggerAction(TriggerAction.ALL);
					comboBox.setWidth(grid.getColumnModel().getColumnWidth(colIndex) - 15);
					comboBox.add(TYPE_COMBO_ONTOLOGY);
					comboBox.add(TYPE_COMBO_FREETEXT);
					comboBox.add(TYPE_COMBO_ENUM);
					comboBox.add(TYPE_COMBO_NUMBER);
					comboBox.add(TYPE_COMBO_INTEGER);
					comboBox.add(TYPE_COMBO_BOOLEAN);
					
					
					
					String value = entry.get(COLUMN_DEF_TYPE_KEY);
					comboBox.setSimpleValue(value);
					
//					for(Ontology ontology : ontologies) {
//						comboBox.add(ontology.getDisplayName());
//					}
					comboBox.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
						
						@Override
						public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
							SimpleComboValue<String> selected = se.getSelectedItem();
							if(selected != null && selected.getValue() != null) {
								presenter.setColumnOntology(entry.getColumnName(), selected.getValue());
							}
						}
					});
					return comboBox;
				}
			}

		};

		return buttonRenderer;
	}

	private GridCellRenderer<ColumnDefinitionTableModel> createConstraintRenderer(final Collection<Enumeration> ontologies) {
		GridCellRenderer<ColumnDefinitionTableModel> buttonRenderer = new GridCellRenderer<ColumnDefinitionTableModel>() {
			@Override
			public Object render(final ColumnDefinitionTableModel model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<ColumnDefinitionTableModel> store,
					Grid<ColumnDefinitionTableModel> grid) {
				final ColumnDefinitionTableModel entry = store.getAt(rowIndex);
				if (entry.isIdColumn()) {
					return getIdentityColumnLabel();					
				} else {					
					TextField<String> field = new TextField<String>();
					field.setWidth(160);
					String value = entry.get(COLUMN_DEF_CONSTRAINT_KEY);
					if(value != null) {
						field.setValue(value);
					}
					LayoutContainer container = new LayoutContainer(new HBoxLayout());
					container.add(field);

					// show different views depending upon the type
					String columnType = entry.get(COLUMN_DEF_TYPE_KEY);
//					if(TYPE_COMBO_ONTOLOGY.equals(columnType)) {
//						Button searchButton = new Button("Find Ontology", AbstractImagePrototype.create(iconsImageBundle.magnify16()));
//						searchButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
//
//							@Override
//							public void componentSelected(ButtonEvent ce) {
//								showOntologySearchWindow();
//							}
//						});
//						
//						container.add(searchButton, new MarginData(0, 0, 0, 5));
//						
//						Button editButton = new Button("Edit Mapping", AbstractImagePrototype.create(iconsImageBundle.documentEdit16()));
//						editButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
//
//							@Override
//							public void componentSelected(ButtonEvent ce) {
//								showOntologySearchWindow();
//							}
//						});
//						
//						container.add(editButton, new MarginData(0, 0, 0, 5));						
//
//					} else {
						// TODO : add for each type
						Button editButton = new Button("", AbstractImagePrototype.create(iconsImageBundle.documentEdit16()));
						editButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

							@Override
							public void componentSelected(ButtonEvent ce) {
								showOntologySearchWindow();
							}
						});
						
						container.add(editButton, new MarginData(0, 0, 0, 5));
//					}
										
					return container;
				}
			}
		};

		return buttonRenderer;
	}

	private GridCellRenderer<ColumnDefinitionTableModel> createUnitsRenderer(final Collection<Enumeration> ontologies) {
		GridCellRenderer<ColumnDefinitionTableModel> buttonRenderer = new GridCellRenderer<ColumnDefinitionTableModel>() {
			@Override
			public Object render(final ColumnDefinitionTableModel model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<ColumnDefinitionTableModel> store,
					Grid<ColumnDefinitionTableModel> grid) {
				final ColumnDefinitionTableModel entry = store.getAt(rowIndex);
				if (entry.isIdColumn()) {
					return getIdentityColumnLabel();					
				} else {
					TextField<String> field = new TextField<String>();
					field.setWidth(90);
					String value = entry.get(COLUMN_DEF_UNITS_KEY);
					if(value != null) {
						field.setValue(value);
					}
					return field;
				}
			}
		};

		return buttonRenderer;
	}

	private GridCellRenderer<ColumnDefinitionTableModel> createDescriptionRenderer(final Collection<Enumeration> ontologies) {
		GridCellRenderer<ColumnDefinitionTableModel> buttonRenderer = new GridCellRenderer<ColumnDefinitionTableModel>() {
			@Override
			public Object render(final ColumnDefinitionTableModel model,
					String property, ColumnData config, final int rowIndex,
					final int colIndex,
					ListStore<ColumnDefinitionTableModel> store,
					Grid<ColumnDefinitionTableModel> grid) {
				final ColumnDefinitionTableModel entry = store.getAt(rowIndex);
				if (entry.isIdColumn()) {
					return getIdentityColumnLabel();					
				} else {
					TextField<String> field = new TextField<String>();
					field.setWidth(160);
					Button editButton = new Button("", AbstractImagePrototype.create(iconsImageBundle.documentEdit16())); 				
					LayoutContainer container = new LayoutContainer(new HBoxLayout());
					container.add(field);
					container.add(editButton);
					return container;
				}
			}
		};

		return buttonRenderer;
	}


	private GridCellRenderer<ColumnDefinitionTableModel> createRemoveRenderer() {
		GridCellRenderer<ColumnDefinitionTableModel> removeButton = new GridCellRenderer<ColumnDefinitionTableModel>() {  			   
			@Override  
			public Object render(final ColumnDefinitionTableModel model, String property, ColumnData config, final int rowIndex,  
			      final int colIndex, ListStore<ColumnDefinitionTableModel> store, Grid<ColumnDefinitionTableModel> grid) {				 
				  final ColumnDefinitionTableModel entry = store.getAt(rowIndex);
			    if(entry.isIdColumn()) {
					return new Label("");					
			    } else {				    
					Anchor removeAnchor = new Anchor();
					removeAnchor.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.deleteButton16()));
					removeAnchor.addClickHandler(new ClickHandler() {			
						@Override
						public void onClick(ClickEvent event) {
							presenter.removeColumn(entry.getColumnName());
						}
					});
					return removeAnchor;
				    
			    }
			  }
			};  
		return removeButton;
	}

	private void showOntologySearchWindow() {
		final Window window = new Window();
		window.setModal(true);
		window.setSize(557, 357);
		window.add(ontologySearchPanel.asWidget());
		Button closeButton = new Button("Close");
		closeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				window.hide();
			}
		});
		window.addButton(closeButton);
		window.show();
	}

	private Object getIdentityColumnLabel() {
		Label lbl = new Label("[Identity Column]");
		lbl.setStyleAttribute("color", "#cccccc");					
		return lbl;					
	}

	
	/*
	 * Private Classes
	 */

	private class ColumnDefinitionTableModel extends BaseModelData {
		String columnName;
		String ontologyKey;
		boolean isIdColumn;	
		
		public ColumnDefinitionTableModel(String columnName, String ontologyKey, boolean isIdColumn) {			
			super();
			
			this.setColumnName(columnName);
			this.setOntologyKey(ontologyKey);
			this.setIdColumn(isIdColumn);			
			
			this.set(COLUMN_REMOVE_COLUMN_KEY, columnName);
		}

		public String getColumnName() {
			return this.get(COLUMN_DEF_COLUMN_NAME_KEY);
		}

		public void setColumnName(String columnName) {
			this.set(COLUMN_DEF_COLUMN_NAME_KEY, columnName);
		}

		public String getOntologyKey() {
			return this.get(COLUMN_DEF_TYPE_KEY);
		}

		public void setOntologyKey(String ontologyKey) {
			this.set(COLUMN_DEF_TYPE_KEY, ontologyKey);
		}

		// Auto generated
		public boolean isIdColumn() {			
			return isIdColumn;
		}

		public void setIdColumn(boolean isIdColumn) {
			this.isIdColumn = isIdColumn;
		}
		
	}
	
}
