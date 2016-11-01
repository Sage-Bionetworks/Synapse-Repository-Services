package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServerException;

/**
 * This is a collection of web client utilities NOT specific to accessing the Synapse 
 * REST APIs. These methods complement those in SharedClientConnection.
 * 
 * @author bhoff
 *
 */
public class HttpClientHelper {
	private HttpClientProvider clientProvider;

	public HttpClientHelper(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}
	
	public String getDataDirect(String endpoint, String uri) throws SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		HttpResponse response = null;
		try {
			response = clientProvider.performRequest(endpoint + uri, "GET", null, null);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode!=HttpStatus.SC_OK) throw new SynapseServerException(statusCode);
			return EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new SynapseClientException(e);
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}
	
	/**
	 * Put the contents of the passed file to the passed URL.
	 * 
	 * @param url
	 * @param file
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public String putFileToURL(URL url, File file, String contentType) throws SynapseException {
		HttpResponse response = null;
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (file == null)
				throw new IllegalArgumentException("File cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			// There must not be any headers added or Amazon will return a 403.
			// Therefore, we must clear the content type.
			@SuppressWarnings("deprecation")
			org.apache.http.entity.FileEntity fe = new org.apache.http.entity.FileEntity(file, contentType);
			httppost.setEntity(fe);
			response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseServerException(code, "Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url + " File: " + file.getName());
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}

	}
	
	/**
	 * Put the contents of the passed byte array to the passed URL, associating the given content type
	 * 
	 * @param url
	 * @param content the byte array to upload
	 * @throws SynapseException
	 */
	public String putBytesToURL(URL url, byte[] content, String contentType) throws SynapseException {
		HttpResponse response = null;
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (content == null)
				throw new IllegalArgumentException("content cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			ByteArrayEntity se = new ByteArrayEntity(content);
			httppost.setHeader("content-type", contentType);
			httppost.setEntity(se);
			response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseServerException(code, "Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url);
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}

	}
	
}
