package org.sagebionetworks.web.client.presenter;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.StepsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.StepView;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.Step;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

/**
 * @author deflaux
 * 
 */
public class StepPresenter extends AbstractActivity implements
		StepView.Presenter {

	private static final String COMMAND_HISTORY_ANNOTATION_KEY = "rHistory";

	private org.sagebionetworks.web.client.place.Step place;
	private StepView view;
	private String stepId;
	private NodeServiceAsync nodeService;
	private PlaceChanger placeChanger;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	@SuppressWarnings("unused")
	private GlobalApplicationState globalApplicationState;
	private LookupPresenter lookupPresenter;

	/**
	 * @param view
	 * @param service
	 * @param nodeModelCreator
	 * @param authenticationController
	 * @param globalApplicationState
	 */
	@Inject
	public StepPresenter(StepView view, NodeServiceAsync service,
			NodeModelCreator nodeModelCreator,
			AuthenticationController authenticationController,
			GlobalApplicationState globalApplicationState) {
		this.view = view;
		this.nodeService = service;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();

		// Set the presenter on the view
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	/**
	 * @param place
	 */
	public void setPlace(org.sagebionetworks.web.client.place.Step place) {
		this.place = place;
		this.stepId = this.place.toToken();
		view.setPresenter(this);

		// load the step given in the Step Place
		refresh();
	}

	@Override
	public void refresh() {
		loadFromServer();
		loadCommandHistory();
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
	public void delete() {
		nodeService.deleteNode(NodeType.STEP, stepId,
				new AsyncCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
						view.showInfo("Step Deleted",
								"The step was successfully deleted.");
						placeChanger.goTo(new StepsHome(
								DisplayUtils.DEFAULT_PLACE_TOKEN));
					}

					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Step delete failed.");
					}
				});
	}

	/**
	 * Protected Methods
	 */
	protected void loadFromServer() {
		// Fetch the data about this step from the server
		nodeService.getNodeJSON(NodeType.STEP, this.stepId,
				new AsyncCallback<String>() {

					@Override
					public void onSuccess(String result) {
						Step step = null;
						try {
							step = nodeModelCreator.createStep(result);
						} catch (RestServiceException ex) {
							if (!DisplayUtils.handleServiceException(ex,
									placeChanger, authenticationController
											.getLoggedInUser())) {
								onFailure(null);
							}
							return;
						}
						setStep(step);
					}

					@Override
					public void onFailure(Throwable caught) {
						setStep(null);
						view
								.showErrorMessage("An error retrieving the Step occured. Please try reloading the page.");
					}
				});
	}

	/**
	 * Sends the step elements to the view
	 * 
	 * @param step
	 */
	protected void setStep(final Step step) {
		if (step != null) {
			UserData currentUser = authenticationController.getLoggedInUser();
			if (currentUser != null) {
				AclUtils.getHighestPermissionLevel(NodeType.STEP, step.getId(),
						nodeService, new AsyncCallback<PermissionLevel>() {
							@Override
							public void onSuccess(PermissionLevel result) {
								boolean isAdministrator = false;
								boolean canEdit = false;
								if (result == PermissionLevel.CAN_EDIT) {
									canEdit = true;
								} else if (result == PermissionLevel.CAN_ADMINISTER) {
									canEdit = true;
									isAdministrator = true;
								}
								setStepDetails(step, isAdministrator, canEdit);
							}

							@Override
							public void onFailure(Throwable caught) {
								view
										.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
								setStepDetails(step, false, false);
							}
						});
			} else {
				// because this is a public page, they can view
				setStepDetails(step, false, false);
			}
		}
	}

	private void setStepDetails(final Step step, boolean isAdministrator,
			boolean canEdit) {
		view.setStepDetails(step.getId(), step.getName(),
				step.getDescription(), step.getCreatedBy(), step
						.getCreationDate(), step.getStartDate(), step
						.getEndDate(), step.getCommandLine(), step.getCode(),
				step.getInput(), step.getOutput(), step
						.getEnvironmentDescriptors(), isAdministrator, canEdit);
	}

	@Override
	public String mayStop() {
		view.clear();
		return null;
	}

	/**
	 * References can be of any entity type, use the Lookup presenter when folks
	 * click on reference links
	 * 
	 * @param lookupPresenter
	 */
	public void setLookupPresenter(LookupPresenter lookupPresenter) {
		this.lookupPresenter = lookupPresenter;
	}

	@Override
	public void doLookupEntity(final String entityId) {
		lookupPresenter.doLookupEntity(entityId);
	}

	/**
	 * Load the command history for this step from the server
	 */
	private void loadCommandHistory() {
		nodeService.getNodeAnnotationsJSON(NodeType.STEP, stepId,
				new AsyncCallback<String>() {
					@Override
					public void onFailure(Throwable caught) {
						// set the table even if we can't get the annotations
						view.setCommandHistoryTable(null);
					}

					@Override
					public void onSuccess(String annotationJsonString) {
						try {
							List<String> commandHistory = null;
							Annotations annotations = nodeModelCreator
									.createAnnotations(annotationJsonString);
							Map<String, List<String>> strAnnotations = annotations
									.getStringAnnotations();
							for (String annotKey : strAnnotations.keySet()) {
								if (annotKey
										.equals(COMMAND_HISTORY_ANNOTATION_KEY)) {
									commandHistory = strAnnotations
											.get(annotKey);
								}
							}
							view.setCommandHistoryTable(commandHistory);
						} catch (RestServiceException ex) {
							DisplayUtils.handleServiceException(ex,
									placeChanger, authenticationController
											.getLoggedInUser());
						}
					}
				});
	}
}
