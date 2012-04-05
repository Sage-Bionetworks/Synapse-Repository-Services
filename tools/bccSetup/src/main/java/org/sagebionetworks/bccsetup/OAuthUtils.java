package org.sagebionetworks.bccsetup;

import org.sagebionetworks.StackConfiguration;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthSigner;

/**
 * 
 * This utility will generate a persistant OAUth access token, token-secret pair given the Consumer info and user approval.
 * 
 * @author brucehoff
 *
 */
public class OAuthUtils {
	  public static void main(String[] args) throws Exception {
		  String consumerKey = BccConfigHelper.getGoogleAppsOAuthConsumerKey();
		  String consumerSecret = BccConfigHelper.getGoogleAppsOAuthConsumerSecret();
		  
		  generateAccessToken(consumerKey, consumerSecret, SpreadsheetHelper.SPREADSHEET_SCOPE);
	  }

	  public static void generateAccessToken(
			  String consumerKey, String consumerSecret,
			  String scope) throws Exception {
		    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
		    oauthParameters.setOAuthConsumerSecret(consumerSecret);
		    oauthParameters.setOAuthConsumerKey(consumerKey);
		    oauthParameters.setScope(scope);
		    OAuthSigner signer = new OAuthHmacSha1Signer();
		    GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(signer);
		    oauthHelper.getUnauthorizedRequestToken(oauthParameters);
		    String requestUrl = oauthHelper.createUserAuthorizationUrl(oauthParameters);
		    System.out.println("Please visit the following URL to authorize your OAuth "
		        + "request token.  Once that is complete, press any key to continue...");
		    System.out.println(requestUrl);
		    System.in.read();

		    String token = oauthHelper.getAccessToken(oauthParameters);
		    System.out.println("OAuth Access Token: " + token); // can also get from oauthParameters.getOAuthToken()
		    System.out.println("OAuth Access Token Secret: " + oauthParameters.getOAuthTokenSecret());
		    System.out.println();
	  }
}
