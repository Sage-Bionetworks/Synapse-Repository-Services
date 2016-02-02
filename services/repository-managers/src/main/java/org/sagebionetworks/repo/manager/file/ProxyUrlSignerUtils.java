package org.sagebionetworks.repo.manager.file;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.url.HttpMethod;
import org.sagebionetworks.url.UrlData;
import org.sagebionetworks.url.UrlSignerUtils;

public class ProxyUrlSignerUtils {

	/**
	 * Generate a pre-signed URL for a given filehandle.
	 * @param proxyHandle
	 * @param proxyStorage
	 * @param expires
	 * @return
	 */
	public static String generatePresignedUrl(ProxyFileHandle proxyHandle,
			ProxyStorageLocationSettings proxyStorage, Date expires) {
		// Generate the
		try {
			StringBuilder path = new StringBuilder();
			path.append("/");
			path.append(proxyStorage.getUploadType().name().toLowerCase());
			path.append("/");
			String proxyPath = proxyHandle.getFilePath().trim();
			if(proxyPath.startsWith("/")){
				path.append(proxyPath.substring(1));
			}else{
				path.append(proxyPath);
			}
			// Build a URL using all of the data from the fileHandle
			String unsignedUrl = new URL("https", proxyStorage.getProxyHost(), -1, path.toString()).toString();
			UrlData unsignedData = new UrlData(unsignedUrl);
			if(proxyHandle.getFileName() != null){
				unsignedData.getQueryParameters().put("fileName", proxyHandle.getFileName());
			}
			if(proxyHandle.getContentType() != null) {
				unsignedData.getQueryParameters().put("contentType", proxyHandle.getContentType());
			}
			if(proxyHandle.getContentMd5() != null){
				unsignedData.getQueryParameters().put("contentMD5", proxyHandle.getContentMd5());
			}
			if(proxyHandle.getContentSize() != null){
				unsignedData.getQueryParameters().put("contentSize", ""+proxyHandle.getContentSize());
			}
			unsignedUrl = unsignedData.toURL().toString();
			
			URL signedUrl = UrlSignerUtils.generatePreSignedURL(HttpMethod.GET, unsignedUrl, expires, proxyStorage.getSecretKey());
			return signedUrl.toString();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
