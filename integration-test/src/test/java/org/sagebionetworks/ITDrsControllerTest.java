package org.sagebionetworks;


import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.Util.FileEntityUtil;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.drs.AccessMethod;
import org.sagebionetworks.repo.model.drs.AccessMethodType;
import org.sagebionetworks.repo.model.drs.Checksum;
import org.sagebionetworks.repo.model.drs.ChecksumType;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ITTestExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ITDrsControllerTest {

    final private SynapseClient synapse;
    private CloudProviderFileHandleInterface fileHandle;
    private Project project;
    private Folder folder;
    private FileEntity file;
    private SynapseAdminClient adminSynapse;

    private FileEntityUtil fileEntityUtil;
    private List<String> fileHandlesToDelete = Lists.newArrayList();

    public ITDrsControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse = adminSynapse;
        this.synapse = synapse;
        this.fileEntityUtil = new FileEntityUtil(adminSynapse, synapse);
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
    public void testGETDrsObjectBlob() throws SynapseException, IOException {
        createProjectHierarchy();
        final String idAndVersion = file.getId() + ".1";
        final DrsObject drsObject = synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(getExpectedDrsObject(idAndVersion), drsObject);
    }

    @Test
    public void testGETDrsObjectBlobWithIncorrectID() throws SynapseException, IOException {
        createProjectHierarchy();
        final String idAndVersion = file.getId();
        final String errorMessage = "Object id should include version. e.g syn123.1";
        try {
            synapse.getDrsObject(idAndVersion);
        } catch (SynapseException synapseException) {
            assertEquals(errorMessage, synapseException.getMessage());
        }
    }

    private void createProjectHierarchy() throws SynapseException, IOException {
        fileEntityUtil.createProjectHierarchy();
        project = fileEntityUtil.project;

        folder = fileEntityUtil.folder;

        fileHandle = fileEntityUtil.fileHandle;
        fileHandlesToDelete.add(fileHandle.getId());

        file = fileEntityUtil.file;

        synapse.clearDownloadList();
    }

    private DrsObject getExpectedDrsObject(final String idAndVersion) {
        final DrsObject drsObject = new DrsObject();
        drsObject.setId(idAndVersion);
        drsObject.setName(file.getName());
        drsObject.setVersion("1");
        drsObject.setSize(fileHandle.getContentSize());
        drsObject.setMime_type(fileHandle.getContentType());
        drsObject.setCreated_time(file.getCreatedOn());
        drsObject.setUpdated_time(file.getModifiedOn());
        final List<Checksum> checksums = new ArrayList<>();
        final Checksum checksum = new Checksum();
        checksum.setChecksum(fileHandle.getContentMd5());
        checksum.setType(ChecksumType.md5);
        checksums.add(checksum);
        drsObject.setChecksums(checksums);
        final List<AccessMethod> accessMethods = new ArrayList<>();
        final AccessMethod accessMethod = new AccessMethod();
        accessMethod.setType(AccessMethodType.https);
        accessMethod.setAccess_id(FileHandleAssociateType.FileEntity.name() + "_" +
                idAndVersion + "_" + file.getDataFileHandleId());
        accessMethods.add(accessMethod);
        drsObject.setAccess_methods(accessMethods);
        drsObject.setSelf_uri("drs://repo-prod.prod.sagebase.org/" + idAndVersion);
        drsObject.setDescription(file.getDescription());
        return drsObject;
    }
}
