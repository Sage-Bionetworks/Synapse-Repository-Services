package org.sagebionetworks.util;

import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandle;

public class FileDataHierarchy {
    private Project project;
    private FileEntity file;
    private FileHandle fileHandle;

    public FileDataHierarchy(Project project, FileEntity file, FileHandle fileHandle) {
        this.project = project;
        this.file = file;
        this.fileHandle = fileHandle;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public FileEntity getFile() {
        return file;
    }

    public void setFile(FileEntity file) {
        this.file = file;
    }

    public FileHandle getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(FileHandle fileHandle) {
        this.fileHandle = fileHandle;
    }
}