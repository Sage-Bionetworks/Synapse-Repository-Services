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
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.NodeType;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityPageTopViewImpl extends Composite implements EntityPageTopView {

	public interface Binder extends UiBinder<Widget, EntityPageTopViewImpl> {
	}
	
	@UiField
	FlowPanel overviewPanel;
	@UiField
	SpanElement titleSpan;
	@UiField 
	SpanElement synapseIdSpan;
	@UiField
	DivElement rClientCodeDiv;
	@UiField 
	SpanElement breadcrumbTitleSpan;
	@UiField
	SimplePanel annotationsPanel;
	@UiField
	SimplePanel bannerRightTopSlot;
	@UiField
	SimplePanel bannerRightBottomSlot;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;

	
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
	public EntityPageTopViewImpl(Binder uiBinder, SageImageBundle sageImageBundle, IconsImageBundle iconsImageBundle) {
		this.sageImageBundle = sageImageBundle;
		this.iconsImageBundle = iconsImageBundle;

		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@Override
	public void setEntityDetails(Entity entity, boolean isAdministrator,
			boolean canEdit) {
		// Clear everything
		clear();
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(entity.getId());
//		createAdminPanel(entity.getId());				
	
		
												
		// fill in fields
		titleSpan.setInnerText(entity.getName());
		synapseIdSpan.setInnerText(DisplayConstants.SYNAPSE_ID_PREFIX + entity.getId());
		rClientCodeDiv.setInnerHTML(DisplayUtils.getRClientEntityLoad(entity.getId()));
		rClientCodeDiv.setClassName(DisplayUtils.STYLE_CODE_CONTENT);
		breadcrumbTitleSpan.setInnerText(entity.getName());
		
		String overviewText = entity.getDescription();
		if(overviewText == null) overviewText = "";
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		overviewPanel.clear();
		overviewPanel.add(previewDisclosurePanel);		
		
		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(DisplayUtils.getNodeTypeForEntity(entity), entity.getId());
		annotationsPanel.add(annotationEditor.asWidget());
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
	

//	private void createAdminPanel(String id) {		
//		if(canEdit) {
//			Button button = new Button("Admin Menu");
//			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
//			button.setMenu(createAdminMenu(id));
//			button.setHeight(25);
//			adminPanel.clear();
//			adminPanel.add(button);
//			
//			
//		}
//		
//		if(canEdit) {
//			// add dataset button on page			
//			addLayerPanel.clear();			
//			addLayerPanel.add(createAddLayerLink(id));			
//		}		
//	}
//
//	private Anchor createAddLayerLink(final String datasetId) {
//		Anchor addLayerLink = new Anchor();
//		addLayerLink.setHTML(AbstractImagePrototype.create(iconsImageBundle.addSquare16()).getHTML() + " " + DisplayConstants.BUTTON_ADD_LAYER);
//		addLayerLink.addClickHandler(new ClickHandler() {			
//			@Override
//			public void onClick(ClickEvent event) {
//				showAddLayerWindow(datasetId);
//			}
//		});
//		return addLayerLink;
//	}
//
//	private Menu createAdminMenu(final String datasetId) {
//		Menu menu = new Menu();		
//		MenuItem item = null; 
//
//		// Edit menu options
//		if(canEdit) {			
//			item = new MenuItem("Edit Dataset Details");
//			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
//			item.addSelectionListener(new SelectionListener<MenuEvent>() {
//				public void componentSelected(MenuEvent menuEvent) {													
//					final Window window = new Window();  
//					window.setSize(600, 345);
//					window.setPlain(true);
//					window.setModal(true);
//					window.setBlinkModal(true);
//					window.setHeading("Edit Dataset");
//					window.setLayout(new FitLayout());								
//					nodeEditor.addCancelHandler(new CancelHandler() {					
//						@Override
//						public void onCancel(CancelEvent event) {
//							window.hide();
//						}
//					});
//					nodeEditor.addPersistSuccessHandler(new PersistSuccessHandler() {					
//						@Override
//						public void onPersistSuccess(PersistSuccessEvent event) {
//							window.hide();
//							presenter.refresh();
//						}
//					});
//					nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
//					window.add(nodeEditor.asWidget(NodeType.DATASET, datasetId), new FitData(4));				
//					window.show();
//				}
//			});
//			menu.add(item);
//						 
//			item = new MenuItem(DisplayConstants.BUTTON_ADD_A_LAYER_TO_DATASET);
//			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
//			item.addSelectionListener(new SelectionListener<MenuEvent>() {
//				public void componentSelected(MenuEvent menuEvent) {													
//					showAddLayerWindow(datasetId);
//				}
//
//			});
//			menu.add(item);
//		}
//		
//		// Administrator Menu Options
//		if(isAdministrator) {
//			item = new MenuItem("Delete Dataset");
//			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
//			item.addSelectionListener(new SelectionListener<MenuEvent>() {
//				public void componentSelected(MenuEvent menuEvent) {
//					MessageBox.confirm("Delete Dataset", "Are you sure you want to delete this dataset?", new Listener<MessageBoxEvent>() {					
//						@Override
//						public void handleEvent(MessageBoxEvent be) { 					
//							Button btn = be.getButtonClicked();
//							if(Dialog.YES.equals(btn.getItemId())) {
//								presenter.delete();
//							}
//						}
//					});
//				}
//			});
//			menu.add(item);
//		}
//
//		return menu;
//	}
//	
//	
}
