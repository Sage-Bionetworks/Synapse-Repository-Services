package org.sagebionetworks.auth;

/*
 * Copyright 2006-2007 Sxip Identity Corporation
 */

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

/**
 * Sample Consumer (Relying Party) implementation.
 */

// http://code.google.com/p/openid4java/wiki/QuickStart
public class SampleConsumer
{
    private ConsumerManager manager;

    public SampleConsumer(ConsumerManager manager) {
        // instantiate a ConsumerManager object
//        manager = new ConsumerManager();
    	this.manager = manager;
    }

    public static final String AX_EMAIL = "Email";
    public static final String AX_FIRST_NAME = "FirstName";
    public static final String AX_LAST_NAME = "LastName";
    
    // --- placing the authentication request ---
    public String authRequest(String userSuppliedString,
    						  String returnToUrl,
    						  HttpServlet servlet,
                              HttpServletRequest httpReq,
                              HttpServletResponse httpResp)
            throws IOException, ServletException  {
        try
        {
            // --- Forward proxy setup (only if needed) ---
            // ProxyProperties proxyProps = new ProxyProperties();
            // proxyProps.setProxyName("proxy.example.com");
            // proxyProps.setProxyPort(8080);
            // HttpClientFactory.setProxyProperties(proxyProps);

            // perform discovery on the user-supplied identifier
            @SuppressWarnings("unchecked")
            List<Discovery> discoveries = (List<Discovery>)manager.discover(userSuppliedString);

            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = manager.associate(discoveries);

            // store the discovery information in the user's session
            httpReq.getSession().setAttribute("openid-disc", discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

            // Attribute Exchange example: fetching the 'email' attribute
            FetchRequest fetch = FetchRequest.createFetchRequest();
            // doesn't work unless you replace 'schema.openid.net' with 'axschema.org'
//            fetch.addAttribute("email", "http://schema.openid.net/contact/email", true);
//            fetch.addAttribute("FirstName", "http://schema.openid.net/namePerson/first", true);
//            fetch.addAttribute("LastName", "http://schema.openid.net/namePerson/last", true);
            fetch.addAttribute(AX_EMAIL, "http://axschema.org/contact/email", true);
            fetch.addAttribute(AX_FIRST_NAME, "http://axschema.org/namePerson/first", true);
            fetch.addAttribute(AX_LAST_NAME, "http://axschema.org/namePerson/last", true);

            // attach the extension to the authentication request
            authReq.addExtension(fetch);


            /* if (true  ! discovered.isVersion2() ) */
            {
                // Option 1: GET HTTP-redirect to the OpenID Provider endpoint
                // The only method supported in OpenID 1.x
                // redirect-URL usually limited ~2048 bytes
                httpResp.sendRedirect(authReq.getDestinationUrl(true));
                return null;
            }
//            else
//            {
//                // Option 2: HTML FORM Redirection (Allows payloads >2048 bytes)
//
//                RequestDispatcher dispatcher =
//                        servlet.getServletContext().getRequestDispatcher("formredirection.jsp");
//                httpReq.setAttribute("parameterMap", authReq.getParameterMap());
//                httpReq.setAttribute("destinationUrl", authReq.getDestinationUrl(false));
//                dispatcher.forward(httpReq, httpResp);
//            }
        }
        catch (OpenIDException e)
        {
            throw new RuntimeException(e);
        }
    }

    // --- processing the authentication response ---
    public OpenIDInfo verifyResponse(HttpServletRequest httpReq)
    {
        try
        {
            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response =
                    new ParameterList(httpReq.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation)
                    httpReq.getSession().getAttribute("openid-disc");

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = httpReq.getRequestURL();
            String queryString = httpReq.getQueryString();
            if (queryString != null && queryString.length() > 0)
                receivingURL.append("?").append(httpReq.getQueryString());

            AuthSuccess authSuccess = null;
            boolean success = false;
            OpenIDInfo result = new OpenIDInfo();
            // modification needed to get it working with hosted google apps.  From 
            // http://groups.google.com/group/openid4java/browse_thread/thread/2349e5e3a29f5c5d?pli=1
            if (false) {
                // verify the response
                VerificationResult verification = manager.verify(
                        receivingURL.toString(), response, discovered);
                Identifier verified = verification.getVerifiedId();
                if (verified != null) {
                	success = true;
                    authSuccess = (AuthSuccess) verification.getAuthResponse();
                    result.setIdentifier(verified.getIdentifier());
                }
           } else {
            	authSuccess = AuthSuccess.createAuthSuccess(response);
            	boolean nonceVerified = manager.verifyNonce(authSuccess, discovered);
            	success = nonceVerified;
            	if (success) {
            		result.setIdentifier(httpReq.getParameter("openid.identity"));
            	}
            }
            

            // examine the verification result and extract the verified identifier
           if (success) {

 
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX))
                {
                    FetchResponse fetchResp = (FetchResponse) authSuccess
                            .getExtension(AxMessage.OPENID_NS_AX);

                    @SuppressWarnings("unchecked")
                    Map<String,List<String>>  attributes = (Map<String,List<String>>)fetchResp.getAttributes();
                    result.setMap(attributes);
                }

                return result;
            }
        }
        catch (OpenIDException e)
        {
            throw new RuntimeException(e);
        }

        return null; // not verified
    }
}