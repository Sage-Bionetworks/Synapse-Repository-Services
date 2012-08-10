package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sagebionetworks.repo.model.versionInfo.VersionInfo;
import org.sagebionetworks.repo.web.UrlHelpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author xschildw
 */
@Controller
public class VersionInfoController extends BaseController {
	
	private static class Holder {
		private static String versionInfo = "";
		
		static {
			InputStream s = VersionInfoController.class.getResourceAsStream("/version-info.properties");
			Properties prop = new Properties();
			try {
				prop.load(s);
			} catch (IOException e) {
				throw new RuntimeException("version-info.properties file not found", e);
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
//	String getVersionInfo(HttpServletRequest req) throws IOException {
	VersionInfo getVersionInfo() throws RuntimeException {
		VersionInfo vInfo = new VersionInfo();
		vInfo.setVersion(Holder.getVersionInfo());
		return vInfo;
	}
	
}
