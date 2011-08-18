package org.sagebionetworks.web.client;


import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.shared.exceptions.BadRequestException;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlexTable;

public class DisplayUtils {

	private static final String REGEX_CLEAN_ANNOTATION_KEY = "^[a-z,A-Z,0-9,_,.]+";
	private static final String REGEX_CLEAN_ENTITY_NAME = "^[a-z,A-Z,0-9,_,., ,\\-,\\+,(,)]+";
	public static final String REPO_ENTITY_NAME_KEY = "name";
		
	public static final String MIME_TYPE_JPEG = "image/jpeg";
	public static final String MIME_TYPE_PNG = "image/png";
	public static final String MIME_TYPE_GIF = "image/gif";
	
	public static final String DEFAULT_PLACE_TOKEN = "0";
	public static PlaceController placeController;	
	
	private static final String ERROR_OBJ_REASON_KEY = "reason";
	/**
	 * Returns a properly aligned icon from an ImageResource
	 * @param icon
	 * @return
	 */
	public static String getIconHtml(ImageResource icon) {
		return "<span class=\"iconSpan\">" + AbstractImagePrototype.create(icon).getHTML() + "</span>";
	}
	
	/**
	 * This returns a properly formatted default History Token for a given Place. This method should
	 * be used in place of static strings of the Place class name. For example, don't use <a href="Home:0">...</a> 
	 * in your UiBinder templates. Instead use a <g:Hyperlink ui:field="link" /> and use Hyperlink's setTargetHistoryToken() 
	 * method with the returned value of this method (with Home.class as the parameter).  
	 * 
	 * @param place A class that extends com.google.gwt.place.shared.Place
	 * @return
	 */
	public static String getDefaultHistoryTokenForPlace(@SuppressWarnings("rawtypes") Class place) {		
		return getPlaceString(place) + ":" + DEFAULT_PLACE_TOKEN;
	}

	/**
	 * Similar to getDefaultHistoryTokenForPlace but inserts the given token instead of the default token
	 * @param place
	 * @param token
	 * @return
	 */
	public static String getHistoryTokenForPlace(@SuppressWarnings("rawtypes") Class place, String token) {
		return getPlaceString(place) + ":" + token;
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
					} else if (code == 400) { // Bad Request
						String message = "";
						if(obj.containsKey(ERROR_OBJ_REASON_KEY)) {
							message = obj.get(ERROR_OBJ_REASON_KEY).isString().stringValue();							
						}
						throw new BadRequestException(message);
					} else {
						throw new UnknownErrorException("Unknown Service error. code: " + code);
					}
				}
			}
		}
	}	

	/**
	 * Handles the exception. Resturn true if the user has been alerted to the exception already
	 * @param ex
	 * @param placeChanger
	 * @return true if the user has been prompted
	 */
	public static boolean handleServiceException(RestServiceException ex, PlaceChanger placeChanger, UserData currentUser) {
		if(ex instanceof UnauthorizedException) {
			// send user to login page						
			Info.display("Session Timeout", "Your session has timed out. Please login again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
			return true;
		} else if(ex instanceof ForbiddenException) {			
			if(currentUser == null) {				
				Info.display(DisplayConstants.ERROR_LOGIN_REQUIRED, DisplayConstants.ERROR_LOGIN_REQUIRED);
				placeChanger.goTo(new LoginPlace(DisplayUtils.DEFAULT_PLACE_TOKEN));
			} else {
				MessageBox.info("Unauthorized", "Sorry, there was a failure due to insufficient privileges.", null);
			}
			return true;
		} else if(ex instanceof BadRequestException) {
			String reason = ex.getMessage();			
			String message = DisplayConstants.ERROR_BAD_REQUEST_MESSAGE;
			if(reason.matches(".*entity with the name: .+ already exites.*")) {
				message = DisplayConstants.ERROR_DUPLICATE_ENTITY_MESSAGE;
			}			
			MessageBox.info("Error", message, null);
			return true;
		} else if(ex instanceof NotFoundException) {
			MessageBox.info("Not Found", "Sorry, the requested object was not found.", null);
			placeChanger.goTo(new Home(DisplayUtils.DEFAULT_PLACE_TOKEN));
			return true;
		} 			
		
		// For other exceptions, allow the consumer to send a good message to the user
		return false;
	}
	
	/*
	 * Button Saving 
	 */
	public static void changeButtonToSaving(Button button, SageImageBundle sageImageBundle) {
		button.setText(DisplayConstants.BUTTON_SAVING);
		button.setIcon(AbstractImagePrototype.create(sageImageBundle.loading16()));
	}

	/**
	 * Check if an Annotation key is valid with the repository service
	 * @param key
	 * @return
	 */
	public static boolean validateAnnotationKey(String key) {
		if(key.matches(REGEX_CLEAN_ANNOTATION_KEY)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Check if an Entity (Node) name is valid with the repository service
	 * @param key
	 * @return
	 */
	public static boolean validateEntityName(String key) {
		if(key.matches(REGEX_CLEAN_ENTITY_NAME)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Cleans any invalid name characters from a string  
	 * @param str
	 * @return
	 */
	public static String getOffendingCharacterForEntityName(String key) {
		return getOffendingCharacter(key, REGEX_CLEAN_ENTITY_NAME);
	}

	/**
	 * Cleans any invalid name characters from a string  
	 * @param str
	 * @return
	 */
	public static String getOffendingCharacterForAnnotationKey(String key) {
		return getOffendingCharacter(key, REGEX_CLEAN_ANNOTATION_KEY);
	}	
	
	/**
	 * Cleans any invalid name characters from a string  
	 * @param str
	 * @return
	 */
	public static String cleanForEntityName(String str) {
		return str.replaceAll("[^A-Za-z0-9_\\. ]", "_");
	}
	
	/*
	 * Private methods
	 */
	private static String getPlaceString(Class place) {
		String fullPlaceName = place.getName();		
		fullPlaceName = fullPlaceName.replaceAll(".+\\.", "");
		return fullPlaceName;
	}

	/**
	 * Returns the offending character given a regex string
	 * @param key
	 * @param regex
	 * @return
	 */
	private static String getOffendingCharacter(String key, String regex) {
		String suffix = key.replaceFirst(regex, "");
		if(suffix != null && suffix.length() > 0) {
			return suffix.substring(0,1);
		}
		return null;		
	}

}
