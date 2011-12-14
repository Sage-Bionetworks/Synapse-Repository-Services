package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.entity.children.EntityChildBrowser;
import org.sagebionetworks.web.client.widget.entity.menu.ActionMenu;
import org.sagebionetworks.web.client.widget.portlet.SynapsePortlet;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.shared.NodeType;

import com.extjs.gxt.ui.client.Style.Direction;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.fx.FxConfig;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.custom.Portal;
import com.extjs.gxt.ui.client.widget.custom.Portlet;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
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
	SimplePanel actionMenuPanel;
	@UiField 
	SimplePanel portalPanel;
	@UiField 
	SimplePanel portalPanelSingleCol;
	
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private PreviewDisclosurePanel previewDisclosurePanel;	
	private AnnotationEditor annotationEditor;
	private AdminMenu adminMenu;
	private ActionMenu actionMenu;
	private EntityChildBrowser entityChildBrowser;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
		
	@Inject
	public EntityPageTopViewImpl(Binder uiBinder,
			SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle,
			AccessMenuButton accessMenuButton, NodeEditor nodeEditor,
			PreviewDisclosurePanel previewDisclosurePanel,
			AnnotationEditor annotationEditor, AdminMenu adminMenu, ActionMenu actionMenu,
			EntityChildBrowser entityChildBrowser) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.annotationEditor = annotationEditor;
		this.adminMenu = adminMenu;
		this.actionMenu = actionMenu;
		this.entityChildBrowser = entityChildBrowser;
		
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@Override
	public void setEntityDetails(Entity entity, String entityTypeDisplay, boolean isAdministrator,
			boolean canEdit) {
		
		NodeType entityType = DisplayUtils.getNodeTypeForEntity(entity);
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		// add breadcrumbs
		breadcrumbTitleSpan.setInnerText(entity.getName());

		//setup action menu
		actionMenuPanel.clear();		
		actionMenu.addEntityUpdatedHandler(new EntityUpdatedHandler() {			
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				presenter.fireEntityUpdatedEvent();
			}
		});
		actionMenuPanel.add(actionMenu.asWidget(entity, isAdministrator, canEdit));
		
		// configure portal
		Portal portal = new Portal(2);  
	    portal.setBorders(false);  
	    portal.setStyleAttribute("backgroundColor", "white");  
	    portal.setColumnWidth(0, .57);  
	    portal.setColumnWidth(1, .43);	 	    
	    portalPanel.clear();
	    portalPanel.add(portal);
	    
	    // Title
	    portal.add(createTitlePortlet(entity, entityTypeDisplay), 0);
	    
	    // Description	    
		portal.add(createDescriptionPortlet(entity), 0);
	    
	    // Annotation Editor Portlet
		portal.add(createAnnotationEditorPortlet(portal, entity), 1);	    
	    // Create R Client portlet
	    portal.add(createRClientPortlet(portal, entity), 0);
	    
	    // Full Width portal
		Portal portalSingleCol = new Portal(1);  
	    portalSingleCol.setBorders(false);  
	    portalSingleCol.setStyleAttribute("backgroundColor", "white");  
	    portalSingleCol.setColumnWidth(0, 1.0);  	 	    
	    portalPanelSingleCol.clear();
	    portalPanelSingleCol.add(portalSingleCol);
	    
	    // Child Browser
	    portalSingleCol.add(createEntityChildBrowser(entity, canEdit), 0);
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
		actionMenu.clearState();
		// TODO : add other widgets here
	}
	
	/*
	 * Private Methods
	 */
	private SynapsePortlet createDescriptionPortlet(Entity entity) {
		SynapsePortlet portlet = new SynapsePortlet("Description");
		String overviewText = entity.getDescription();
		if(overviewText == null) overviewText = "";
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		portlet.add(previewDisclosurePanel);
		return portlet;
	}

	private SynapsePortlet createTitlePortlet(Entity entity, String entityTypeDisplay) {
	    String title = "<span style=\"font-weight:lighter;\">["
				+ entityTypeDisplay.substring(0, 1)
				+ "]</span> "
				+ entity.getName()
				+ "<br/><span style=\"font-size: 60%\">"
				+ DisplayConstants.SYNAPSE_ID_PREFIX + entity.getId()
				+ "</span>";
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
	
	private Portlet createEntityChildBrowser(Entity entity, boolean canEdit) {
		String typeDisplay = DisplayUtils.getEntityTypeDisplay(entity);
		SynapsePortlet portlet = new SynapsePortlet(typeDisplay + " " + "Contents");
		portlet.add(entityChildBrowser.asWidget(entity, canEdit));
		return portlet;
	}
	
	
}
