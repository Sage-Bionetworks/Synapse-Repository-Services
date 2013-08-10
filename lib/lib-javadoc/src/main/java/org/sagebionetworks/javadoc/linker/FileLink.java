package org.sagebionetworks.javadoc.linker;

import java.io.File;

/**
 * Links a file name to an actual file.
 * 
 * @author jmhill
 *
 */
public class FileLink {

	File file;
	String name;
	boolean isHashTagId = false;
	
	public FileLink(File file, String name) {
		super();
		this.file = file;
		this.name = name;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isHashTagId() {
		return isHashTagId;
	}
	public void setHashTagId(boolean isHashTagId) {
		this.isHashTagId = isHashTagId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + (isHashTagId ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		FileLink other = (FileLink) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (isHashTagId != other.isHashTagId)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FileLink [file=" + file + ", name=" + name + ", isHashTagId="
				+ isHashTagId + "]";
	}

}
