package org.sagebionetworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A helper class to load properties from the users .m2 settings file.
 * 
 * @author jmhill
 *
 */
public class SettingsLoader {
	
	private static final Logger log = LogManager.getLogger(SettingsLoader.class
			.getName());
	
	public static Properties loadSettingsFile() throws IOException, JDOMException{
		// Get the user home
		String userHome = System.getProperty("user.home");
		File settingsFile = new File(userHome+File.separator+".m2"+File.separator+"settings.xml");
		if(!settingsFile.exists()){
			log.info("Cannot find the Maven Settings file: "+settingsFile.getAbsolutePath());
			return null;
		}else{
			log.info("Loading Maven settings file: "+settingsFile.getAbsolutePath());
		}
		FileInputStream in = new FileInputStream(settingsFile);
		try{
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(false);
			builder.setIgnoringElementContentWhitespace(true);
			Document doc = builder.build(settingsFile);
			Element root = doc.getRootElement();
			Properties props = new Properties();
			findProperties(root, props);
			return props;
		}finally{
			in.close();
		}
	}
	
	/**
	 * Recursive function to find the properties in the settings file.
	 * @param current
	 * @param props
	 */
	public static void findProperties(Element current, Properties props) {
		List children = current.getChildren();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			Element child = (Element) iterator.next();
			if("properties".equals(child.getName())){
				List propList = child.getChildren();
				Iterator propIt = propList.iterator();
				while(propIt.hasNext()){
					Element prop = (Element) propIt.next();
					props.setProperty(prop.getName(), prop.getValue());
					if(log.isTraceEnabled()){
						log.trace(prop.getName()+"="+prop.getValue());
					}
				}
			}else{
				findProperties(child, props);
			}
		}
	}

}
