package org.sagebionetworks;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ITTestExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ITDrsControllerTest {

    final private SynapseClient synapse;
    private Project project;
    private final List<FileEntity> fileEntities = new ArrayList<>();
    private final List<FileHandle> fileHandles = new ArrayList<>();

    public ITDrsControllerTest(final SynapseClient synapse) {
        this.synapse = synapse;
    }

    @AfterEach
    public void after() throws Exception {
        if (this.project != null) {
            this.synapse.deleteEntity(this.project, true);
        }
        for (final FileHandle handle : this.fileHandles) {
            try {
                this.synapse.deleteFileHandle(handle.getId());
            } catch (final Exception e) {
            }
        }
    }

    @Test
    public void testGetDrsServiceInfo() throws SynapseException {
        final ServiceInformation serviceInformation = this.synapse.getDrsServiceInfo();
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
        createFileEntity(1);
        final FileEntity file = this.fileEntities.get(0);
        final FileHandle fileHandle = this.fileHandles.get(0);
        final String idAndVersion = file.getId() + ".1";
        final DrsObject drsObject = this.synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(getExpectedDrsBlobObject(file, fileHandle), drsObject);
    }

    @Test
    public void testGetDrsObjectBlobWithIncorrectID() {
        final String idAndVersion = "syn123";
        final String errorMessage = "Object id should include version. e.g syn123.1";
        try {
            this.synapse.getDrsObject(idAndVersion);
        } catch (final SynapseException synapseException) {
            assertEquals(errorMessage, synapseException.getMessage());
        }
    }

    @Test
    public void testGetDrsObjectBundle() throws SynapseException {
        createFileEntity(2);
        final Dataset dataset = new Dataset();
        dataset.setParentId(this.project.getId());
        dataset.setVersionComment("1");
        dataset.setName("DrsBundle");
        dataset.setDescription("Human readable text");
        final List<EntityRef> entityRefList = new ArrayList<>();
        this.fileEntities.forEach(fileEntity -> {
            final EntityRef entityRef = new EntityRef();
            entityRef.setEntityId(fileEntity.getId());
            entityRef.setVersionNumber(fileEntity.getVersionNumber());
            entityRefList.add(entityRef);
        });

        dataset.setItems(entityRefList);
        final Dataset createdDataset = this.synapse.createEntity(dataset);
        final String idAndVersion = createdDataset.getId() + ".1";
        final DrsObject drsObject = this.synapse.getDrsObject(idAndVersion);
        assertNotNull(drsObject);
        assertEquals(getExpectedDrsBundleObject(createdDataset), drsObject);
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

    private DrsObject getExpectedDrsBundleObject(final Dataset expectedDataset) {
        final DrsObject drsObject = new DrsObject();
        final String idAndVersion = expectedDataset.getId() + "." + 1;
        drsObject.setId(idAndVersion);
        drsObject.setName(expectedDataset.getName());
        drsObject.setVersion(expectedDataset.getVersionComment());
        drsObject.setCreated_time(expectedDataset.getCreatedOn());
        drsObject.setUpdated_time(expectedDataset.getModifiedOn());
        drsObject.setSelf_uri("drs://repo-prod.prod.sagebase.org/" + idAndVersion);
        final List<Content> contentList = new ArrayList<>();
        expectedDataset.getItems().forEach(entityRef -> {
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

    /**
     * create given number of file and fileHandle under same project.
     *
     * @param numberOfFiles
     * @throws SynapseException
     */
    public void createFileEntity(final int numberOfFiles) throws SynapseException {
        this.project = createProject();
        for (int i = 0; i < numberOfFiles; i++) {
            FileEntity file = new FileEntity();
            final FileHandle fileHandle = uploadFile("file " + i + " contents");
            final String fileHandleId = fileHandle.getId();
            this.fileHandles.add(fileHandle);
            file.setDataFileHandleId(fileHandleId);
            file.setParentId(this.project.getId());
            file = this.synapse.createEntity(file);
            this.fileEntities.add(file);

        }
    }

    public Project createProject() throws SynapseException {
        final Project project = new Project();
        project.setName("DTest.Project");
        return this.synapse.createEntity(project);
    }

    /**
     * Method to upload a file with the given file contents.
     *
     * @param contents
     * @return
     * @throws SynapseException
     */
    CloudProviderFileHandleInterface uploadFile(final String contents) throws SynapseException {
        final byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        final InputStream input = new ByteArrayInputStream(bytes);
        final long fileSize = bytes.length;
        final String fileName = "SomeFileName";
        final String contentType = "text/plain; charset=us-ascii";
        final Boolean generatePreview = false;
        final Boolean forceRestart = false;
        return this.synapse.multipartUpload(input, fileSize, fileName, contentType, null, generatePreview,
                forceRestart);
    }
}
