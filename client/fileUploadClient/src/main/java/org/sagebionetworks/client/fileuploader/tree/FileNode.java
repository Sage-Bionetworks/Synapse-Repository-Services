package org.sagebionetworks.client.fileuploader.tree;

import java.io.File;

public class FileNode extends TreeNode {

	private File file;
	
	public FileNode(File file) {
		super();
		this.file = file;
	}

	public FileNode(String parentId, File file) {
		super();
		setParentId(parentId);
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileNode other = (FileNode) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
	
	
}
