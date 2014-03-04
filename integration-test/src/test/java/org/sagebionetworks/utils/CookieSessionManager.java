package org.sagebionetworks.utils;


//import com.gdevelop.gwt.syncrpc.CookieManager;
//import com.gdevelop.gwt.syncrpc.CredentialsManager;
//import com.gdevelop.gwt.syncrpc.LoginCredentials;
//import com.gdevelop.gwt.syncrpc.LoginProvider;
//import com.gdevelop.gwt.syncrpc.LoginProviderRegistry;
//import com.gdevelop.gwt.syncrpc.SessionManager;

public class CookieSessionManager 
//implements SessionManager
{

//	  private PublicCookieManager cookieManager = new PublicCookieManager();
//	  private CredentialsManager credentialsManager;
//	  private HashSet<String> loggedIn = new HashSet<String>();
//
//
//	  @Override
//	  public HttpURLConnection openConnection(URL url) throws Exception {
//	    login(url);
//	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//	    cookieManager.setCookies(connection);
//	    return connection;
//	  }
//
//	  @Override
//	  public void handleResponseHeaders(HttpURLConnection connection) throws IOException {
//	    cookieManager.storeCookies(connection);
//	  }
//	  
//	  public void login(URL url) throws Exception {
//	    if(credentialsManager == null) {
//	      return; 
//	    }
//	    
//	    LoginCredentials credentials = credentialsManager.getLoginCredentials(url);
//	    if(credentials == null)
//	    {
//	      return;
//	    }
//	    
//	    //If we've already logged in return:
//	    if(loggedIn.contains(credentials.getLoginUrl()))
//	    {
//	      return;
//	    }
//	    
//	    LoginProvider provider = LoginProviderRegistry.getLoginProvider(credentials.getLoginScheme());
//	    if(provider == null)
//	    {
//	      throw new IOException(credentials.getLoginScheme() + " is not a registered login scheme");
//	    }
//	    provider.login(url, credentials, this);
//	    loggedIn.add(credentials.getLoginUrl());
//	  }
//
//	  public CookieManager getCookieManager() {
//	    return cookieManager;
//	  }
//
//	  @Override
//	  public void setCredentialsManager(CredentialsManager credentialsManager) {
//	    this.credentialsManager = credentialsManager;
//	  }

}
