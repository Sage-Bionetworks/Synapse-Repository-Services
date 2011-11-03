package org.sagebionetworks.web.client.view;

import java.util.Date;
import java.util.List;
import java.util.Set;

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
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.EnvironmentDescriptor;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.Reference;

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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * @author deflaux
 * 
 */
public class StepViewImpl extends Composite implements StepView {

	/**
	 * @author deflaux
	 * 
	 */
	public interface StepViewImplUiBinder extends
			UiBinder<Widget, StepViewImpl> {
	}

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
	SimplePanel followStepButtonPanel;
	@UiField
	SimplePanel seeTermsButtonPanel;
	@UiField
	FlowPanel overviewPanel;
	@UiField
	FlexTable propertiesFlexTable;
	@UiField
	FlexTable environmentDescriptorsFlexTable;
	@UiField
	SimplePanel referenceListTablePanel;
	@UiField
	FlexTable referencesFlexTable;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	@UiField
	SimplePanel addReferencePanel;
	@UiField
	FlexTable commandHistoryFlexTable;

	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private IconsImageBundle iconsImageBundle;
	private boolean isAdministrator = false;
	private boolean canEdit = false;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	@SuppressWarnings("unused")
	private AdminMenu adminMenu;
	private ModalWindow followStepModal;
	@SuppressWarnings("unused")
	private ModalWindow seeTermsModal;
	@SuppressWarnings("unused")
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private Header headerWidget;
	private AnnotationEditor annotationEditor;

	private static final String STEP_DETAILS_STYLE = "step_details";
	private static final String REFERENCES_TABLE_STYLE = "references_table";
	private static final String REFERENCES_TABLE_HEADING_STYLE = "references_table_heading";

	/**
	 * @param binder
	 * @param headerWidget
	 * @param footerWidget
	 * @param iconsImageBundle
	 * @param imageBundle
	 * @param followStepModal
	 * @param seeTermsModal
	 * @param previewDisclosurePanel
	 * @param queryServiceTableResourceProvider
	 * @param accessMenuButton
	 * @param nodeEditor
	 * @param annotationEditor
	 * @param adminMenu
	 */
	@Inject
	public StepViewImpl(
			StepViewImplUiBinder binder,
			Header headerWidget,
			Footer footerWidget,
			IconsImageBundle iconsImageBundle,
			SageImageBundle imageBundle,
			ModalWindow followStepModal,
			ModalWindow seeTermsModal,
			PreviewDisclosurePanel previewDisclosurePanel,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			AccessMenuButton accessMenuButton, NodeEditor nodeEditor,
			AnnotationEditor annotationEditor, AdminMenu adminMenu) {
		initWidget(binder.createAndBindUi(this));

		this.previewDisclosurePanel = previewDisclosurePanel;
		this.iconsImageBundle = iconsImageBundle;
		this.accessMenuButton = accessMenuButton;
		this.nodeEditor = nodeEditor;
		this.adminMenu = adminMenu;
		this.followStepModal = followStepModal;
		this.seeTermsModal = seeTermsModal;
		this.queryServiceTableResourceProvider = queryServiceTableResourceProvider;
		this.headerWidget = headerWidget;
		this.annotationEditor = annotationEditor;

		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);

