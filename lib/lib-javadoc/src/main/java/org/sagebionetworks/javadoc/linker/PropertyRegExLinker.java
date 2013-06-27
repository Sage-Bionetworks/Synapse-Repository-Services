package org.sagebionetworks.javadoc.linker;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.javadoc.BasicFileUtils;

/**
 * This linker replaces all symbolic links that have a java property style regular expression.
 * For example, given the following symbolic link: '${org.sage.prop.one}', this linker will 
 * replace this regular expression with the file that has been assigned the name "ore.sage.prop.one"
 * 
 * @author jmhill
 *
 */
public class PropertyRegExLinker implements Linker {

	@Override
	public void link(File baseDir, List<FileLink> toLink) throws Exception {
		// Now load each file and replace the all symbolic links with actual links
		for(FileLink link: toLink){
			// Build the path from this file to the base
			// First map each name to the file
			Map<String, String> replacements = buildReplacement(baseDir, link.file, toLink);
			String fileString = FileUtils.readFileToString(link.getFile());
			// Use the property replacement util
			fileString = PropertyReplacement.replaceProperties(fileString, replacements);
			// Write it back to the file
			FileUtils.writeStringToFile(link.getFile(), fileString);
		}
	}

	/**
	 * Build up a replacement map
	 * @param toLink
	 * @return
	 */
	static Map<String, String> buildReplacement(File baseDir, File thisFile, List<FileLink> toLink) {
		// first calculate the path of this file to the base directory
		String pathToRoot = BasicFileUtils.pathToRoot(baseDir, thisFile);
		Map<String, String> replacements = new HashMap<String, String>();
		String baseDirPath = replacePath(baseDir.getAbsolutePath());
		for(FileLink link: toLink){
			String path = replacePath(link.getFile().getAbsolutePath());
			String subPath = path.substring(baseDirPath.length()+1, path.length());
			replacements.put(link.getName(), pathToRoot+subPath);
		}
		return replacements;
	}
	
	/**
	 * Replace all back slashes with forward slashes.
	 * @param path
	 * @return
	 */
	public static String replacePath(String path){
		return path.replaceAll("\\\\","/");
	}

}
