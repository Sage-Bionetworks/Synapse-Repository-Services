package org.sagebionetworks.util;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileDataCreationUtil {

    public Project project;
    public Folder folder;
    public CloudProviderFileHandleInterface fileHandle;
    public SynapseAdminClient adminSynapse;
    public SynapseClient synapse;

    public FileDataCreationUtil(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse = adminSynapse;
        this.synapse = synapse;
    }

    /**
     * Method to upload a file with the given file contents.
     *
     * @param contents
     * @return
     * @throws SynapseException
     */
    CloudProviderFileHandleInterface uploadFile(String contents) throws SynapseException {
        final byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        final InputStream input = new ByteArrayInputStream(bytes);
        long fileSize = bytes.length;
        final String fileName = "SomeFileName";
        final String contentType = "text/plain; charset=us-ascii";
        final Boolean generatePreview = false;
        final Boolean forceRestart = false;
        return synapse.multipartUpload(input, fileSize, fileName, contentType, null, generatePreview,
                forceRestart);
    }

    /**
     * create given number of file and fileHandle under same project.
     *
     * @param numberOfFiles
     * @return List<FileDataHierarchy>
     * @throws SynapseException
     */
    public List<FileDataHierarchy> createFileEntity(int numberOfFiles) throws SynapseException {
        project = createProject();
        final List<FileDataHierarchy> fileDataHierarchies = new ArrayList<>();
        for (int i = 0; i < numberOfFiles; i++) {
            FileEntity file = new FileEntity();
            final FileHandle fileHandle = uploadFile("file " + i + " contents");
            String fileHandleId = fileHandle.getId();
            file.setDataFileHandleId(fileHandleId);
            file.setParentId(project.getId());
            file = synapse.createEntity(file);

            final FileDataHierarchy fileWithFileHandle = new FileDataHierarchy(project, file, fileHandle);
            fileDataHierarchies.add(fileWithFileHandle);
        }
        return fileDataHierarchies;
    }

    public Project createProject() throws SynapseException {
        final Project project = new Project();
        project.setName("Test.Project");
        return synapse.createEntity(project);
    }
}

