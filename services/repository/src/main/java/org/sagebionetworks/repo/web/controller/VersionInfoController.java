package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	
/*	private static class Singleton {
		
	}
	
*/	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(
			value=UrlHelpers.VERSIONINFO,
			method=RequestMethod.GET
			)
	public 
	@ResponseBody
//	String getVersionInfo(HttpServletRequest req) throws IOException {
	String getVersionInfo() throws IOException {
/*	Old way of doing this, using .MANIFEST file.
 *  Per discussion with John, have Maven write the versions in .properties files
 *  and get the values from there.
//		URLClassLoader c = (URLClassLoader)this.getClass().getClassLoader();
//		URL url = c.findResource("META-INF/MANIFEST.MF");
//		InputStream s = c.getResourceAsStream("/META-INF/MANIFEST.MF");
//		ServletContext ctxt = req.getSession().getServletContext();
//		InputStream s = ctxt.getResourceAsStream("META-INF/MANIFEST.MF");
*/		
		InputStream s = VersionInfoController.class.getResourceAsStream("/version-info.properties");
		Properties prop = new Properties();
		prop.load(s);
		String p = prop.getProperty("org.sagebionetworks.repository.version");
		
		return p;
	}
	
}
