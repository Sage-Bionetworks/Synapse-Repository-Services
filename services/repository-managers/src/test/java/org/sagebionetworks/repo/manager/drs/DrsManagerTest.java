package org.sagebionetworks.repo.manager.drs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.OrganizationInformation;
import org.sagebionetworks.repo.model.drs.PackageInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DrsManagerTest {

    @InjectMocks
    DrsManagerImpl drsManager;
    @Mock
    StackConfiguration stackConfiguration;
    @Mock
    EntityManager entityManager;
    @Mock
    FileHandleManager fileHandleManager;
    @Mock
    UserInfo userInfo;
    @Mock
    UserManager userManager;

    private static final String FILE_ID = "syn1.1";
    private static final String FILE_NAME = "Test File";
    private static final String FILE_DESCRIPTION = "Drs Test File";
    private static final String DATA_FILE_HANDLE_ID = "123456";
    private static final String FILE_CHECKSUM = "HexOfMd5";
    private static final Long USER_ID = 1L;

    @Test
    public void testGETDrsServiceInformation() {
        when(stackConfiguration.getStack()).thenReturn("dev");
        when(stackConfiguration.getStackInstance()).thenReturn("417.0.1");
        final ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
        assertEquals(createExpectedServiceInformation(), serviceInformation);
    }

    @Test
    public void testGETBlobDrsObject() {
        final FileEntity file = getFileEntity();
        final FileHandle fileHandle = getFileHandle();
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(file);
        when(fileHandleManager.getRawFileHandle(any(), any())).thenReturn(fileHandle);
        when(userManager.getUserInfo(any())).thenReturn(userInfo);
        final DrsObject drsObject = drsManager.getDrsObject(USER_ID, file.getId());
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), file.getId());
        assertEquals(drsObject.getName(), file.getName());
        assertEquals(drsObject.getDescription(), file.getDescription());
        assertEquals(drsObject.getMime_type(), fileHandle.getContentType());
        assertEquals(drsObject.getChecksums().get(0).getChecksum(), fileHandle.getContentMd5());
        assertEquals(drsObject.getAccess_methods().get(0).getAccess_id(), FileHandleAssociateType.FileEntity.name()
              + "_" + file.getId() + "_"+ file.getDataFileHandleId());
    }

    @Test
    public void testGETBlobDrsObjectWithInvalidID() {
        final String id = "syn1";
        final String expectedErrorMessage = String.format("Object id should include version. e.g syn123.1", id);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            drsManager.getDrsObject(USER_ID, id);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void testGETInvalidDrsObject() {
        final Project project = getProject("syn1.1","project");
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(project);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            drsManager.getDrsObject(USER_ID, project.getId());
        });
        assertEquals("DRS API only supports FileEntity and Datasets.", exception.getMessage());
    }

    private FileEntity getFileEntity() {
        final FileEntity file = new FileEntity();
        file.setId(FILE_ID);
        file.setName(FILE_NAME);
        file.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        file.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        file.setDescription(FILE_DESCRIPTION);
        file.setDataFileHandleId(DATA_FILE_HANDLE_ID);
        return file;
    }

    private FileHandle getFileHandle() {
        final FileHandle fileHandle = new S3FileHandle();
        fileHandle.setConcreteType(FileHandleAssociateType.FileEntity.name());
        fileHandle.setContentSize(8000L);
        fileHandle.setContentMd5(FILE_CHECKSUM);
        return fileHandle;
    }

    private Project getProject(final String id, final String name){
        final Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("test");
        project.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        project.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        return project;
    }

    private ServiceInformation createExpectedServiceInformation() {
        final ServiceInformation serviceInformation = new ServiceInformation();
        final String baseURL = String.format("%s://%s", DrsManagerImpl.HTTPS, DrsManagerImpl.REGISTERED_HOSTNAME);
        serviceInformation.setId(DrsManagerImpl.REVERSE_DOMAIN_NOTATION);
        serviceInformation.setName(DrsManagerImpl.SERVICE_NAME);
        final PackageInformation drsPackageInformation = new PackageInformation();
        drsPackageInformation.setGroup(DrsManagerImpl.DRS_GROUP);
        drsPackageInformation.setArtifact(DrsManagerImpl.DRS_ARTIFACT);
        drsPackageInformation.setVersion(DrsManagerImpl.DRS_VERSION);
        serviceInformation.setType(drsPackageInformation);
        serviceInformation.setDescription(DrsManagerImpl.DESCRIPTION);
        final OrganizationInformation organization = new OrganizationInformation();
        organization.setName(DrsManagerImpl.ORGANIZATION_NAME);
        organization.setUrl(DrsManagerImpl.ORGANIZATION_URL);
        serviceInformation.setOrganization(organization);
        serviceInformation.setContactUrl(DrsManagerImpl.CONTACT_URL);
        serviceInformation.setDocumentationUrl(DrsManagerImpl.DOCUMENTATION_URL);
        serviceInformation.setCreatedAt(DrsManagerImpl.CREATED_AT);
        serviceInformation.setUpdatedAt(DrsManagerImpl.UPDATED_AT);
        serviceInformation.setEnvironment("dev");
        serviceInformation.setVersion("417.0.1");
        serviceInformation.setUrl(baseURL);
        return serviceInformation;
    }

}
