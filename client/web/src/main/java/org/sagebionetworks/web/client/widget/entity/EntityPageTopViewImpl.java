package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.entity.children.EntityChildBrowser;
import org.sagebionetworks.web.client.widget.entity.menu.ActionMenu;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
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
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityPageTopViewImpl extends Composite implements EntityPageTopView {

	public interface Binder extends UiBinder<Widget, EntityPageTopViewImpl> {
	}
	
	@UiField
	SimplePanel breadcrumbsPanel;
	@UiField
	SimplePanel actionMenuPanel;
	@UiField 
	SimplePanel portalPanel;
	@UiField 
	SimplePanel portalPanelSingleCol;
	@UiField 
	SimplePanel portalPanelThreeCol;
	
	private Presenter presenter;
	private SageImageBundle sageImageBundle;
	private IconsImageBundle iconsImageBundle;
	private PreviewDisclosurePanel previewDisclosurePanel;	
	private AnnotationEditor annotationEditor;
	private AdminMenu adminMenu;
	private ActionMenu actionMenu;
	private EntityChildBrowser entityChildBrowser;
	private LicensedDownloader licensedDownloader;
	private Breadcrumb breadcrumb;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
		
	@Inject
	public EntityPageTopViewImpl(Binder uiBinder,
			SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle,
			AccessMenuButton accessMenuButton, NodeEditor nodeEditor,
			PreviewDisclosurePanel previewDisclosurePanel,
			AnnotationEditor annotationEditor, AdminMenu adminMenu, ActionMenu actionMenu,
			EntityChildBrowser entityChildBrowser, LicensedDownloader licensedDownloader, Breadcrumb breadcrumb) {
		this.iconsImageBundle = iconsImageBundle;
		this.sageImageBundle = sageImageBundle;
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.annotationEditor = annotationEditor;
		this.adminMenu = adminMenu;
		this.actionMenu = actionMenu;
		this.entityChildBrowser = entityChildBrowser;
		this.licensedDownloader = licensedDownloader;
		this.breadcrumb = breadcrumb;
		
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
		breadcrumbsPanel.clear();
		breadcrumbsPanel.add(breadcrumb.asWidget(entity));

		//setup action menu
		actionMenuPanel.clear();		
		actionMenu.addEntityUpdatedHandler(new EntityUpdatedHandler() {			
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				presenter.fireEntityUpdatedEvent();
			}
		});
		actionMenuPanel.add(actionMenu.asWidget(entity, isAdministrator, canEdit));
		
		// Portal #1 - 2 columns
		Portal portalTwoCol = new Portal(2);  
	    portalTwoCol.setBorders(false);  
	    portalTwoCol.setStyleAttribute("backgroundColor", "white");  
	    portalTwoCol.setColumnWidth(0, .57);  
	    portalTwoCol.setColumnWidth(1, .43);	 	    
	    portalPanel.clear();
	    portalPanel.add(portalTwoCol);	    
	    // Title
	    portalTwoCol.add(createTitlePortlet(entity, entityTypeDisplay), 0);	    
	    // Description	    
		portalTwoCol.add(createDescriptionPortlet(entity), 0);	    
	    // Annotation Editor Portlet
		portalTwoCol.add(createAnnotationEditorPortlet(entity), 1);	    
	    
	    
		// Portal #2 - full width
		Portal portalSingleCol = new Portal(1);  
	    portalSingleCol.setBorders(false);  
	    portalSingleCol.setStyleAttribute("backgroundColor", "white");  
	    portalSingleCol.setColumnWidth(0, 1.0);  	 	    
	    portalPanelSingleCol.clear();
	    portalPanelSingleCol.add(portalSingleCol);	    
	    // Child Browser
	    portalSingleCol.add(createEntityChildBrowser(entity, canEdit), 0);
	    
	    
		// Portal #3 - 3 columns
		Portal portalThreeCol = new Portal(3);  
		portalThreeCol.setBorders(false);  
		portalThreeCol.setStyleAttribute("backgroundColor", "white");  
		portalThreeCol.setColumnWidth(0, .33);  	 	    
		portalThreeCol.setColumnWidth(1, .33);  	 	    
		portalThreeCol.setColumnWidth(2, .33);  	 	    
		portalPanelThreeCol.clear();
		portalPanelThreeCol.add(portalThreeCol);
	    // Create R Client portlet
	    portalThreeCol.add(createRClientPortlet(entity), 0);
	    // Create References portlet
	    portalThreeCol.add(createReferencesPortlet(entity), 1);
	    // Create References portlet
	    portalThreeCol.add(createActivityFeedPortlet(entity), 2);
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
		
		// download button		
		if(presenter.isLocationable()) {
			portlet.add(licensedDownloader.asWidget(entity, false), new MarginData(10, 0, 0, 0));
		}
		
		// add some properties		
		String propString = "Modified By " + entity.getModifiedBy() + " on "
		+ DisplayUtils.convertDateToString(entity.getModifiedOn())
		+ "<br/>" 
		+ "Created By " + entity.getCreatedBy()
		+ " on "
		+ DisplayUtils.convertDateToString(entity.getCreatedOn());
		
		if(entity instanceof Versionable) {
			propString += "<br/>" + "Version " + entity.getVersion();
		}
		
		Html propHtml = new Html(propString);
		propHtml.setStyleName(DisplayUtils.STYLE_SMALL_GREY_TEXT);		
		portlet.add(propHtml, new MarginData(10,0,0,0));
		
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

	private Portlet createAnnotationEditorPortlet(Entity entity) {
	    SynapsePortlet portlet = new SynapsePortlet("Properties &amp; Annotations", true, false);  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);  	  
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(DisplayUtils.getNodeTypeForEntity(entity), entity.getId());				
		portlet.add(annotationEditor.asWidget());
		return portlet;
	}	
	
	
	
	private Portlet createRClientPortlet(Entity entity) {			  
	    SynapsePortlet portlet = new SynapsePortlet("Synapse R Client");  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);  	  

	    // setup install code widgets		
	    Html loadEntityCode = new Html(DisplayUtils.getRClientEntityLoad(entity.getId()));
	    loadEntityCode.setStyleName(DisplayUtils.STYLE_CODE_CONTENT);		
		
		final LayoutContainer container = new LayoutContainer();
	    String rSnipet = "# " + DisplayConstants.LABEL_R_CLIENT_INSTALL
		+ "<br/>" + DisplayUtils.R_CLIENT_DOWNLOAD_CODE;
		container.addText(rSnipet);	    		
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
	
	private Portlet createReferencesPortlet(Entity entity) {			  
	    SynapsePortlet portlet = new SynapsePortlet("References");  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);  	  
	    
	    if(DisplayConstants.showDemoHtml && DisplayConstants.MSKCC_DATASET_DEMO_ID.equals(entity.getId())) {					
			portlet.add(new HTML(DisplayConstants.DEMO_ANALYSIS));
		}

	    
	    return portlet;  
	}	
	
	private Portlet createActivityFeedPortlet(Entity entity) {			  
	    SynapsePortlet portlet = new SynapsePortlet("Activity Feed");  
	    portlet.setLayout(new FitLayout());    
	    portlet.setAutoHeight(true);
	    
		if(DisplayConstants.showDemoHtml && DisplayConstants.MSKCC_DATASET_DEMO_ID.equals(entity.getId())) {			
			portlet.add(new HTML(DisplayConstants.DEMO_COMMENTS));
		}

	    return portlet;  
	}	
	
	private Portlet createEntityChildBrowser(Entity entity, boolean canEdit) {
		String typeDisplay = DisplayUtils.getEntityTypeDisplay(entity);
		SynapsePortlet portlet = new SynapsePortlet(typeDisplay + " " + "Contents");
		portlet.add(entityChildBrowser.asWidget(entity, canEdit));
		return portlet;
	}
	
	
}
