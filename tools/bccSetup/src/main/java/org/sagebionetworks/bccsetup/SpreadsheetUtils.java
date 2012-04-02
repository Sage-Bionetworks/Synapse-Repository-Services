package org.sagebionetworks.bccsetup;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;

import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
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

public class SpreadsheetUtils {
	  
	  public static final String SPREADSHEET_SCOPE = "https://spreadsheets.google.com/feeds https://docs.google.com/feeds";
	  public static final String SPREADSHEET_FEED_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
	  public static final String APPLICATION_NAME = "synapse";
	  
	  private static final String BCC_REGISTRANT_SPREADSHEET_TITLE = "BCC Registrants"  ;
	  private static final String REGISTRANT_COLUMN_TITLE = "Registrant Email Address";
	  private static final String APPROVAL_COLUMN_TITLE = "Date approved for participation";
	  private static final String ALLOCATED_COLUMN_TITLE = "Date resources allocated";
	  
	  
	  public static void main(String[] args) throws Exception {
		  SpreadsheetService spreadsheetService = createSpreadsheetService();
		  List<String> participantsToAllocate = participantsToAllocate(spreadsheetService);
		  System.out.println("Will allocate resources for "+participantsToAllocate);
		  for (String participant : participantsToAllocate) {
			  recordAllocation(spreadsheetService, participant, "<timestamp>"); // TODO:  would use an actual time stamp here
		  }
		  System.out.println("Done.");
     }
	  
	  public static String columnHeaderToTag(String s) {return s.toLowerCase().replaceAll(" ", "");}
	  
	  public static List<String> participantsToAllocate(SpreadsheetService service) throws Exception {
		  List<String> ans = new ArrayList<String>();
		  WorksheetEntry worksheetEntry = getBCCWorksheet(service);
		  URL listFeedUrl = worksheetEntry.getListFeedUrl();
		  ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);
		  for (ListEntry entry : feed.getEntries()) {
		    CustomElementCollection cec = entry.getCustomElements();
		   
		    if (cec.getValue(columnHeaderToTag(APPROVAL_COLUMN_TITLE))!=null &&
		    		cec.getValue(columnHeaderToTag(ALLOCATED_COLUMN_TITLE))==null ){
		    	ans.add(cec.getValue(columnHeaderToTag(REGISTRANT_COLUMN_TITLE)));
		    }
		  }	
		  return ans;
	  }
	  
	  public static void recordAllocation(SpreadsheetService service, String participant, String allocationTimestamp) throws Exception {
		  WorksheetEntry worksheetEntry = getBCCWorksheet(service);
		  URL listFeedUrl = worksheetEntry.getListFeedUrl();
		  ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);
		  for (ListEntry entry : feed.getEntries()) {
		    CustomElementCollection cec = entry.getCustomElements();
		    
		    if (cec.getValue(columnHeaderToTag(REGISTRANT_COLUMN_TITLE)).equals(participant)) {
		    	// TODO:  check that the column is initially null and that the 'approval' column is not null
		    	cec.setValueLocal(columnHeaderToTag(ALLOCATED_COLUMN_TITLE), allocationTimestamp);
		    	ListEntry updatedRow = entry.update();
		    	return;
		    }
		  }			  
		  throw new IllegalStateException("No entry found for "+participant);
	  }
	  
	  public static WorksheetEntry getBCCWorksheet(SpreadsheetService service) throws Exception {
		    // Make a request to the API and get all spreadsheets.
		    SpreadsheetFeed feed = service.getFeed(new URL(SPREADSHEET_FEED_URL), SpreadsheetFeed.class);
		    List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		    for (SpreadsheetEntry spreadsheet : spreadsheets) {
		    	if (!spreadsheet.getTitle().getPlainText().equals(BCC_REGISTRANT_SPREADSHEET_TITLE)) continue;

			    List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
			    if (worksheets.size()!=1) throw new RuntimeException("Expected just one worksheet in "+BCC_REGISTRANT_SPREADSHEET_TITLE);

			    return worksheets.iterator().next();
		    }
		    throw new RuntimeException("Unable to find "+BCC_REGISTRANT_SPREADSHEET_TITLE);
		  
	  }
  
	  
	  public static SpreadsheetService createSpreadsheetService() throws Exception {
		    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();

			String consumerKey = StackConfiguration.getGoogleAppsOAuthConsumerKey();
			String consumerSecret = StackConfiguration.getGoogleAppsOAuthConsumerSecret();
			String accessToken = StackConfiguration.getGoogleAppsOAuthAccessToken();
			String accessTokenSecret = StackConfiguration.getGoogleAppsOAuthAccessTokenSecret();
			  
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
