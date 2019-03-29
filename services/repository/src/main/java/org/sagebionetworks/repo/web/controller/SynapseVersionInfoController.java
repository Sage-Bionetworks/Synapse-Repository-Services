package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author xschildw
 */
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SynapseVersionInfoController {
	
	private static class Holder {
		private static String versionInfo = "";
		
		static {
			InputStream s = SynapseVersionInfoController.class.getResourceAsStream("/version-info.properties");
			Properties prop = new Properties();
			try {
				prop.load(s);
			} catch (IOException e) {
				throw new RuntimeException("version-info.properties file not found", e);
			} finally {
				IOUtils.closeQuietly(s);
			}
			versionInfo = prop.getProperty("org.sagebionetworks.repository.version");
		}
		
		private static String getVersionInfo() {
			return versionInfo;
		}
				
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(
			value=UrlHelpers.VERSIONINFO,
			method=RequestMethod.GET
			)
	public 
	@ResponseBody
	SynapseVersionInfo getVersionInfo(HttpServletRequest request) throws RuntimeException {
		SynapseVersionInfo vInfo = new SynapseVersionInfo();
		vInfo.setVersion(Holder.getVersionInfo());
		return vInfo;
	}
	
}
