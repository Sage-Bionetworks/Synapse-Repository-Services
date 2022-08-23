package org.sagebionetworks.util;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileEntityUtil {

    public Project project;
    public Folder folder;
    public FileEntity file;
    public CloudProviderFileHandleInterface fileHandle;
    public SynapseAdminClient adminSynapse;
    public SynapseClient synapse;
    public File sampleFile;

    public FileEntityUtil(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse =adminSynapse;
        this.synapse = synapse;
    }

    public void createProjectHierarchy() throws SynapseException, IOException {
        adminSynapse.clearAllLocks();

        // Create a project, this will own the file entity

        project = new Project();
        project = synapse.createEntity(project);

        // create a folder
        folder = new Folder();
        folder.setName("someFolder");
        folder.setParentId(project.getId());
        folder = this.synapse.createEntity(folder);
        // create fileHandle
        fileHandle = createFileHandle();
        // Add a file to the folder
        file = new FileEntity();
        file.setName("someFile");
        file.setParentId(folder.getId());
        file.setDescription("Test file");
        file.setDataFileHandleId(fileHandle.getId());
        file = this.synapse.createEntity(file);
    }

    public CloudProviderFileHandleInterface createFileHandle() throws SynapseException, IOException {
        final URL url = FileEntityUtil.class.getClassLoader().getResource("SmallTextFiles/TinyFile.txt");
        sampleFile = new File(url.getFile().replaceAll("%20", " "));
        assertNotNull(sampleFile);
        assertTrue(sampleFile.exists());

        return synapse.multipartUpload(sampleFile, null, true, true);
    }
}
