package org.sagebionetworks;


import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ITTestExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ITDrsControllerTest {

    final private SynapseClient synapse;
    private CloudProviderFileHandleInterface fileHandle;
    private Project project;
    private Folder folder;
    private FileEntity file;
    private File sampleFile;
    private FileHandleAssociation association;
    private SynapseAdminClient adminSynapse;
    private List<String> fileHandlesToDelete = Lists.newArrayList();

    public ITDrsControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse = adminSynapse;
        this.synapse = synapse;
    }

    @BeforeEach
    public void before() throws SynapseException, IOException {
        adminSynapse.clearAllLocks();
        // Create a project, this will own the file entity
        project = new Project();
        project = synapse.createEntity(project);

        // create a folder
        folder = new Folder();
        folder.setName("someFolder");
        folder.setParentId(project.getId());
        folder = this.synapse.createEntity(folder);

        // Get the image file from the classpath.
        final URL url = IT054FileEntityTest.class.getClassLoader().getResource("SmallTextFiles/TinyFile.txt");
        sampleFile = new File(url.getFile().replaceAll("%20", " "));
        assertNotNull(sampleFile);
        assertTrue(sampleFile.exists());

        fileHandle = synapse.multipartUpload(sampleFile, null, true, true);
        fileHandlesToDelete.add(fileHandle.getId());

        // Add a file to the folder
        file = new FileEntity();
        file.setName("someFile");
        file.setParentId(folder.getId());
        file.setDataFileHandleId(fileHandle.getId());
        file.setDescription("Test file");
        file = this.synapse.createEntity(file);

        // Association for this file.
        association = new FileHandleAssociation();
        association.setAssociateObjectId(file.getId());
        association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
        association.setFileHandleId(file.getDataFileHandleId());

        synapse.clearDownloadList();
    }

    @AfterEach
    public void after() throws Exception {
        if (project != null) {
            synapse.deleteEntity(project, true);
        }
        for (String handle : fileHandlesToDelete) {
            try {
                synapse.deleteFileHandle(handle);
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testGETDrsServiceInfo() throws SynapseException {
        final ServiceInformation serviceInformation = synapse.getDrsServiceInfo();
        assertNotNull(serviceInformation);
        assertEquals(serviceInformation.getId(), "org.sagebase.prod.repo-prod");
        assertEquals(serviceInformation.getName(), "Sage Bionetworks Synapse DRS API");
        assertEquals(serviceInformation.getUrl(), "https://repo-prod.prod.sagebase.org");
        assertEquals(serviceInformation.getDocumentationUrl(), "https://docs.synapse.org");
        assertEquals(serviceInformation.getContactUrl(), "https://sagebionetworks.jira.com/servicedesk/customer/portal/9");
        assertEquals(serviceInformation.getOrganization().getName(), "Sage Bionetworks");
        assertEquals(serviceInformation.getOrganization().getUrl(), "https://www.sagebionetworks.org");
        assertEquals(serviceInformation.getType().getGroup(), "org.ga4gh");
        assertEquals(serviceInformation.getType().getArtifact(), "drs");
        assertEquals(serviceInformation.getType().getVersion(), "1.2.0");
    }

    @Test
    public void testGETDrsObjectBlob() throws SynapseException {
        final String idAndVersion = file.getId() + ".1";
        final DrsObject drsObject = synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), idAndVersion);
        assertEquals(drsObject.getName(), file.getName());
        assertEquals(drsObject.getVersion(), 1L);
        assertEquals(drsObject.getSize(), fileHandle.getContentSize());
        assertEquals(drsObject.getMime_type(), fileHandle.getContentType());
        assertEquals(drsObject.getChecksums().size(), 1);
        assertEquals(drsObject.getChecksums().get(0).getChecksum(), fileHandle.getContentMd5());
        assertEquals(drsObject.getChecksums().get(0).getType(), "md5");
        assertEquals(drsObject.getAccess_methods().size(), 1);
        assertEquals(drsObject.getAccess_methods().get(0).getAccess_id(), FileHandleAssociateType.FileEntity.name()
                + "_" + idAndVersion + "_" + file.getDataFileHandleId());
        assertEquals(drsObject.getSelf_uri(), "drs://repo-prod.prod.sagebase.org/" + idAndVersion);
        assertEquals(drsObject.getDescription(), file.getDescription());
    }

    @Test
    public void testGETDrsObjectBlobWithIncorrectID() {
        final String idAndVersion = file.getId();
        final String errorMessage = String.format("Drs object id %s does not exists", idAndVersion);
        try {
            synapse.getDrsObject(idAndVersion);
        } catch (SynapseException synapseException) {
            assertEquals(404, synapseException.getHttpStatusCode());
            assertEquals(errorMessage, synapseException.getMessage());
        }
    }
}
