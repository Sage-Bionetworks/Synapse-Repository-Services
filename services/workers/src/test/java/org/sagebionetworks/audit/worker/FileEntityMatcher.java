package org.sagebionetworks.audit.worker;

import java.util.regex.Pattern;

public class FileEntityMatcher {
	
	Long entityId;
	Pattern pattern;
	
	public FileEntityMatcher(Long entityId){
		this.entityId = entityId;
		pattern = Pattern.compile("/entity/(syn)?"+entityId+"(/version/(\\d)+)?/file(preview)?");
	}
	
	public boolean matches(String toTest){
		return pattern.matcher(toTest).find();
	}

	public static void main(String[] args){
		long id = 123456;
		FileEntityMatcher matcher = new FileEntityMatcher(id);
		String[] toTest = new String[]{
				"/entity/syn"+id+"/file",
				"/entity/"+id+"/file",
				"/entity/"+id+"/filepreview",
				"/entity/"+id+"/version/2/file",
				"/entity/syn"+id+"/version/3/file",
				"/entity/syn"+id+"/version/3/filepreview",
				"/entity/syn"+id+"2/file",
				"/entity/"+id+"2/file",
				"/entity/"+id+"2/filepreview",
				"/entity/"+id+"2/version/2/file",
				"/entity/syn"+id+"2/version/3/file",
				"/entity/syn"+id+"2/version/3/filepreview",
		};
		
		for(String test: toTest){
			System.out.println("'"+test+"' matches: "+matcher.matches(test));
		}
	}
}
