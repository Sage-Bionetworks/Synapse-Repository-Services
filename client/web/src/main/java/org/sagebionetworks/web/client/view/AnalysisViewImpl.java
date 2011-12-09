package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.WindowEvent;
import com.extjs.gxt.ui.client.event.WindowListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Info;
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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AnalysisViewImpl extends Composite implements AnalysisView {

	public interface AnalysisViewImplUiBinder extends UiBinder<Widget, AnalysisViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
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
	SimplePanel followAnalysisButtonPanel;
	@UiField
	SimplePanel seeTermsButtonPanel;
	@UiField
	FlowPanel overviewPanel;
	@UiField
	SimplePanel stepListTablePanel;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	@UiField
	SimplePanel addStepPanel;
	
	
	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private QueryServiceTable stepsListQueryServiceTable;
	private IconsImageBundle iconsImageBundle;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	private AdminMenu adminMenu;
	private ModalWindow followAnalysisModal;
	private ModalWindow seeTermsModal;
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private Header headerWidget;
	private AnnotationEditor annotationEditor;

	@Inject
	public AnalysisViewImpl(AnalysisViewImplUiBinder binder, Header headerWidget,
			Footer footerWidget, IconsImageBundle iconsImageBundle,
			SageImageBundle imageBundle, ModalWindow followAnalysisModal,
			ModalWindow seeTermsModal,
			PreviewDisclosurePanel previewDisclosurePanel,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			AccessMenuButton accessMenuButton,
			NodeEditor nodeEditor,
			AnnotationEditor annotationEditor,
			AdminMenu adminMenu) {		
		initWidget(binder.createAndBindUi(this));

		this.previewDisclosurePanel = previewDisclosurePanel;
		this.iconsImageBundle = iconsImageBundle;
		this.accessMenuButton = accessMenuButton;
		this.nodeEditor = nodeEditor;
		this.adminMenu = adminMenu;
		this.followAnalysisModal = followAnalysisModal;
		this.seeTermsModal = seeTermsModal;
		this.queryServiceTableResourceProvider =  queryServiceTableResourceProvider;
		this.headerWidget = headerWidget;
		this.annotationEditor = annotationEditor;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);				
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		headerWidget.refresh();
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void clear() {
		titleSpan.setInnerText("");
		rClientCodeDiv.setInnerHTML("");
		adminPanel.clear();
	}

	@Override
	public void setAnalysisDetails(String id, String name, String description,
			String createdBy, Date creationDate, boolean isAdministrator, boolean canEdit) {
		// Assure reasonable values
		if(id == null) id = "";
		if(name == null) name = "";
		if(description == null) description = "";
		if(createdBy == null) createdBy = "";		

		// clear out any previous values in the view
		clear();
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(id);
		createAdminPanel(id); 
		
		Anchor followAnalysis = createFollowAnalysisButton(iconsImageBundle, followAnalysisModal);
		followAnalysisButtonPanel.clear();
		followAnalysisButtonPanel.add(followAnalysis);			

		// List of steps table
		setupStepTable(id);
		stepListTablePanel.clear();
		stepListTablePanel.add(stepsListQueryServiceTable.asWidget());				
		
		// fill in fields
		titleSpan.setInnerText(name);
		synapseIdSpan.setInnerText(DisplayConstants.SYNAPSE_ID_PREFIX + id);
		rClientCodeDiv.setInnerHTML(DisplayUtils.getRClientEntityLoad(id));
		rClientCodeDiv.setClassName(DisplayUtils.STYLE_CODE_CONTENT);
		breadcrumbTitleSpan.setInnerText(name);
		
		// analysis overview
		int summaryLength = description.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : description.length();
		previewDisclosurePanel.init("Expand", description.substring(0, summaryLength), description);
		overviewPanel.add(previewDisclosurePanel);		

		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(NodeType.ANALYSIS, id);
		annotationsPanel.add(annotationEditor.asWidget());			
	}

	/*
	 * Private Methods
	 */
	private void setupStepTable(String id) {
		int stepTableWidth = 320;
		int stepTableHeight = 237;
		stepsListQueryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.step, true, stepTableWidth, stepTableHeight, presenter.getPlaceChanger());
		List<String> visibileColumns = new ArrayList<String>();
		visibileColumns.add("step.NameLink");
		visibileColumns.add("step.createdBy");
		visibileColumns.add("step.modifiedOn");
		stepsListQueryServiceTable.setDispalyColumns(visibileColumns, false);
		// load the steps for this analysis
		List<WhereCondition> whereList = new ArrayList<WhereCondition>();
		whereList.add(new WhereCondition("step.parentId", WhereOperator.EQUALS, id));
		stepsListQueryServiceTable.setWhereCondition(whereList);
	}


	private Anchor createFollowAnalysisButton(IconsImageBundle icons,
			final ModalWindow followAnalysisModal) {
		followAnalysisModal.setHeading("Follow this Analysis");
		followAnalysisModal.setDimensions(180, 500);		
		followAnalysisModal.setHtml(DisplayConstants.FOLLOW_ANALYSIS_HTML);
		followAnalysisModal.setCallbackButton("Confirm", new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				// TODO : call a service layer to follow the step				
				followAnalysisModal.hideWindow();
			}

			@Override
			public void onFailure(Throwable caught) {			}
		});
		// follow link		
		Anchor followStepAnchor = new Anchor();
		followStepAnchor.setHTML(AbstractImagePrototype.create(icons.arrowCurve16()).getHTML() + " Follow this Analysis");
		followStepAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				followAnalysisModal.showWindow();
			}
		});		
		return followStepAnchor;		
	}

	private void createAdminPanel(String analysisId) {		
		if(canEdit) {
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
			//adminButton.setIconAlign(IconAlign.LEFT);
			button.setMenu(createAdminMenu(analysisId));
			button.setHeight(25);
			adminPanel.add(button);
			
			// add step button on page			
			addStepPanel.clear();			
			addStepPanel.add(createAddStepLink(analysisId));
		}
	}	
	
	private Anchor createAddStepLink(final String analysisId) {
		Anchor addStepLink = new Anchor();
		addStepLink.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.addSquare16()) + " " + DisplayConstants.BUTTON_ADD_STEP);
		addStepLink.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				showAddStepWindow(analysisId);
			}
		});
		return addStepLink;
	}



	private Menu createAdminMenu(final String analysisId) {
		Menu menu = new Menu();		
		MenuItem item = null; 

		// Edit menu options
		if(canEdit) {		
			item = new MenuItem(DisplayConstants.BUTTON_EDIT_ANALYSIS_DETAILS);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 300);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Analysis");
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
							presenter.refresh();
						}
					});
					nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
					window.add(nodeEditor.asWidget(NodeType.ANALYSIS, analysisId), new FitData(4));
					window.show();
				}
			});
			menu.add(item);
			
			item = new MenuItem(DisplayConstants.BUTTON_ADD_STEP_TO_ANALYSIS);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					showAddStepWindow(analysisId);
				}
			});
			menu.add(item);
		}
		
		// Administrator Menu Options
		if(isAdministrator) {
			item = new MenuItem(DisplayConstants.LABEL_DELETE_ANALYSIS);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm(DisplayConstants.LABEL_DELETE_ANALYSIS, "Are you sure you want to delete this analysis?", new Listener<MessageBoxEvent>() {					
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

	private void createAccessPanel(String id) {				
		AccessLevel accessLevel = AccessLevel.SHARED;		
		ImageResource icon = null;
		if(accessLevel == AccessLevel.PUBLIC) {
			icon = iconsImageBundle.lockUnlocked16();
		} else {
			icon = iconsImageBundle.lock16();
		}		

		if(isAdministrator) {		
			accessMenuButton.setPlaceChanger(presenter.getPlaceChanger());
			accessMenuButton.createAccessButton(accessLevel, NodeType.ANALYSIS, id);			
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"+ DisplayUtils.getIconHtml(icon) +" "+ accessLevel +"</span>");
		}
	}

	private void showAddStepWindow(final String analysisId) {
		final Window window = new Window();  
		window.setSize(600, 370);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading(DisplayConstants.TITLE_CREATE_STEP);
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
				presenter.refresh();
			}
		});
		nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
		window.add(nodeEditor.asWidget(NodeType.STEP, null, analysisId), new FitData(4));
		window.show();
	}

}
