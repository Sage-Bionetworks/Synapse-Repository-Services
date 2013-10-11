package org.sagebionetworks.authutil;

/*
 * Copyright 2006-2007 Sxip Identity Corporation
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StringEncrypter;
import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Modified "Relying Party" implementation
 * Taken from: http://code.google.com/p/openid4java/wiki/QuickStart
 */
public class BasicOpenIDConsumer {

	public static final String AX_EMAIL = "Email";
	public static final String AX_FIRST_NAME = "FirstName";
	public static final String AX_LAST_NAME = "LastName";

	public static final String DISCOVERY_INFO_COOKIE_NAME = "org.sagebionetworks.auth.discoveryInfoCookie";
	public static final int DISCOVERY_INFO_COOKIE_MAX_AGE = 60; // sec

	private static final String encryptionKey = StackConfiguration.getEncryptionKey();

	/**
	 * Serializes, encrypts and Base-64 encodes an object, so that it can be
	 * safely put in a cookie. Note: Encryption/decryption doesn't seem to work
	 * on the binary serialized object directly, so we Base64 encode it one
	 * extra time before encrypting. For small objects this doesn't add a
	 * performance burden.
	 */
	public static <T> String encryptingSerializer(T o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(o);
		oos.close();
		byte[] serializedAndBase64Encoded = Base64.encodeBase64(out
				.toByteArray());
		StringEncrypter se = new StringEncrypter(encryptionKey);
		String encrypted = se.encrypt(new String(serializedAndBase64Encoded));
		return encrypted;
	}

	/**
	 * Decrypts and deserializes an object. See 'encryptingSerializer' for
	 * details.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decryptingDeserializer(String s) throws IOException {
		String encryptedDI = s;
		StringEncrypter se = new StringEncrypter(encryptionKey);
		String serializedAndBase64EncodedDI = se.decrypt(encryptedDI);
		byte[] serializedByteArray = Base64
				.decodeBase64(serializedAndBase64EncodedDI.getBytes());
		ByteArrayInputStream bais = new ByteArrayInputStream(
				serializedByteArray);
		ObjectInputStream ois = new ObjectInputStream(bais);
		try {
			return (T) ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Places an OpenID request
	 */
	public static void authRequest(String userSuppliedString, String returnToUrl,
			HttpServlet servlet, HttpServletRequest httpReq,
			HttpServletResponse httpResp) throws IOException, ServletException {
		ConsumerManager manager = new ConsumerManager();
		
		try {
			// --- Forward proxy setup (only if needed) ---
			// ProxyProperties proxyProps = new ProxyProperties();
			// proxyProps.setProxyName("proxy.example.com");
			// proxyProps.setProxyPort(8080);
			// HttpClientFactory.setProxyProperties(proxyProps);

			// perform discovery on the user-supplied identifier
			@SuppressWarnings("unchecked")
			List<Discovery> discoveries = (List<Discovery>) manager
					.discover(userSuppliedString);

			// attempt to associate with the OpenID provider
			// and retrieve one service endpoint for authentication
			DiscoveryInformation discovered = manager.associate(discoveries);

			// write it to a cookie
			String encryptedDI = encryptingSerializer(discovered);
			Cookie cookie = new Cookie(DISCOVERY_INFO_COOKIE_NAME, encryptedDI);
			cookie.setMaxAge(DISCOVERY_INFO_COOKIE_MAX_AGE);
			httpResp.addCookie(cookie);

			// store the discovery information in the user's session
			httpReq.getSession().setAttribute("openid-disc", discovered);

			// obtain a AuthRequest message to be sent to the OpenID provider
			AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

			// Attribute Exchange example: fetching the 'email' attribute
			FetchRequest fetch = FetchRequest.createFetchRequest();
			// doesn't work unless you replace 'schema.openid.net' with 'axschema.org'
			// fetch.addAttribute("email", "http://schema.openid.net/contact/email", true);
			// fetch.addAttribute("FirstName", "http://schema.openid.net/namePerson/first", true);
			// fetch.addAttribute("LastName", "http://schema.openid.net/namePerson/last", true);
			fetch.addAttribute(AX_EMAIL, "http://axschema.org/contact/email",
					true);
			fetch.addAttribute(AX_FIRST_NAME,
					"http://axschema.org/namePerson/first", true);
			fetch.addAttribute(AX_LAST_NAME,
					"http://axschema.org/namePerson/last", true);

			// attach the extension to the authentication request
			authReq.addExtension(fetch);

			// Option 1: GET HTTP-redirect to the OpenID Provider endpoint
			// The only method supported in OpenID 1.x
			// redirect-URL usually limited ~2048 bytes
			httpResp.sendRedirect(authReq.getDestinationUrl(true));

		} catch (OpenIDException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches Open ID information from the request
	 * @throws UnauthorizedException If the request is invalid
	 */
	public static OpenIDInfo verifyResponse(HttpServletRequest httpReq)
			throws IOException, UnauthorizedException {
		ConsumerManager manager = new ConsumerManager();
		
		// Extract the parameters from the authentication response
		// (which comes in as a HTTP request from the OpenID provider)
		ParameterList response = new ParameterList(
				httpReq.getParameterMap());

		AuthSuccess authSuccess = null;
		boolean success = false;
		OpenIDInfo result = new OpenIDInfo();
		
		//TODO Modification is needed to get it working with hosted google apps
		// See: http://groups.google.com/group/openid4java/browse_thread/thread/2349e5e3a29f5c5d?pli=1

		Cookie[] cookies = httpReq.getCookies();
		DiscoveryInformation discovered = null;
		for (Cookie c : cookies) {
			if (DISCOVERY_INFO_COOKIE_NAME.equals(c.getName())) {
				discovered = decryptingDeserializer(c.getValue());
				break;
			}
		}
		if (discovered == null) {
			throw new RuntimeException(
					"OpenID authentication failure: Missing required discovery information.");
		}
		try {
			authSuccess = AuthSuccess.createAuthSuccess(response);
			boolean nonceVerified = manager.verifyNonce(authSuccess, discovered);
			success = nonceVerified;
			if (success) {
				result.setIdentifier(httpReq.getParameter("openid.identity"));
			}

			// Examine the verification result and extract the verified identifier
			if (success) {
				if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
					FetchResponse fetchResp = (FetchResponse) authSuccess
							.getExtension(AxMessage.OPENID_NS_AX);

					@SuppressWarnings("unchecked")
					Map<String, List<String>> attributes = (Map<String, List<String>>) fetchResp
							.getAttributes();
					result.setMap(attributes);
				}

				return result;
			}
		} catch (OpenIDException e) {
			throw new UnauthorizedException(e);
		}
		
		// not verified
		return null;
	}
}