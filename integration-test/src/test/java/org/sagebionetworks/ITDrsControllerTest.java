package org.sagebionetworks;


import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.drs.AccessMethod;
import org.sagebionetworks.repo.model.drs.AccessMethodType;
import org.sagebionetworks.repo.model.drs.Checksum;
import org.sagebionetworks.repo.model.drs.ChecksumType;
import org.sagebionetworks.repo.model.drs.Content;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.util.FileDataCreationUtil;
import org.sagebionetworks.util.FileDataHierarchy;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ITTestExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ITDrsControllerTest {

    final private SynapseClient synapse;
    private Project project;
    private SynapseAdminClient adminSynapse;
    private FileDataCreationUtil fileEntityUtil;
    private List<String> fileHandlesToDelete = Lists.newArrayList();

    public ITDrsControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse = adminSynapse;
        this.synapse = synapse;
        this.fileEntityUtil = new FileDataCreationUtil(adminSynapse, synapse);
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
    public void testGetDrsServiceInfo() throws SynapseException {
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
    public void testGetDrsObjectBlob() throws SynapseException {
        final List<FileDataHierarchy> fileDataHierarchies = fileEntityUtil.createFileEntity(1);
        project = fileDataHierarchies.get(0).getProject();
        final FileEntity file = fileDataHierarchies.get(0).getFile();
        final FileHandle fileHandle = fileDataHierarchies.get(0).getFileHandle();
        fileHandlesToDelete.add(fileHandle.getId());
        final String idAndVersion = file.getId() + ".1";
        final DrsObject drsObject = synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(getExpectedDrsBlobObject(file, fileHandle), drsObject);
    }

    @Test
    public void testGetDrsObjectBlobWithIncorrectID() {
        final String idAndVersion = "syn123";
        final String errorMessage = "Object id should include version. e.g syn123.1";
        try {
            synapse.getDrsObject(idAndVersion);
        } catch (SynapseException synapseException) {
            assertEquals(errorMessage, synapseException.getMessage());
        }
    }

    @Test
    public void testGetDrsObjectBundle() throws SynapseException {
        final List<FileDataHierarchy> fileDataHierarchyList = fileEntityUtil.createFileEntity(2);
        project = fileDataHierarchyList.get(0).getProject();
        final Dataset dataset = new Dataset();
        dataset.setParentId(project.getId());
        dataset.setVersionComment("1");
        dataset.setName("DrsBundle");
        dataset.setDescription("Human readable text");
        final List<EntityRef> entityRefList = new ArrayList<>();
        fileDataHierarchyList.forEach(dataHierarchy -> {
            final EntityRef entityRef = new EntityRef();
            entityRef.setEntityId(dataHierarchy.getFile().getId());
            entityRef.setVersionNumber(dataHierarchy.getFile().getVersionNumber());
            entityRefList.add(entityRef);
            fileHandlesToDelete.add(dataHierarchy.getFileHandle().getId());
        });

        dataset.setItems(entityRefList);
        final Dataset createdDataset = synapse.createEntity(dataset);
        final String idAndVersion = createdDataset.getId() + ".1";
        final DrsObject drsObject = synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(getExpectedDrsBundleObject(createdDataset, entityRefList), drsObject);
    }


    private DrsObject getExpectedDrsBlobObject(final FileEntity expectedFile, final FileHandle fileHandle) {
        final DrsObject drsObject = new DrsObject();
        final String idAndVersion = expectedFile.getId() + ".1";
        drsObject.setId(expectedFile.getId() + "." + "1");
        drsObject.setName(expectedFile.getName());
        drsObject.setVersion("1");
        drsObject.setSize(fileHandle.getContentSize());
        drsObject.setMime_type(fileHandle.getContentType());
        drsObject.setCreated_time(expectedFile.getCreatedOn());
        drsObject.setUpdated_time(expectedFile.getModifiedOn());
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
                idAndVersion + "_" + expectedFile.getDataFileHandleId());
        accessMethods.add(accessMethod);
        drsObject.setAccess_methods(accessMethods);
        drsObject.setSelf_uri("drs://repo-prod.prod.sagebase.org/" + idAndVersion);
        drsObject.setDescription(expectedFile.getDescription());
        return drsObject;
    }

    private DrsObject getExpectedDrsBundleObject(final Dataset expectedDataset, final List<EntityRef> entityRefList) {
        final DrsObject drsObject = new DrsObject();
        final String idAndVersion = expectedDataset.getId() + "." + 1;
        drsObject.setId(idAndVersion);
        drsObject.setName(expectedDataset.getName());
        drsObject.setVersion(expectedDataset.getVersionComment());
        drsObject.setCreated_time(expectedDataset.getCreatedOn());
        drsObject.setUpdated_time(expectedDataset.getModifiedOn());
        drsObject.setSelf_uri("drs://repo-prod.prod.sagebase.org/" + idAndVersion);
        final List<Content> contentList = new ArrayList<>();
        entityRefList.forEach(entityRef -> {
            final Content content = new Content();
            final String fileIdAndVersion = entityRef.getEntityId() + "." + entityRef.getVersionNumber();
            content.setId(fileIdAndVersion);
            content.setName(fileIdAndVersion);
            content.setDrs_uri("drs://repo-prod.prod.sagebase.org/" + fileIdAndVersion);
            contentList.add(content);
        });
        drsObject.setContents(contentList);
        drsObject.setDescription(expectedDataset.getDescription());
        return drsObject;
    }
}
