package org.sagebionetworks.file.manager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The results of a file upload.
 * 
 * @author John
 *
 */
public class FileUploadResults {
	
	private Map<String, String> parameters = new HashMap<String, String>();
	private List<FileData> files = new LinkedList<FileData>();
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	public List<FileData> getFiles() {
		return files;
	}
	public void setFiles(List<FileData> files) {
		this.files = files;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((files == null) ? 0 : files.hashCode());
		result = prime * result
				+ ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileUploadResults other = (FileUploadResults) obj;
		if (files == null) {
			if (other.files != null)
				return false;
		} else if (!files.equals(other.files))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FileUploadResults [parameters=" + parameters + ", files="
				+ files + "]";
	}

}
