package org.sagebionetworks.web.client.widget.editpanels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.CPInstruction;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.ontology.OntologyTerm;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.CellEditor;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AnnotationEditorViewImpl extends LayoutContainer implements AnnotationEditorView {
     
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	
	EditorGrid<EditableAnnotationModelData> grid;
	private ListStore<EditableAnnotationModelData> store;
	private EditableColumnModel cm; 	
	private CellEditor textEditor;
	private CellEditor textAreaEditor;	
	private CellEditor dateEditor;
	private CellEditor booleanEditor;
	private ColumnConfig valueCol;
	private FormPanel addAnnotationPanel;
	private Button deleteButton;
	private Map<String, CellEditor> keyToComboEditor;
	private Map<String, FormField> keyToFormFieldMap;
	
	private Collection<Ontology> ontologies;
	private TextField<String> addAnnotationName;
	private SimpleComboBox<String> addAnnotationTypeCombo;
	private SimpleComboBox<Ontology> addAnnotationOntologyCombo;
	private Button createAnnotationButton;
	private Window addAnnotationsWindow;	
	
	private static final String MENU_ADD_ANNOTATION_TEXT = "Text";
	private static final String MENU_ADD_ANNOTATION_NUMBER = "Number";
	private static final String MENU_ADD_ANNOTATION_DATE = "Date";
	private static final String MENU_ADD_ANNOTATION_ONTOLOGY = "Ontology";
	
    @Inject
    public AnnotationEditorViewImpl(SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.setScrollMode(Scroll.AUTOY);
		
		valueCol = new ColumnConfig();
		deleteButton = new Button();		
    }
 
    @Override
    protected void onRender(Element parent, int pos) {
        super.onRender(parent, pos);
    }

	@Override
	public void generateAnnotationForm(final List<FormField> formFields, String displayString, String topText, boolean editable) {
		// remove any old forms, this is a singleton afterall
		this.clear();
		setLayout(new FlowLayout(0));
		
		// Setup CellEditors
		setupCellEditors(formFields);		
		
		// setup columns & column model
		createColumnModel();
				
		// configure list store for grid & add data
		store = new ListStore<EditableAnnotationModelData>();
		keyToFormFieldMap = new HashMap<String, FormField>();
		EditorUtils.addAnnotationsToStore(formFields, store, keyToFormFieldMap);

		// create Grid
		grid = new EditorGrid<EditableAnnotationModelData>(store, cm);
		grid.setAutoExpandColumn(EditableAnnotationModelData.KEY_COLUMN_ID);
		grid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		grid.setBorders(true);
		grid.setStripeRows(true);
		ToolBar toolBar = createToolbarAndButtons(grid);		
		addGridListeners(formFields, keyToFormFieldMap, grid);

		// create toolbar (needs to happen before grid listeners)

		// Setup containing panel
		ContentPanel cp = new ContentPanel();
		cp.setHeaderVisible(false);
		cp.setFrame(false);
		cp.setBodyBorder(true);
		cp.setSize(400, 200);
		cp.setLayout(new FitLayout());
		cp.add(grid);		
		
		if(editable) {			
			cp.setBottomComponent(toolBar);
			cm.setCellsEditable(true);
		} else {
			cm.setCellsEditable(false);
		}
		cp.layout();		
		
		add(cp);	
		this.layout();
		grid.reconfigure(store, cm);
	}

	@Override
	public void showErrorMessage(String message) {
		this.clear();	
		Html html = new Html(DisplayUtils.getIconHtml(iconsImageBundle.error16()) + " " + message);
		this.add(html);		
	}

    @Override
    public Widget asWidget() {
        return this;
    }

    
	@Override
	public void showLoading() {
		Html html = new Html(DisplayUtils.getIconHtml(sageImageBundle.loading16()) + " Loading...");
		this.add(html);
	}
    
	@Override
	public void clear() {
		this.removeAll();
	}
 
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

	@Override
	public void showPersistSuccess() {		
		store.commitChanges();
		Info.display("Saved", "Annotation change saved.");
	}

	@Override
	public void showPersistFail() {		
		store.rejectChanges();
		MessageBox.info("Error", "An error occuring attempting to save. Please try again.", null);
		resetCreateAnnotationButton();
	}

	@Override
	public void setOntologies(Collection<Ontology> ontologies) {
		this.ontologies = ontologies;
	}


	@Override
	public void showAddAnnotationFail(String string) {
		resetCreateAnnotationButton();
		showErrorMessage(string);
	}

	@Override
	public void showAddAnnotationSuccess() {
		addAnnotationsWindow.hide();
		resetCreateAnnotationButton();
		cleanupAddAnnotationPanel();
		showInfo("Added", "Annotation added");		
	}

	@Override
	public void showInfo(String title, String message) {
		Info.display(title, message);
	}

	@Override
	public void updateAnnotations(List<FormField> formFields) {
		// refill the store
		store.removeAll(); 
		keyToFormFieldMap.clear();
		EditorUtils.addAnnotationsToStore(formFields, store, keyToFormFieldMap);
		setupCellEditors(formFields);
		grid.reconfigure(store, cm);		
	}

	@Override
	public void showDeleteAnnotationSuccess() {
		showInfo("Deleted", "Annotation deleted");		
	}

	@Override
	public void showDeleteAnnotationFail() {		
		showErrorMessage("A problem occured deleting the annotation.");
	}
	

	/*
	 * Private Methods
	 */
	private void setupCellEditors(List<FormField> formFields) {

		// Text Editor
		TextField<String> text = new TextField<String>();
		text.setAllowBlank(false);
		textEditor = new CellEditor(text);

		// Text Field Editor
		TextArea textArea = new TextArea();
		textArea.setAllowBlank(false);
		textAreaEditor = new CellEditor(textArea);
		
		// Date Editor
		DateField dateField = new DateField();
		dateField.getPropertyEditor().setFormat(DateTimeFormat.getFormat("MM/dd/yyyy"));		
		dateEditor = new CellEditor(dateField);

		keyToComboEditor = new HashMap<String, CellEditor>();
		// Create a SimpleComboBox CellEditor for each FormField that has an ontology (not all ontologies)
		for(FormField formField : formFields) {
			createComboEditorForFormField(formField);
		}		

		// Boolean (checkbox) editor
		booleanEditor = new CellEditor(new CheckBox());				
	}

	private void createComboEditorForFormField(FormField formField) {
		String key = formField.getKey();
		if(key != null && formField.isOntologyBased()) {
			// Combo Editor
			final SimpleComboBox<String> ontologyCombo = new SimpleComboBox<String>();
			ontologyCombo.setFieldLabel(key);					
			ontologyCombo.setForceSelection(true);
			ontologyCombo.setTriggerAction(TriggerAction.ALL);
			for(OntologyTerm ontologyTerm : formField.getOntologyTerms()) {
				ontologyCombo.add(ontologyTerm.getValue());
			}

			CellEditor comboEditor = new CellEditor(ontologyCombo) {
				@Override
				public Object preProcessValue(Object value) {
					if (value == null) {
						return value;
					}
					return ontologyCombo.findModel(value.toString());
				}

				@SuppressWarnings("unchecked")
				@Override
				public Object postProcessValue(Object value) {
					if (value == null) {
						return value;
					}						
					return ((SimpleComboValue<String>) value).getValue();
				}
			};

			keyToComboEditor.put(key, comboEditor);
		}
	}

	private void createColumnModel() {
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
		ColumnConfig column = new ColumnConfig();
		column.setId(EditableAnnotationModelData.KEY_COLUMN_ID);
		column.setHeader("Name");
		column.setWidth(220);
		configs.add(column);		
		valueCol.setId(EditableAnnotationModelData.VALUE_COLUMN_ID);
		valueCol.setHeader("Value");
		valueCol.setWidth(220);
		valueCol.setEditor(textEditor); // start with text editor, switch depending upon row
		// Create renderer that adapts to display valueCol differently 
		createGridCellRenderer();
		configs.add(valueCol);
		cm = new EditableColumnModel(configs);
	}

	private void addGridListeners(final List<FormField> formFields,
			final Map<String, FormField> keyToFormFieldMap,
			final EditorGrid<EditableAnnotationModelData> grid) {

		// select proper editor for the row
		grid.addListener(Events.BeforeEdit, new Listener<GridEvent<EditableAnnotationModelData>>() {
			@Override
			public void handleEvent(GridEvent<EditableAnnotationModelData> event) {				
				EditableAnnotationModelData model = event.getModel();
				ColumnEditType type = model.getColumnEditType();
				if(type == ColumnEditType.COMBO) {
					// lookup ontology editor to use via model key
					String key = model.getKey();
					if(keyToComboEditor.containsKey(model.getKey())) {
						valueCol.setEditor(keyToComboEditor.get(key));
					} else {
						// fallback if no ontology editor
						valueCol.setEditor(textEditor);
					}					
				} else if(type == ColumnEditType.TEXTAREA) {
					valueCol.setEditor(textAreaEditor);
				} else if(type == ColumnEditType.DATE) {
					valueCol.setEditor(dateEditor);
				} else {
					// default to the regular text editor
					valueCol.setEditor(textEditor);
				}
			}
		});
		
		// persist change
		grid.addListener(Events.AfterEdit, new Listener<GridEvent<EditableAnnotationModelData>>() {
			@Override
			public void handleEvent(GridEvent<EditableAnnotationModelData> event) {
				EditableAnnotationModelData model = event.getModel();
				ColumnEditType type = model.getColumnEditType();
				String val = "";
				// get string of value
				if(type == ColumnEditType.DATE) {
					val = DisplayConstants.DATE_FORMAT.format((Date) model.getValue());
				} else {					
					val = (String) model.getValue();
				}				
				
				presenter.editAnnotation(model.getKey(), val); // persist
				grid.getSelectionModel().deselectAll(); // deselect the edited row
				deleteButton.disable(); // disable after deselect
			}
		});
		
		// setup a selection listener to enable the delete button
		grid.addListener(Events.CellClick, new Listener<GridEvent<EditableAnnotationModelData>>() {
			@Override
			public void handleEvent(GridEvent<EditableAnnotationModelData> event) {				
				deleteButton.enable();
			}
		});
	}

	private void createGridCellRenderer() {
		final GridCellRenderer<EditableAnnotationModelData> wrap = new GridCellRenderer<EditableAnnotationModelData>() {
			public String render(EditableAnnotationModelData model, String property,
					ColumnData config, int rowIndex, int colIndex,
					ListStore<EditableAnnotationModelData> store, Grid<EditableAnnotationModelData> grid) {
				String val = "";
				ColumnEditType type = model.getColumnEditType();
				
				// get string of value
				if(type == ColumnEditType.DATE) {
					val = DisplayConstants.DATE_FORMAT.format((Date) model.get(property));
				} else {					
					val = (String) model.get(property);
				}
				
				// render text areas as tall as they need to be (can also add "height:60px;" below for fixed height
				if(type == ColumnEditType.TEXTAREA) {
					return "<div style=\"white-space: normal;\">" + val +"</div>";					
				} else {
					return val;
				}
			}
		};
		valueCol.setRenderer(wrap);
	}
	
	private ToolBar createToolbarAndButtons(
			final EditorGrid<EditableAnnotationModelData> grid) {
		// setup toolbar
		ToolBar toolBar = new ToolBar();
		Button addButton = new Button("Add Annotation");
		addButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
		addButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				showAddAnnotationWindow(grid);
			}
		});
		toolBar.add(addButton);
		
		deleteButton = new Button("Delete Selected Annotation");
		deleteButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
		deleteButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				// TODO : implement delete
				GridSelectionModel<EditableAnnotationModelData> selectionModel = grid.getSelectionModel();
				EditableAnnotationModelData model = selectionModel.getSelectedItem();				
				presenter.deleteAnnotation(model.getKey());				
				selectionModel.deselectAll();
				deleteButton.disable();
			}
		});
		deleteButton.disable();
		toolBar.add(deleteButton);
		return toolBar;
	}

	private void showAddAnnotationWindow(EditorGrid<EditableAnnotationModelData> grid) {
		addAnnotationsWindow = new Window();
		addAnnotationsWindow.setTitle("Add Annotation");
		addAnnotationsWindow.setModal(true);
		addAnnotationsWindow.setSize(400, 170);
		if(addAnnotationPanel == null) {		
			setupAddAnnotationPanel(addAnnotationsWindow);
		}
		addAnnotationsWindow.add(addAnnotationPanel);
		addAnnotationsWindow.show();
		
//		EditableAnnotationModelData plant = new EditableAnnotationModelData();
//		plant.setKey("annot name");
//		plant.setValue("");
//		plant.setColumnEditType(ColumnEditType.TEXT);
//		
//		grid.stopEditing();
//		store.insert(plant, 0);
//		grid.startEditing(store.indexOf(plant), 0);
	
	}

	private void setupAddAnnotationPanel(final Window window) {
		// Setup panel
		addAnnotationPanel = new FormPanel();
		addAnnotationPanel.setHeaderVisible(false);
		addAnnotationPanel.setFrame(false);
		addAnnotationPanel.setBodyBorder(true);
		addAnnotationPanel.setAutoHeight(true);
		addAnnotationPanel.setAutoWidth(true);
		FormLayout layout = new FormLayout();  
		layout.setLabelWidth(110);  
		addAnnotationPanel.setLayout(layout); 

		final FormData formData = new FormData("-20");					
		
		addAnnotationName = new TextField<String>();
		addAnnotationName.setFieldLabel("Annotation Name");
		addAnnotationName.setAllowBlank(false);		
		addAnnotationPanel.add(addAnnotationName, formData);

		// Build up ontologies
		addAnnotationOntologyCombo = new SimpleComboBox<Ontology>();
		addAnnotationOntologyCombo.setFieldLabel("Ontology");					
		addAnnotationOntologyCombo.setForceSelection(true);
		addAnnotationOntologyCombo.setTriggerAction(TriggerAction.ALL);
		if(ontologies != null) {
			for(Ontology ontology : ontologies) {
				addAnnotationOntologyCombo.add(ontology);				
			}
		}
		addAnnotationOntologyCombo.hide();
		
		addAnnotationTypeCombo = new SimpleComboBox<String>();
		addAnnotationTypeCombo.setFieldLabel("Type");					
		addAnnotationTypeCombo.setForceSelection(true);
		addAnnotationTypeCombo.setTriggerAction(TriggerAction.ALL);
		addAnnotationTypeCombo.add(MENU_ADD_ANNOTATION_TEXT);
		addAnnotationTypeCombo.add(MENU_ADD_ANNOTATION_NUMBER);
		addAnnotationTypeCombo.add(MENU_ADD_ANNOTATION_DATE);
		addAnnotationTypeCombo.add(MENU_ADD_ANNOTATION_ONTOLOGY);
		addAnnotationTypeCombo.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				SimpleComboValue<String> selectedItem = se.getSelectedItem();				
				if(selectedItem.getValue().equals(MENU_ADD_ANNOTATION_ONTOLOGY)) {
					addAnnotationOntologyCombo.show();					
				} else {
					addAnnotationOntologyCombo.clearSelections();
					addAnnotationOntologyCombo.hide();
				}
			}
		});
		addAnnotationPanel.add(addAnnotationTypeCombo, formData);						
		addAnnotationPanel.add(addAnnotationOntologyCombo, formData);
		
		// buttons
		createAnnotationButton = new Button(DisplayConstants.BUTTON_ADD_ANNOTATION, AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
		createAnnotationButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				handleAddAnnotation();				
			}

		});
		
		Button cancelButton = new Button("Cancel");
		cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				cleanupAddAnnotationPanel();
				window.hide();
			}
		});
		
		addAnnotationPanel.addButton(createAnnotationButton);
		addAnnotationPanel.addButton(cancelButton);
		addAnnotationPanel.setButtonAlign(HorizontalAlignment.CENTER);
	}

	private void resetCreateAnnotationButton() {
		createAnnotationButton.setText(DisplayConstants.BUTTON_ADD_ANNOTATION);
		createAnnotationButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
	}
	
	private void cleanupAddAnnotationPanel() {
		resetCreateAnnotationButton();
		addAnnotationName.clear();
		addAnnotationTypeCombo.clearSelections();
		addAnnotationOntologyCombo.clearSelections();
		addAnnotationOntologyCombo.hide();
	}

	private ColumnEditType getColumnEditTypeFromDisplayType(String type) {
		if(type == null) return null;
		if(MENU_ADD_ANNOTATION_DATE.equals(type)) {
			return ColumnEditType.DATE;
		} else if (MENU_ADD_ANNOTATION_NUMBER.equals(type)) {
			return ColumnEditType.TEXT;			
		} else if (MENU_ADD_ANNOTATION_ONTOLOGY.equals(type)) {
			return ColumnEditType.COMBO;			
		} else {
			return ColumnEditType.TEXT;
		}	
	}

	private void handleAddAnnotation() {
		Ontology ontologySelected = null;
		ColumnEditType typeSelected = null;
		String nameSelected = addAnnotationName.getValue();
		
		// error check
		if(nameSelected == null || nameSelected.length() == 0) {
			// TODO : check for reasonable characters in name!
			MessageBox.alert("Name Required", "Please enter a Name", null);
			return;
		}

		List<SimpleComboValue<String>> selections = addAnnotationTypeCombo.getSelection();				
		if(selections.size() == 0) {
			MessageBox.alert("Type Required", "Please select a Type", null);
			return;
		}
		
		String selected = selections.get(0).getValue();
		if(selected.equals(MENU_ADD_ANNOTATION_ONTOLOGY)) {
			List<SimpleComboValue<Ontology>> selectedOntologies = addAnnotationOntologyCombo.getSelection();
			if(selectedOntologies.size() >= 1) {
				ontologySelected = selectedOntologies.get(0).getValue();
			} else {
				MessageBox.alert("Ontology Required", "Please select an Ontology", null);
				return;
			}
		} else if(selected.equals(MENU_ADD_ANNOTATION_DATE)) {					
			if(!nameSelected.matches(".*Date$")) {
				MessageBox.alert("Error", "Currently Date annotations' Name must end with 'Date' (i.e. 'ReleaseDate'). This requirement will be removed in the future.", null);				
				return;
			}
		}
		typeSelected = getColumnEditTypeFromDisplayType(selected);
		
		// TODO : Remove this when we have an Ontology Service
		if(typeSelected == ColumnEditType.COMBO && ontologySelected != null
				&& !ontologySelected.getDisplayName().equals(nameSelected)) {
			MessageBox.alert("Error", "Currently the annotation Name field must match the name of the Ontology. This requirement will be removed in the future.", null);
			return;
		}
		
		// add Annotation
		DisplayUtils.changeButtonToSaving(createAnnotationButton, sageImageBundle);					
		if(typeSelected == ColumnEditType.COMBO && ontologySelected != null) {
			presenter.addAnnotation(nameSelected, typeSelected, ontologySelected);
		} else {
			presenter.addAnnotation(nameSelected, typeSelected); 
		}
	}

}