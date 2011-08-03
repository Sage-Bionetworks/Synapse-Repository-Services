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
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
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

public class ProjectViewImpl extends Composite implements ProjectView {

	public interface ProjectViewImplUiBinder extends UiBinder<Widget, ProjectViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SpanElement titleSpan;
	@UiField 
	SpanElement breadcrumbTitleSpan;
	@UiField
	SimplePanel annotationsPanel;
//	@UiField
//	FlexTable rightFlexTable;
	@UiField
	SimplePanel followProjectButtonPanel;
	@UiField
	SimplePanel seeTermsButtonPanel;
	@UiField
	FlowPanel overviewPanel;
	@UiField
	SimplePanel datasetListTablePanel;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	
	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private QueryServiceTable datasetsListQueryServiceTable;
	private IconsImageBundle iconsImageBundle;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	private AdminMenu adminMenu;
	private ModalWindow followProjectModal;
	private ModalWindow seeTermsModal;
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private Header headerWidget;
	private AnnotationEditor annotationEditor;

	@Inject
	public ProjectViewImpl(ProjectViewImplUiBinder binder, Header headerWidget,
			Footer footerWidget, IconsImageBundle iconsImageBundle,
			SageImageBundle imageBundle, ModalWindow followProjectModal,
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
		this.followProjectModal = followProjectModal;
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
		MessageBox.info("Message", message, null);
	}

	@Override
	public void showInfo(String title, String message) {
		Info.display(title, message);
	}

	@Override
	public void setProjectDetails(String id, String name, String description,
			String creator, Date creationDate, String status, boolean isAdministrator, boolean canEdit) {
		// Assure reasonable values
		if(id == null) id = "";
		if(name == null) name = "";
		if(description == null) description = "";
		if(creator == null) creator = "";		
		if(status == null) status = "";

		// clear out any previous values in the view
		clear();
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(id);
		createAdminPanel(id);
		
		Anchor followProject = createFollowProjectButton(iconsImageBundle, followProjectModal);
		followProjectButtonPanel.clear();
		followProjectButtonPanel.add(followProject);			

		// List of datasets table
		setupDatasetTable(id);
		datasetListTablePanel.clear();
		datasetListTablePanel.add(datasetsListQueryServiceTable.asWidget());				
		
		// fill in fields
		titleSpan.setInnerText(name);
		breadcrumbTitleSpan.setInnerText(name);
		
		// project overview
		int summaryLength = description.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : description.length();
		previewDisclosurePanel.init("Expand", description.substring(0, summaryLength), description);
		overviewPanel.add(previewDisclosurePanel);		

		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(NodeType.PROJECT, id);
		annotationsPanel.add(annotationEditor.asWidget());

		// add values to annotation table
//		int rowIndex = 0;
//		if(creationDate != null) DisplayUtils.addRowToTable(rowIndex++, "Project Formed:", DisplayConstants.DATE_FORMAT.format(creationDate), rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Leaders:", "", rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Members:", "", rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Publications:", "", rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Status:", status, rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Project Web Site:", "", rightFlexTable);
				
	}

	/*
	 * Private Methods
	 */
	private void clear() {
		titleSpan.setInnerText("");
//		rightFlexTable.clear();
//		rightFlexTable.removeAllRows();
		adminPanel.clear();
	}

	private void setupDatasetTable(String id) {
		datasetsListQueryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.dataset, true, 320, 237, presenter.getPlaceChanger());
		// load the datasets for this project
		List<WhereCondition> whereList = new ArrayList<WhereCondition>();
		whereList.add(new WhereCondition("dataset.parentId", WhereOperator.EQUALS, id));
		datasetsListQueryServiceTable.setWhereCondition(whereList);
	}


	private Anchor createFollowProjectButton(IconsImageBundle icons,
			final ModalWindow followProjectModal) {
		followProjectModal.setHeading("Follow this Project");
		followProjectModal.setDimensions(180, 500);		
		followProjectModal.setHtml(DisplayConstants.FOLLOW_PROJECT_HTML);
		followProjectModal.setCallbackButton("Confirm", new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				// TODO : call a service layer to follow the dataset				
				followProjectModal.hideWindow();
			}

			@Override
			public void onFailure(Throwable caught) {			}
		});
		// follow link		
		Anchor followDatasetAnchor = new Anchor();
		followDatasetAnchor.setHTML(AbstractImagePrototype.create(icons.arrowCurve16()).getHTML() + " Follow this Project");
		followDatasetAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				followProjectModal.showWindow();
			}
		});		
		return followDatasetAnchor;		
	}

	private void createAdminPanel(String projectId) {		
		if(canEdit) {
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
			//adminButton.setIconAlign(IconAlign.LEFT);
			button.setMenu(createAdminMenu(projectId));
			button.setHeight(25);
			adminPanel.add(button);
		}
	}

	private Menu createAdminMenu(final String projectId) {
		Menu menu = new Menu();		
		MenuItem item = null; 

		// Edit menu options
		if(canEdit) {		
			item = new MenuItem("Edit Project Details");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 300);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Project");
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
					window.add(nodeEditor.asWidget(NodeType.PROJECT, projectId), new FitData(4));
					window.show();
				}
			});
			menu.add(item);
			
			item = new MenuItem("Add Dataset to Project");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 370);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Create Dataset");
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
					window.add(nodeEditor.asWidget(NodeType.DATASET, null, projectId), new FitData(4));
					window.show();
				}
			});
			menu.add(item);
		}
		
		// Administrator Menu Options
		if(isAdministrator) {
			item = new MenuItem("Delete Project");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm("Delete Project", "Are you sure you want to delete this project?", new Listener<MessageBoxEvent>() {					
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
			accessMenuButton.setResource(NodeType.PROJECT, id);
			accessMenuButton.setAccessLevel(accessLevel);
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"+ DisplayUtils.getIconHtml(icon) +" "+ accessLevel +"</span>");
		}
	}

}
