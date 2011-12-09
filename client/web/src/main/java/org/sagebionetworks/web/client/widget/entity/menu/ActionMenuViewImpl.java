package org.sagebionetworks.web.client.widget.entity.menu;

import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.sharing.AccessControlListEditor;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.shared.EntityType;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ActionMenuViewImpl extends LayoutContainer implements ActionMenuView {

	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private NodeEditor nodeEditor;
	private AccessMenuButton accessMenuButton;
	private AccessControlListEditor accessControlListEditor;
	private Button editButton;
	private Button shareButton;
	private Button addButton;
	private Button toolsButton;
	private HorizontalPanel hp;
	
		
	@Inject
	public ActionMenuViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, NodeEditor nodeEditor,
			AccessMenuButton accessMenuButton,
			AccessControlListEditor accessControlListEditor) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;
		this.nodeEditor = nodeEditor;
		this.accessMenuButton = accessMenuButton;	
		this.accessControlListEditor = accessControlListEditor;
		this.setLayout(new FitLayout());
	}

	@Override
	public void createMenu(Entity entity, EntityType entityType, boolean isAdministrator,
			boolean canEdit) {			
		
		// add items in order. spacing is done with Html widgets as we don't want to add top/bottom padding
		boolean addHpanel = hp == null ? true : false;
		if(hp == null) {			
			hp = new HorizontalPanel();
		}
		if(canEdit) {
			boolean add = editButton == null ? true : false;
			configureEditButton(entity, entityType);
			if(add) { 
				hp.add(editButton);
				hp.add(new Html("&nbsp;"));
			}
		}
		if(isAdministrator) { 
			boolean add = shareButton == null ? true : false;
			configureShareButton(entity);
			if(add) { 
				hp.add(shareButton);
				hp.add(new Html("&nbsp;"));
			}
		}
		if(canEdit) {
			boolean add = addButton == null ? true : false;
			configureAddMenu(entity, entityType);
			if(add) {
				hp.add(addButton);
				hp.add(new Html("&nbsp;"));
			}
		}
		boolean add = toolsButton == null ? true : false;		
		configureToolsMenu(entity, entityType, isAdministrator, canEdit);
		if(add) {
			hp.add(toolsButton);	
		}

		if(addHpanel) add(hp);
//		this.layout();
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}	

	@Override 
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
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
		if(editButton != null) editButton.removeAllListeners();
		if(shareButton != null) shareButton.removeAllListeners();	
	}

	/*
	 * Private Methods
	 */
	private void configureEditButton(final Entity entity, EntityType entityType) {
		final String typeDisplay = DisplayUtils.uppercaseFirstLetter(entityType.getName());
		if(editButton == null) {			
			editButton = new Button(DisplayConstants.BUTTON_EDIT, AbstractImagePrototype.create(iconsImageBundle.editGrey16()));
			editButton.setHeight(25);
		}		
		editButton.removeAllListeners();
		editButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				final Window window = new Window();  
				window.setSize(600, 345);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.BUTTON_EDIT + " " + typeDisplay);
				window.setLayout(new FitLayout());								
				nodeEditor.addCancelHandler(new CancelHandler() {					
					@Override
					public void onCancel(CancelEvent event) {
						window.hide();
					}
				});
				nodeEditor.addPersistSuccessHandler(new EntityUpdatedHandler() {					
					@Override
					public void onPersistSuccess(EntityUpdatedEvent event) {
						window.hide();
						presenter.fireEntityUpdatedEvent();
					}
				});
				nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
				window.add(nodeEditor.asWidget(DisplayUtils.getNodeTypeForEntity(entity), entity.getId()), new FitData(4));				
				window.show();
			}
		});		
	}
	
	private void configureShareButton(Entity entity) {		
		accessControlListEditor.setPlaceChanger(presenter.getPlaceChanger());
		accessControlListEditor.setResource(entity);
		if(shareButton == null) { 
			shareButton = new Button(DisplayConstants.BUTTON_SHARE, AbstractImagePrototype.create(iconsImageBundle.mailGrey16()));
			shareButton.setHeight(25);
		}
		shareButton.removeAllListeners();		
		shareButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final Window window = new Window();  
				window.setSize(550, 440);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.TITLE_SHARING_PANEL);
				window.setLayout(new FitLayout());
				window.add(accessControlListEditor.asWidget(), new FitData(4));
				Button closeButton = new Button("Close");
				closeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
				});
				window.setButtonAlign(HorizontalAlignment.RIGHT);
				window.addButton(closeButton);
				window.show();
			}
		});		
	}
	
	private void configureAddMenu(final Entity entity, final EntityType entityType) {
		if(addButton == null) {
				addButton = new Button(DisplayConstants.BUTTON_ADD, AbstractImagePrototype.create(iconsImageBundle.addGrey16()));
				addButton.setHeight(25);
		}
				
		// create add menu button from children
		List<EntityType> children = entityType.getValidChildTypes();
		Menu menu = new Menu();		
		MenuItem item;
		
		// TODO : sort children?
		for(final EntityType childType : children) {
			String displayName = DisplayUtils.uppercaseFirstLetter(childType.getName());			
			item = new MenuItem(displayName);
			// TODO : replace icon with entity type icon
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					showAddWindow(childType, entity.getId());
				}
			});
			menu.add(item);
		}
		
		addButton.setMenu(menu);
	}

	private void configureToolsMenu(Entity entity, EntityType entityType, boolean isAdministrator, boolean canEdit) {		
		if(toolsButton == null) {
			toolsButton = new Button(DisplayConstants.BUTTON_TOOLS_MENU, AbstractImagePrototype.create(iconsImageBundle.adminToolsGrey16()));
			toolsButton.setHeight(25);
		}
		// create drop down menu
		Menu menu = new Menu();		
		// add restricted items to the Tools menu
		if(canEdit) addCanEditToolMenuItems(menu, entity, entityType);
		if(isAdministrator) addIsAdministratorToolMenuItems(menu, entity, entityType);

		toolsButton.setMenu(menu);
	}

	/**
	 * Administrator Menu Options
	 * @param menu
	 * @param entityType 
	 */
	private void addIsAdministratorToolMenuItems(Menu menu, Entity entity, EntityType entityType) {
		final String typeDisplay = DisplayUtils.uppercaseFirstLetter(entityType.getName());
		MenuItem item = new MenuItem(DisplayConstants.LABEL_DELETE + " " + typeDisplay);
		item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {
				MessageBox.confirm(DisplayConstants.LABEL_DELETE +" " + typeDisplay, DisplayConstants.PROMPT_SURE_DELETE + " " + typeDisplay +"?", new Listener<MessageBoxEvent>() {					
					@Override
					public void handleEvent(MessageBoxEvent be) { 					
						Button btn = be.getButtonClicked();
						if(Dialog.YES.equals(btn.getItemId())) {
							presenter.deleteEntity();
						}
					}
				});
			}
		});
		menu.add(item);
	}

	/**
	 * Edit menu options
	 * @param menu
	 * @param entity 
	 * @param entityType 
	 */
	private void addCanEditToolMenuItems(Menu menu, final Entity entity, EntityType entityType) {		
		// add Can Edit permission tools here (other than add and edit)
	}

	private void showAddWindow(EntityType childType, String parentId) {
		final String typeDisplay = DisplayUtils.uppercaseFirstLetter(childType.getName());
		final Window window = new Window();  
		window.setSize(600, 275);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading(DisplayConstants.LABEL_CREATE + " " + typeDisplay);
		window.setLayout(new FitLayout());				
		nodeEditor.addCancelHandler(new CancelHandler() {					
			@Override
			public void onCancel(CancelEvent event) {
				window.hide();
			}
		});
		nodeEditor.addPersistSuccessHandler(new EntityUpdatedHandler() {					
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				window.hide();
				presenter.fireEntityUpdatedEvent();
			}
		});
		nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
		window.add(nodeEditor.asWidget(DisplayUtils.getNodeTypeForEntityType(childType), null, parentId), new FitData(4));
		window.show();
	}

}