		// alignment setup
		propertiesFlexTable.setCellSpacing(5);
		environmentDescriptorsFlexTable.setCellSpacing(5);
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
	public void setStepDetails(String id, String name, String description,
			String createdBy, Date creationDate, Date startDate, Date endDate,
			String commandLine, Set<Reference> code, Set<Reference> input,
			Set<Reference> output,
			Set<EnvironmentDescriptor> environmentDescriptors,
			boolean isAdministrator, boolean canEdit) {

		// Assure reasonable values
		if (id == null)
			id = "";
		if (name == null)
			name = "";
		if (description == null)
			description = "";
		if (createdBy == null)
			createdBy = "";
		if (commandLine == null)
			commandLine = "";

		// clear out any previous values in the view
		clear();

		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(id);
		createAdminPanel(id);

		Anchor followStep = createFollowStepButton(iconsImageBundle,
				followStepModal);
		followStepButtonPanel.clear();
		followStepButtonPanel.add(followStep);

		// fill in fields
		titleSpan.setInnerText(name);
		synapseIdSpan.setInnerText(DisplayConstants.SYNAPSE_ID_PREFIX + id);
		rClientCodeDiv.setInnerHTML(DisplayUtils.getRClientEntityLoad(id));
		rClientCodeDiv.setClassName(DisplayUtils.STYLE_CODE_CONTENT);
		breadcrumbTitleSpan.setInnerText(name);

		// step overview
		int summaryLength = description.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH
				: description.length();
		previewDisclosurePanel.init("Expand", description.substring(0,
				summaryLength), description);
		overviewPanel.add(previewDisclosurePanel);

		// add metadata to tables
		int rowIndex = 0;
		DisplayUtils.addRowToTable(rowIndex++, "Created By:", createdBy,
				STEP_DETAILS_STYLE, propertiesFlexTable);
		DisplayUtils.addRowToTable(rowIndex++, "Command Line:", commandLine,
				STEP_DETAILS_STYLE, propertiesFlexTable);
		DisplayUtils.addRowToTable(rowIndex++, "Start Date:",
				(null != startDate) ? DisplayConstants.DATE_FORMAT
						.format(startDate) : "", STEP_DETAILS_STYLE,
				propertiesFlexTable);
		DisplayUtils.addRowToTable(rowIndex++, "End Date:",
				(null != endDate) ? DisplayConstants.DATE_FORMAT
						.format(endDate) : "", STEP_DETAILS_STYLE,
				propertiesFlexTable);

		rowIndex = 0;
		for (EnvironmentDescriptor descriptor : environmentDescriptors) {
			String descriptorDisplay = (null == descriptor.getQuantifier() || (0 == descriptor
					.getQuantifier().length())) ? descriptor.getName()
					: descriptor.getName() + ", " + descriptor.getQuantifier();
			DisplayUtils.addRowToTable(rowIndex++, descriptor.getType(),
					descriptorDisplay, STEP_DETAILS_STYLE,
					environmentDescriptorsFlexTable);
		}

		// List of references table
		rowIndex = 1;
		referencesFlexTable.setHTML(rowIndex, 0, "Reference Type");
		referencesFlexTable.setHTML(rowIndex, 1, "Entity Id");
		referencesFlexTable.setHTML(rowIndex, 2, "Entity Version");
		referencesFlexTable.getRowFormatter().addStyleName(rowIndex,
				REFERENCES_TABLE_HEADING_STYLE);
		rowIndex++;
		rowIndex = addRefsToReferenceTable(rowIndex, code, "Code Reference");
		rowIndex = addRefsToReferenceTable(rowIndex, input,
				"Input Layer Reference");
		rowIndex = addRefsToReferenceTable(rowIndex, output,
				"Output Layer Reference");
		referencesFlexTable.setStyleName(REFERENCES_TABLE_STYLE);
		referenceListTablePanel.clear();
		referenceListTablePanel.add(referencesFlexTable.asWidget());

		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(NodeType.STEP, id);
		annotationsPanel.add(annotationEditor.asWidget());
	}

