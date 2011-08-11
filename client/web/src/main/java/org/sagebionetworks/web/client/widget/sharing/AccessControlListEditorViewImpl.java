package org.sagebionetworks.web.client.widget.sharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.shared.users.AclEntry;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AccessControlListEditorViewImpl extends LayoutContainer implements AccessControlListEditorView {
 
	private static final String PRINCIPAL_COLUMN_ID = "principalData";
	private static final String ACCESS_COLUMN_ID = "accessData";
	private static final String REMOVE_COLUMN_ID = "removeData";
	
	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;		
	private Grid<PermissionsTableEntry> permissionsGrid;
	private Map<PermissionLevel, String> permissionDisplay;
	
	
	@Inject
	public AccessControlListEditorViewImpl(IconsImageBundle iconsImageBundle) {
		this.iconsImageBundle = iconsImageBundle;		
		
		permissionDisplay = new HashMap<PermissionLevel, String>();
		permissionDisplay.put(PermissionLevel.CAN_VIEW, DisplayConstants.MENU_PERMISSION_LEVEL_CAN_VIEW);
		permissionDisplay.put(PermissionLevel.CAN_EDIT, DisplayConstants.MENU_PERMISSION_LEVEL_CAN_EDIT);
		permissionDisplay.put(PermissionLevel.CAN_ADMINISTER, DisplayConstants.MENU_PERMISSION_LEVEL_CAN_ADMINISTER);		
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);		
	}
		
	@Override
	public Widget asWidget() {
		return this;
	}
	
	@Override
	public void refresh(List<AclEntry> entries, List<AclPrincipal> principals, boolean isEditable) {
		ListStore<PermissionsTableEntry> permissionsStore = loadPermissionsStore(entries);
		permissionsGrid.reconfigure(permissionsStore, permissionsGrid.getColumnModel());
	}

	private ListStore<PermissionsTableEntry> loadPermissionsStore(
			List<AclEntry> entries) {
		final ListStore<PermissionsTableEntry> permissionsStore = new ListStore<PermissionsTableEntry>();
		AclEntry ownerEntry = null;
		for(AclEntry aclEntry : entries) {
			if(aclEntry.isOwner()) {
				ownerEntry = aclEntry;
				continue;
			}
			permissionsStore.add(new PermissionsTableEntry(aclEntry));
		}
		permissionsStore.sort(PRINCIPAL_COLUMN_ID, SortDir.ASC);
		permissionsStore.insert(new PermissionsTableEntry(ownerEntry), 0); // insert owner first
		return permissionsStore;
	}
	
	@Override
	public void setAclDetails(List<AclEntry> entries, List<AclPrincipal> principals, boolean isInherited) {		
		this.removeAll(true);
		
		// setup view
		this.setLayout(new FlowLayout(10));			
		Label permissionsLabel = new Label(DisplayConstants.LABEL_SHARING_PANEL_EXISTING + ":");
		permissionsLabel.setStyleAttribute("font-weight", "bold");
		permissionsLabel.setStyleAttribute("font-size", "105%");
		add(permissionsLabel, new MarginData(15, 0, 0, 0));

		// show existing permissions
		ListStore<PermissionsTableEntry> permissionsStore = loadPermissionsStore(entries);
		createPermissionsGrid(permissionsStore);	
		if(isInherited) { 
			permissionsGrid.disable();
			Label readOnly = new Label(DisplayConstants.PERMISSIONS_INHERITED_TEXT);			
			add(readOnly);			
			
			Button createAcl = new Button(DisplayConstants.BUTTON_PERMISSIONS_CREATE_NEW_ACL, AbstractImagePrototype.create(iconsImageBundle.addSquare16()));
			createAcl.addSelectionListener(new SelectionListener<ButtonEvent>() {
				@Override
				public void componentSelected(ButtonEvent ce) {
					presenter.createAcl();					
				}
			});
			add(createAcl, new MarginData(30, 0, 0, 0));
			add(new Label(DisplayUtils.getIconHtml(iconsImageBundle.warning16()) + " " + DisplayConstants.PERMISSIONS_CREATE_NEW_ACL_TEXT), new MarginData(5, 0, 0, 0));
		} else {
			// show add people view
			
			ListStore<PeopleModel> allUsersStore = new ListStore<PeopleModel>();  
			for(AclPrincipal principal : principals) {
				allUsersStore.add(new PeopleModel(principal));
			}
  
			FormPanel form2 = new FormPanel();  
			form2.setFrame(false);  
			form2.setHeaderVisible(false);  
			form2.setAutoWidth(true);
			
			form2.setLayout(new FlowLayout());  
			  
			FieldSet fieldSet = new FieldSet();  
			fieldSet.setHeading(DisplayConstants.LABEL_PERMISSION_TEXT_ADD_PEOPLE );  
			fieldSet.setCheckboxToggle(false);
			fieldSet.setCollapsible(false);			
			
			FormLayout layout = new FormLayout();  
			layout.setLabelWidth(75);			
			fieldSet.setLayout(layout);  
			
			final ComboBox<PeopleModel> combo = new ComboBox<PeopleModel>();  
			combo.setEmptyText("Enter email addresses or group names...");  
			combo.setDisplayField("name");  
			combo.setWidth(450);
			combo.setHeight(21);
			combo.setStore(allUsersStore);		
			combo.setTypeAhead(true);
			combo.setFieldLabel("User/Group");
			combo.setForceSelection(true);
			combo.setTriggerAction(TriggerAction.ALL);
			//combo.setTriggerAction(TriggerAction.ALL);  
			fieldSet.add(combo, new RowData(450,21));
			
			final SimpleComboBox<PermissionLevelSelect> selectPermissionLevel = new SimpleComboBox<PermissionLevelSelect>();
			//ListStore<PermissionLevelSelect> permStore = new Lis
			selectPermissionLevel.add(new PermissionLevelSelect(permissionDisplay.get(PermissionLevel.CAN_VIEW), PermissionLevel.CAN_VIEW));
			selectPermissionLevel.add(new PermissionLevelSelect(permissionDisplay.get(PermissionLevel.CAN_EDIT), PermissionLevel.CAN_EDIT));
			selectPermissionLevel.add(new PermissionLevelSelect(permissionDisplay.get(PermissionLevel.CAN_ADMINISTER), PermissionLevel.CAN_ADMINISTER));			
			selectPermissionLevel.setEmptyText("Select access level...");
			selectPermissionLevel.setFieldLabel("Access Level");
			selectPermissionLevel.setTypeAhead(false);
			selectPermissionLevel.setEditable(false);
			selectPermissionLevel.setForceSelection(true);
			selectPermissionLevel.setTriggerAction(TriggerAction.ALL);
			fieldSet.add(selectPermissionLevel);
			
			Button shareButton = new Button("Share");
			shareButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
				@Override
				public void componentSelected(ButtonEvent ce) {
					List<PeopleModel> selectedPrincipals = combo.getSelection();
					if(selectedPrincipals != null && selectedPrincipals.size() > 0) {
						AclPrincipal principal = selectedPrincipals.get(0).getAclPrincipal();
						List<SimpleComboValue<PermissionLevelSelect>> selectedPermissions = selectPermissionLevel.getSelection();
						if(selectedPermissions != null && selectedPermissions.size() > 0) {
							PermissionLevel level = selectedPermissions.get(0).getValue().getLevel();
							presenter.addAccess(principal, level);
							
							// clear out selections
							combo.clearSelections();
							selectPermissionLevel.clearSelections();
						} else {
							showAddMessage("Please select a permission level to grant.");
						}
					} else {
						showAddMessage("Please select a user or group to grant permission to.");
					}
				}
			});

			fieldSet.add(shareButton);
			form2.add(fieldSet);
			//form2.addButton(shareButton);
			
			add(form2);
		}
		
	}
	
	private void showAddMessage(String message) {
		// TODO : put this on the form somewher
		showErrorMessage(message);
	}

	private void createPermissionsGrid(ListStore<PermissionsTableEntry> permissionsStore) {			
		GridCellRenderer<PermissionsTableEntry> peopleRenderer = createPeopleRenderer();
		GridCellRenderer<PermissionsTableEntry> buttonRenderer = createButtonRenderer();
		GridCellRenderer<PermissionsTableEntry> removeRenderer = createRemoveRenderer();						   
				   
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();  
				   
		ColumnConfig column = new ColumnConfig();  
		column.setId(PRINCIPAL_COLUMN_ID);  
		column.setHeader("People");  
		column.setWidth(200);  
		column.setRenderer(peopleRenderer);
		configs.add(column);  
				   
		column = new ColumnConfig();  
		column.setId(ACCESS_COLUMN_ID);  
		column.setHeader("Access");  
		column.setWidth(110);  
		column.setRenderer(buttonRenderer);  
		configs.add(column);  
				   
		column = new ColumnConfig();  
		column.setId(REMOVE_COLUMN_ID);  
		column.setHeader("");  
		column.setAlignment(HorizontalAlignment.RIGHT);  
		column.setWidth(25);  
		column.setRenderer(removeRenderer);  
		configs.add(column);  
				   				   			   
		ColumnModel columnModel = new ColumnModel(configs);  				  				 
		permissionsGrid = new Grid<PermissionsTableEntry>(permissionsStore, columnModel);    
		permissionsGrid.setAutoExpandColumn(PRINCIPAL_COLUMN_ID);  
		permissionsGrid.setBorders(true);		
		permissionsGrid.setWidth(520);
		permissionsGrid.setHeight(150);
		
		add(permissionsGrid, new MarginData(5, 0, 0, 0));
		
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}

	/*
	 * Private Methods
	 */
	private Menu createEditAccessMenu(final AclEntry aclEntry) {
		Menu menu = new Menu();		
		MenuItem item = null; 
			
		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_VIEW));			
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {
				presenter.changeAccess(aclEntry, PermissionLevel.CAN_VIEW);
			}
		});
		menu.add(item);

		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_EDIT));			
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {
				presenter.changeAccess(aclEntry, PermissionLevel.CAN_EDIT);
			}
		});
		menu.add(item);

		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_ADMINISTER));			
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {
				presenter.changeAccess(aclEntry, PermissionLevel.CAN_ADMINISTER);
			}
		});
		menu.add(item);

		
		return menu;
	}  

	private Menu createNewAccessMenu() {
		Menu menu = new Menu();		
		MenuItem item = null; 
			
		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_VIEW));			
		menu.add(item);

		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_EDIT));			
		menu.add(item);

		item = new MenuItem(permissionDisplay.get(PermissionLevel.CAN_ADMINISTER));			
		menu.add(item);

		return menu;
	}  

	private GridCellRenderer<PermissionsTableEntry> createPeopleRenderer() {
		GridCellRenderer<PermissionsTableEntry> personRenderer = new GridCellRenderer<PermissionsTableEntry>() {
			@Override
			public Object render(PermissionsTableEntry model, String property,
					ColumnData config, int rowIndex, int colIndex,
					ListStore<PermissionsTableEntry> store,
					Grid<PermissionsTableEntry> grid) {
				PermissionsTableEntry entry = store.getAt(rowIndex);
				String iconHtml = "";
				if(entry.getAclEntry().getPrincipal().isIndividual()) {
					iconHtml = DisplayUtils.getIconHtml(iconsImageBundle.userBusiness16());
				} else {
					iconHtml = DisplayUtils.getIconHtml(iconsImageBundle.users16());	
				}
				return iconHtml + "&nbsp;&nbsp;" + model.get(property);
			}
			
		};
		return personRenderer;
	}

	private GridCellRenderer<PermissionsTableEntry> createButtonRenderer() {
		GridCellRenderer<PermissionsTableEntry> buttonRenderer = new GridCellRenderer<PermissionsTableEntry>() {  
			   
			  private boolean init;  
			  @Override	   
			  public Object render(final PermissionsTableEntry model, String property, ColumnData config, final int rowIndex,  
			      final int colIndex, ListStore<PermissionsTableEntry> store, Grid<PermissionsTableEntry> grid) {
				  PermissionsTableEntry entry = store.getAt(rowIndex);
			    if (!init) {  
			      init = true;  
			      grid.addListener(Events.ColumnResize, new Listener<GridEvent<PermissionsTableEntry>>() {  
					   
			        public void handleEvent(GridEvent<PermissionsTableEntry> be) {  
			          for (int i = 0; i < be.getGrid().getStore().getCount(); i++) {  
			            if (be.getGrid().getView().getWidget(i, be.getColIndex()) != null  
			                && be.getGrid().getView().getWidget(i, be.getColIndex()) instanceof BoxComponent) {  
			              ((BoxComponent) be.getGrid().getView().getWidget(i, be.getColIndex())).setWidth(be.getWidth() - 10);  
			            }  
			          }  
			        }  
			      });  
			    }  
			    if(entry.getAclEntry().isOwner()) {
				    Button b = new Button(DisplayConstants.MENU_PERMISSION_LEVEL_IS_OWNER);
				    b.setWidth(grid.getColumnModel().getColumnWidth(colIndex) - 15);
				    b.disable();
					return b;		    	
			    } else {
				    Button b = new Button((String) model.get(property));  
				    b.setWidth(grid.getColumnModel().getColumnWidth(colIndex) - 25);  
				    b.setToolTip("Click to change permissions");				  
				    b.setMenu(createEditAccessMenu(entry.getAclEntry()));
				    return b;
			    }
			  }
			};  
			
			return buttonRenderer;
	}

	private GridCellRenderer<PermissionsTableEntry> createRemoveRenderer() {
		GridCellRenderer<PermissionsTableEntry> removeButton = new GridCellRenderer<PermissionsTableEntry>() {  			   
			@Override  
			public Object render(final PermissionsTableEntry model, String property, ColumnData config, final int rowIndex,  
			      final int colIndex, ListStore<PermissionsTableEntry> store, Grid<PermissionsTableEntry> grid) {				 
				  final PermissionsTableEntry entry = store.getAt(rowIndex);
			    if(entry.getAclEntry().isOwner()) {
					return new Label("");		    	
			    } else {				    
					Anchor removeAnchor = new Anchor();
					removeAnchor.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.deleteButton16()));
					removeAnchor.addClickHandler(new ClickHandler() {			
						@Override
						public void onClick(ClickEvent event) {
							presenter.removeAccess(entry.getAclEntry());
						}
					});
					return removeAnchor;
				    
			    }
			  }
			};  
		return removeButton;
	}
	
	/*
	 * Private Classes
	 */
	private class PermissionsTableEntry extends BaseModelData {
		private AclEntry aclEntry;
		public PermissionsTableEntry(AclEntry aclEntry) {			
			super();			
			this.aclEntry = aclEntry;
			AclPrincipal principal = aclEntry.getPrincipal();
			this.set(PRINCIPAL_COLUMN_ID, principal.getName());			
			PermissionLevel level = AclUtils.getPermissionLevel(aclEntry.getAccessTypes());			
			if(level != null) {
				this.set(ACCESS_COLUMN_ID, permissionDisplay.get(level)); 
			}			
			this.set(REMOVE_COLUMN_ID, aclEntry.getPrincipalId());			
		}
		public AclEntry getAclEntry() {
			return aclEntry;
		}		
	}

	private class PeopleModel extends BaseModelData {
		private AclPrincipal aclPrincipal;
		public PeopleModel(AclPrincipal aclPrincipal) {
			this.aclPrincipal = aclPrincipal;
			String groupStr = aclPrincipal.isIndividual() ? "" : " (Group)"; 			
			this.set("name", aclPrincipal.getName() + groupStr);
		}
		
		public AclPrincipal getAclPrincipal() {
			return aclPrincipal;
		}
	}

	private class PermissionLevelSelect {
		private String display;
		private PermissionLevel level;
		public PermissionLevelSelect(String display, PermissionLevel level) {
			super();
			this.display = display;
			this.level = level;
		}
		public String getDisplay() {
			return display;
		}
		public void setDisplay(String display) {
			this.display = display;
		}
		public PermissionLevel getLevel() {
			return level;
		}
		public void setLevel(PermissionLevel level) {
			this.level = level;
		}		
		
		public String toString() {
			return display;
		}
	}
	
}
