package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.portlet.SynapsePortlet;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.shared.NodeType;

import com.extjs.gxt.ui.client.Style.Direction;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.fx.FxConfig;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.extjs.gxt.ui.client.widget.custom.Portlet;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityPageTopViewImpl extends Composite implements EntityPageTopView {

	public interface Binder extends UiBinder<Widget, EntityPageTopViewImpl> {
	}
	
	@UiField 
	SpanElement breadcrumbTitleSpan;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	@UiField 
	SimplePanel portalPanel;
	
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	private AnnotationEditor annotationEditor;
	private AdminMenu adminMenu;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
		
	@Inject
	public EntityPageTopViewImpl(Binder uiBinder,
			SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle,
			AccessMenuButton accessMenuButton, NodeEditor nodeEditor,
			PreviewDisclosurePanel previewDisclosurePanel,
			AnnotationEditor annotationEditor, AdminMenu adminMenu) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.accessMenuButton = accessMenuButton;
		this.nodeEditor = nodeEditor;
		this.annotationEditor = annotationEditor;
		this.adminMenu = adminMenu;
		
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@Override
	public void setEntityDetails(Entity entity, boolean isAdministrator,
			boolean canEdit) {
		// Clear everything
		clear();
		
		NodeType entityType = DisplayUtils.getNodeTypeForEntity(entity);
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(entity.getId());
		createAdminPanel(entity.getId(), entityType);	
		// add breadcrumbs
		breadcrumbTitleSpan.setInnerText(entity.getName());

		// configure portal
		Portal portal = new Portal(2);  
	    portal.setBorders(false);  
	    portal.setStyleAttribute("backgroundColor", "white");  
	    portal.setColumnWidth(0, .57);  
	    portal.setColumnWidth(1, .43);	 	    
	    portalPanel.clear();
	    portalPanel.add(portal);
	    
	    // Title
	    portal.add(createTitlePortlet(entity), 0);
	    
	    // Description	    
		portal.add(createDescriptionPortlet(entity), 0);
	    
	    // Annotation Editor Portlet
		portal.add(createAnnotationEditorPortlet(portal, entity), 1);	    
	    // Create R Client portlet
	    portal.add(createRClientPortlet(portal, entity), 1);
		
		
	}

	private SynapsePortlet createDescriptionPortlet(Entity entity) {
		SynapsePortlet portlet = new SynapsePortlet("Description");
		String overviewText = entity.getDescription();
		if(overviewText == null) overviewText = "";
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		portlet.add(previewDisclosurePanel);
		return portlet;
	}

	private SynapsePortlet createTitlePortlet(Entity entity) {
	    Html synapseId = new Html();
	    synapseId.setStyleAttribute("font-size", "60%");
	    String title = entity.getName() + "<br/><span style=\"font-size: 60%\">" + DisplayConstants.SYNAPSE_ID_PREFIX + entity.getId() + "</span>";
	    SynapsePortlet titlePortlet = new SynapsePortlet(title, true, true);
	    titlePortlet.setAutoHeight(true);
		return titlePortlet;
	}

	private Portlet createAnnotationEditorPortlet(Portal portal, Entity entity) {
	    SynapsePortlet portlet = new SynapsePortlet("Properties &amp; Annotations", true, false);  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);  	  
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(DisplayUtils.getNodeTypeForEntity(entity), entity.getId());				
		portlet.add(annotationEditor.asWidget());
		return portlet;
	}	
	
	
	
	private Portlet createRClientPortlet(Portal portal, Entity entity) {			  
	    SynapsePortlet portlet = new SynapsePortlet("Synapse R Client");  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);  	  
	   	
	    Html loadEntityCode = new Html(DisplayUtils.getRClientEntityLoad(entity.getId()));
	    loadEntityCode.setStyleName(DisplayUtils.STYLE_CODE_CONTENT);		
		
		// setup install code widgets
		final LayoutContainer container = new LayoutContainer();	    
		container.addText("# " + DisplayConstants.LABEL_INSTALL_R_CLIENT_CODE
				+ "<br/>" + DisplayUtils.R_CLIENT_DOWNLOAD_CODE);	    		
	    container.setStyleName(DisplayUtils.STYLE_CODE_CONTENT);
	    container.setVisible(false);
	    
	    
		Button getRClientButton = new Button(DisplayConstants.BUTTON_SHOW_R_CLIENT_INSTALL,
				new SelectionListener<ButtonEvent>() {
					public void componentSelected(ButtonEvent ce) {
						if (container.isVisible()) {							
							container.el().slideOut(Direction.UP, FxConfig.NONE);
						} else {
							container.setVisible(true);
							container.el().slideIn(Direction.DOWN, FxConfig.NONE);
						}
					}

				});
		getRClientButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.cog16()));
		
		VerticalPanel vp = new VerticalPanel();
		vp.add(getRClientButton);
		vp.add(container);
		
		portlet.add(new Html("<p>The Synapse R Client allows you to interact with the Synapse system programmatically.</p>"));
		portlet.add(loadEntityCode);
		portlet.add(vp);
	    return portlet;  
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
		// TODO
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
		
	}
	
	@Override
	public void showEntityDeleteFailure() {
		// TODO Auto-generated method stub
		
	}

	
	/*
	 * Private Methods
	 */
	private void createAccessPanel(String id) {		
		// TODO : get access level from Authorization service
		AccessLevel accessLevel = AccessLevel.SHARED;		
		ImageResource icon = null;
		if(accessLevel == AccessLevel.PUBLIC) {
			icon = iconsImageBundle.lockUnlocked16();
		} else {
			icon = iconsImageBundle.lock16();
		}		

		if(isAdministrator) {		
			accessMenuButton.setPlaceChanger(presenter.getPlaceChanger());			
			accessMenuButton.createAccessButton(accessLevel, NodeType.DATASET, id);
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"+ DisplayUtils.getIconHtml(icon) +" "+ accessLevel +"</span>");
		}
	}
	

	private void createAdminPanel(String id, NodeType entityType) {		
		if(canEdit) {
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
			button.setMenu(createAdminMenu(id, entityType));
			button.setHeight(25);
			adminPanel.clear();
			adminPanel.add(button);	
		}		
	}

	private Menu createAdminMenu(final String datasetId, NodeType entityType) {
		Menu menu = new Menu();		
		MenuItem item = null; 

		// Edit menu options
		if(canEdit) {			
			item = new MenuItem("Edit Dataset Details");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 345);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Dataset");
					window.setLayout(new FitLayout());								
					nodeEditor.addCancelHandler(new CancelHandler() {					
						@Override
						public void onCancel(CancelEvent event) {
							window.hide();
						}
					});
					nodeEditor.addPersistSuccessHandler(new PersistSuccessHandler() {					
						@Override
						public void onPersistSuccess(PersistSuccessEvent event) {
							window.hide();
							presenter.refresh();
						}
					});
					nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
					window.add(nodeEditor.asWidget(NodeType.DATASET, datasetId), new FitData(4));				
					window.show();
				}
			});
			menu.add(item);
						 
			item = new MenuItem(DisplayConstants.BUTTON_ADD_A_LAYER_TO_DATASET);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					showAddLayerWindow(datasetId);
				}

			});
			menu.add(item);
		}
		
		// Administrator Menu Options
		if(isAdministrator) {
			item = new MenuItem("Delete Dataset");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm("Delete Dataset", "Are you sure you want to delete this dataset?", new Listener<MessageBoxEvent>() {					
						@Override
						public void handleEvent(MessageBoxEvent be) { 					
							Button btn = be.getButtonClicked();
							if(Dialog.YES.equals(btn.getItemId())) {
								presenter.delete();
							}
						}
					});
				}
			});
			menu.add(item);
		}

		return menu;
	}

	private void showAddLayerWindow(final String datasetId) {
		final Window window = new Window();  
		window.setSize(600, 275);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading("Create Layer");
		window.setLayout(new FitLayout());				
		nodeEditor.addCancelHandler(new CancelHandler() {					
			@Override
			public void onCancel(CancelEvent event) {
				window.hide();
			}
		});
		nodeEditor.addPersistSuccessHandler(new PersistSuccessHandler() {					
			@Override
			public void onPersistSuccess(PersistSuccessEvent event) {
				window.hide();
				presenter.refresh();
			}
		});
		nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
		window.add(nodeEditor.asWidget(NodeType.LAYER, null, datasetId), new FitData(4));
		window.show();
	}
	
}