	/*
	 * Private Methods
	 */
	private Anchor createReferenceLink(final String referenceId) {
		Anchor referenceLink = new Anchor();
		referenceLink.setHTML(referenceId);
		referenceLink.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.doLookupEntity(referenceId);
			}
		});
		return referenceLink;
	}

	private int addRefsToReferenceTable(int rowIndex,
			Set<Reference> references, String referenceLabel) {
		for (Reference ref : references) {
			Anchor referenceLink = createReferenceLink(ref.getTargetId());
			DisplayUtils.addRowToTable(rowIndex++, referenceLabel,
					referenceLink, ref.getTargetVersionNumber().toString(),
					STEP_DETAILS_STYLE, referencesFlexTable);
		}
		return rowIndex;
	}

	private Anchor createFollowStepButton(IconsImageBundle icons,
			final ModalWindow followStepModal) {
		followStepModal.setHeading("Follow this Step");
		followStepModal.setDimensions(180, 500);
		followStepModal.setHtml(DisplayConstants.FOLLOW_STEP_HTML);
		followStepModal.setCallbackButton("Confirm", new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				// TODO : call a service layer to follow the reference
				followStepModal.hideWindow();
			}

			@Override
			public void onFailure(Throwable caught) {
			}
		});
		// follow link
		Anchor followReferenceAnchor = new Anchor();
		followReferenceAnchor.setHTML(AbstractImagePrototype.create(
				icons.arrowCurve16()).getHTML()
				+ " Follow this Step");
		followReferenceAnchor.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				followStepModal.showWindow();
			}
		});
		return followReferenceAnchor;
	}

	private void createAdminPanel(String stepId) {
		if (canEdit) {
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle
					.adminTools16()));
			// adminButton.setIconAlign(IconAlign.LEFT);
			button.setMenu(createAdminMenu(stepId));
			button.setHeight(25);
			adminPanel.add(button);

			// add reference button on page
			addReferencePanel.clear();
			addReferencePanel.add(createAddReferenceLink(stepId));
		}
	}

	private Anchor createAddReferenceLink(final String stepId) {
		Anchor addReferenceLink = new Anchor();
		addReferenceLink.setHTML(DisplayUtils.getIconHtml(iconsImageBundle
				.addSquare16())
				+ " " + DisplayConstants.BUTTON_ADD_REFERENCE);
		addReferenceLink.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showAddReferenceWindow(stepId);
			}
		});
		return addReferenceLink;
	}

	private Menu createAdminMenu(final String stepId) {
		Menu menu = new Menu();
		MenuItem item = null;

		// Edit menu options
		if (canEdit) {
			item = new MenuItem(DisplayConstants.BUTTON_EDIT_STEP_DETAILS);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle
					.applicationEdit16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				@Override
				public void componentSelected(MenuEvent menuEvent) {
					final Window window = new Window();
					window.setSize(600, 300);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Step");
					window.setLayout(new FitLayout());
					nodeEditor.addCancelHandler(new CancelHandler() {
						@Override
						public void onCancel(CancelEvent event) {
							window.hide();
						}
					});
					nodeEditor
							.addPersistSuccessHandler(new PersistSuccessHandler() {
								@Override
								public void onPersistSuccess(
										PersistSuccessEvent event) {
									window.hide();
									presenter.refresh();
								}
							});
					nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
					window.add(nodeEditor.asWidget(NodeType.STEP, stepId),
							new FitData(4));
					window.show();
				}
			});
			menu.add(item);

			item = new MenuItem(DisplayConstants.BUTTON_ADD_REFERENCE_TO_STEP);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle
					.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				@Override
				public void componentSelected(MenuEvent menuEvent) {
					showAddReferenceWindow(stepId);
				}
			});
			menu.add(item);
		}

		// Administrator Menu Options
		if (isAdministrator) {
			item = new MenuItem(DisplayConstants.LABEL_DELETE_STEP);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle
					.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				@Override
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm(DisplayConstants.LABEL_DELETE_STEP,
							"Are you sure you want to delete this step?",
							new Listener<MessageBoxEvent>() {
								@Override
								public void handleEvent(MessageBoxEvent be) {
									Button btn = be.getButtonClicked();
									if (Dialog.YES.equals(btn.getItemId())) {
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
		if (accessLevel == AccessLevel.PUBLIC) {
			icon = iconsImageBundle.lockUnlocked16();
		} else {
			icon = iconsImageBundle.lock16();
		}

		if (isAdministrator) {
			accessMenuButton.setPlaceChanger(presenter.getPlaceChanger());
			accessMenuButton.createAccessButton(accessLevel, NodeType.STEP, id);
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan
					.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"
							+ DisplayUtils.getIconHtml(icon)
							+ " "
							+ accessLevel + "</span>");
		}
	}

	private void showAddReferenceWindow(final String stepId) {
		final Window window = new Window();
		window.setSize(600, 370);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading(DisplayConstants.TITLE_CREATE_REFERENCE);
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
		window.add(nodeEditor.asWidget(NodeType.STEP, null, stepId),
				new FitData(4));
		window.show();
	}

	@Override
	public void setCommandHistoryTable(List<String> commands) {
		int rowIndex = 0;
		for (String command : commands) {
			DisplayUtils.addRowToTable(rowIndex++, Integer.toString(rowIndex),
					command, STEP_DETAILS_STYLE, commandHistoryFlexTable);
			if (1 == (rowIndex % 2)) {
				commandHistoryFlexTable.getRowFormatter().addStyleName(
						rowIndex, REFERENCES_TABLE_HEADING_STYLE);
			}
		}
	}

}
