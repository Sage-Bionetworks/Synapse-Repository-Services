package org.sagebionetworks.bccsetup;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;

import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthSigner;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

public class SpreadsheetHelper {
	  
	  public static final String SPREADSHEET_SCOPE = "https://spreadsheets.google.com/feeds https://docs.google.com/feeds";
	  public static final String SPREADSHEET_FEED_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
	  public static final String APPLICATION_NAME = "synapse";
	  
	  public static final String REGISTRANT_COLUMN_TITLE = "Registrant Email Address";
	  public static final String APPROVAL_COLUMN_TITLE = "Date approved for participation";
	  public static final String ALLOCATED_COLUMN_TITLE = "Date resources allocated";
	  
	  private static final String spreadSheetTitle = BccConfigHelper.getBCCSpreadsheetTitle();
	  
	  private SpreadsheetService spreadsheetService = null;
	  
	  public SpreadsheetHelper() {
		  try {
			  spreadsheetService = createSpreadsheetService();
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }
	  }
	  
	  public static String columnHeaderToTag(String s) {return s.toLowerCase().replaceAll(" ", "");}
	  
	  /*
	   * Returns the users which have been approved for participation but for which resources have not yet been allocated.
	   * @return a List of participants (which are email addresses)
	   */
	  public  List<String> getParticipantsToAllocate() {
		  List<String> ans = new ArrayList<String>();
		  try {
			  WorksheetEntry worksheetEntry = getBCCWorksheet(spreadSheetTitle);
			  URL listFeedUrl = worksheetEntry.getListFeedUrl();
			  ListFeed feed = spreadsheetService.getFeed(listFeedUrl, ListFeed.class);
			  for (ListEntry entry : feed.getEntries()) {
			    CustomElementCollection cec = entry.getCustomElements();
			   
			    if (cec.getValue(columnHeaderToTag(APPROVAL_COLUMN_TITLE))!=null &&
			    		cec.getValue(columnHeaderToTag(ALLOCATED_COLUMN_TITLE))==null ){
			    	ans.add(cec.getValue(columnHeaderToTag(REGISTRANT_COLUMN_TITLE)));
			    }
			  }	
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }
		  return ans;
	  }
	  
	  /**
	   * Records in the shared spreadsheet that the given participant has been allocated resources
	   * 
	   * @param participant the email address of the participant
	   * @param allocationTimestamp the date/time when the resources were allocated
	   */
	  public void recordAllocation(String participant, String allocationTimestamp) {
		  setCellValue(participant, ALLOCATED_COLUMN_TITLE, allocationTimestamp);
	  }
		  
	  /**
	   * Sets a value in the shared spreadsheet for a given participant (i.e. a given row) and a given column title
	   * 
	   * @param participant
	   * @param column
	   * @param value the desired value. value=null 'clears' the existing value.
	   * @exception if the participant or the column are not found in the spreadsheet
	   */
	  public void setCellValue(String participant, String column, String value) {
		  try {
			  WorksheetEntry worksheetEntry = getBCCWorksheet(spreadSheetTitle);
			  URL listFeedUrl = worksheetEntry.getListFeedUrl();
			  ListFeed feed = spreadsheetService.getFeed(listFeedUrl, ListFeed.class);
			  for (ListEntry entry : feed.getEntries()) {
			    CustomElementCollection cec = entry.getCustomElements();
			    
			    if (cec.getValue(columnHeaderToTag(REGISTRANT_COLUMN_TITLE)).equals(participant)) {
			    	if (value==null) {
			    		cec.clearValueLocal(columnHeaderToTag(column));
			    	} else {
			    		cec.setValueLocal(columnHeaderToTag(column), value);
			    	}
			    	entry.update();
			    	return;
			    }
			  }			  
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }
		  throw new IllegalStateException("No entry found for "+participant);
	  }
	  
	  private WorksheetEntry getBCCWorksheet(String spreadsheetTitle) throws IOException, ServiceException {
		    SpreadsheetFeed feed = spreadsheetService.getFeed(new URL(SPREADSHEET_FEED_URL), SpreadsheetFeed.class);
		    List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		    for (SpreadsheetEntry spreadsheet : spreadsheets) {
		    	if (!spreadsheet.getTitle().getPlainText().equals(spreadsheetTitle)) continue;

			    List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
			    if (worksheets.size()!=1) throw new RuntimeException("Expected just one worksheet in "+spreadsheetTitle);

			    return worksheets.iterator().next();
		    }
		    throw new RuntimeException("Unable to find "+spreadsheetTitle);
		  
	  }
  
	  
	  public static SpreadsheetService createSpreadsheetService() throws ServiceException, IOException, OAuthException {
		    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();

			String consumerKey = BccConfigHelper.getGoogleAppsOAuthConsumerKey();
			String consumerSecret = BccConfigHelper.getGoogleAppsOAuthConsumerSecret();
			String accessToken = BccConfigHelper.getGoogleAppsOAuthAccessToken();
			String accessTokenSecret = BccConfigHelper.getGoogleAppsOAuthAccessTokenSecret();
			  
		    oauthParameters.setOAuthConsumerKey(consumerKey);
	        oauthParameters.setOAuthConsumerSecret(consumerSecret);

		    oauthParameters.setScope(SPREADSHEET_SCOPE);
		    oauthParameters.setOAuthToken(accessToken);
		    oauthParameters.setOAuthTokenSecret(accessTokenSecret);

		    SpreadsheetService googleService =  new SpreadsheetService(APPLICATION_NAME);
		    
	        OAuthSigner signer = new OAuthHmacSha1Signer();
		    googleService.setOAuthCredentials(oauthParameters, signer);
		    return googleService;
	  }
	  
}
