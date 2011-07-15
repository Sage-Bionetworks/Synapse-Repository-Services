package org.sagebionetworks.web.client;


import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;
import org.sagebionetworks.web.shared.users.PermissionLevel;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlexTable;

public class DisplayUtils {

	public static final String DEFAULT_PLACE_TOKEN = "0";
	public static PlaceController placeController;
	
	/**
	 * Returns a properly aligned icon from an ImageResource
	 * @param icon
	 * @return
	 */
	public static String getIconHtml(ImageResource icon) {
		return "<span class=\"iconSpan\">" + AbstractImagePrototype.create(icon).getHTML() + "</span>";
	}
	
	/**
	 * Add a row to the provided FlexTable.
	 * 
	 * @param key
	 * @param value
	 * @param table
	 */
	public static void addRowToTable(int row, String key, String value,
			FlexTable table) {
		table.setHTML(row, 0, key);
		table.getCellFormatter().addStyleName(row, 0, "boldRight");
		table.setHTML(row, 1, value);
	}

	public static void checkForErrors(JSONObject obj) throws RestServiceException {
		if(obj == null) return;
		if(obj.containsKey("error")) {
			JSONObject errorObj = obj.get("error").isObject();
			if(errorObj.containsKey("statusCode")) {
				JSONNumber codeObj = errorObj.get("statusCode").isNumber();
				if(codeObj != null) {
					int code = ((Double)codeObj.doubleValue()).intValue();
					if(code == 401) { // UNAUTHORIZED
						throw new UnauthorizedException();
					} else if(code == 403) { // FORBIDDEN
						throw new ForbiddenException();
					} else if (code == 404) { // NOT FOUND
						throw new NotFoundException();
					} else {
						throw new UnknownErrorException("Unknown Service error. code: " + code);
					}
				}
			}
		}
	}	

	public static void handleServiceException(RestServiceException ex, PlaceChanger placeChanger) {
		if(ex instanceof UnauthorizedException) {
			// send user to login page						
			Info.display("Session Timeout", "Your session has timed out. Please login again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
		} else if(ex instanceof ForbiddenException) {
			// alerting here this seems kinda lame, but keeps the code out of the client
			MessageBox.info("Unauthorized", "Sorry, there was a failure due to insufficient privledges.", null);			
		} else if(ex instanceof NotFoundException) {
			MessageBox.info("Not Found", "Sorrr, the requested object was not found.", null);
			placeChanger.goTo(new Home(DisplayUtils.DEFAULT_PLACE_TOKEN));
		} else {
			MessageBox.info("Unknown Error", "Sorry, an error has occured.", null);
		}
	}
	
	/*
	 * Button Saving 
	 */
	public static void changeButtonToSaving(Button button, SageImageBundle sageImageBundle) {
		button.setText(DisplayConstants.BUTTON_SAVING);
		button.setIcon(AbstractImagePrototype.create(sageImageBundle.loading16()));
	}
		
}
