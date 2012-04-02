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
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;

public class SpreadsheetUtils {
	  
	  public static final String SPREADSHEET_SCOPE = "https://spreadsheets.google.com/feeds https://docs.google.com/feeds";
	  public static final String SPREADSHEET_FEED_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
	  public static final String APPLICATION_NAME = "synapse";
	  
	  private static final int HEADER_ROW = 0;
	  private static final int REGISTRANT_COL = 0;
	  private static final int APPROVAL_COL = 1;
	  private static final int ALLOCATED_COL = 2;
	  
	  private static final String BCC_REGISTRANT_SPREADSHEET_TITLE = "BCC Registrants"  ;
	  private static final String REGISTRANT_COLUMN_TITLE = "Registrant Email Address";
	  private static final String APPROVAL_COLUMN_TITLE = "Date approved for participation";
	  private static final String ALLOCATED_COLUMN_TITLE = "Date resources allocated";
	  
	  
	  public static void main(String[] args) throws Exception {
		  SpreadsheetService spreadsheetService = createSpreadsheetService();
		  CellFeed cellFeed = getCellFeedForBCCRegistrationSpreadsheet(spreadsheetService);
		  WorksheetModel model = getWorksheetModel(cellFeed);
		  List<String> participantsToAllocate = participantsToAllocate(model);
		  System.out.println("Need to allocate resources for "+participantsToAllocate);
  }
  
	  private static CellFeed getCellFeedForBCCRegistrationSpreadsheet(SpreadsheetService service) throws Exception {
			    // Make a request to the API and get all spreadsheets.
			    SpreadsheetFeed feed = service.getFeed(new URL(SPREADSHEET_FEED_URL), SpreadsheetFeed.class);
			    List<SpreadsheetEntry> spreadsheets = feed.getEntries();

			    for (SpreadsheetEntry spreadsheet : spreadsheets) {
			    	if (!spreadsheet.getTitle().getPlainText().equals(BCC_REGISTRANT_SPREADSHEET_TITLE)) continue;
	
				    List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
				    if (worksheets.size()!=1) throw new RuntimeException("Expected just one worksheet in "+BCC_REGISTRANT_SPREADSHEET_TITLE);
	
				    WorksheetEntry worksheet = worksheets.iterator().next();
				    return service.getFeed(worksheet.getCellFeedUrl(), CellFeed.class);
			    }
			    throw new RuntimeException("Unable to find "+BCC_REGISTRANT_SPREADSHEET_TITLE);
	  }
	  
	  public static List<String> participantsToAllocate(WorksheetModel model) {
		  List<String> ans = new ArrayList<String>();
		    if (!model.get(HEADER_ROW, REGISTRANT_COL).getValue().equals(REGISTRANT_COLUMN_TITLE)) throw new RuntimeException(REGISTRANT_COLUMN_TITLE+" expected.");
		    if (!model.get(HEADER_ROW, APPROVAL_COL).getValue().equals(APPROVAL_COLUMN_TITLE)) throw new RuntimeException(APPROVAL_COLUMN_TITLE+" expected.");
		    if (!model.get(HEADER_ROW, ALLOCATED_COL).getValue().equals(ALLOCATED_COLUMN_TITLE)) throw new RuntimeException(ALLOCATED_COLUMN_TITLE+" expected.");
		    for (int i=1; i<model.getRows(); i++) {
		    	Cell participantCell = model.get(i, REGISTRANT_COL);
		    	if (participantCell==null) continue;
		    	String participantName = participantCell.getValue();
		    	boolean isApproved = null!=model.get(i, APPROVAL_COL);
		    	boolean isAllocated = null!=model.get(i, ALLOCATED_COL);
		    	if (isApproved && !isAllocated) ans.add(participantName);
		    }
		    return ans;
	  }
	  
	  /**
	   * 
	   * read in the worksheet feed and populate the model
	   * @param worksheet
	   * @return
	   */
	  public static WorksheetModel getWorksheetModel(CellFeed cellFeed) {
		  WorksheetModel ans = new WorksheetModel();
	      List<CellEntry> cellEntries = cellFeed.getEntries();
	      for (CellEntry cellEntry : cellEntries) {
	    	  Cell cell = cellEntry.getCell();
	    	  ans.add(cell.getRow()-1, cell.getCol()-1, cell);
	      }
	      return ans;
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
