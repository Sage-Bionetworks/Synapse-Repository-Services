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
import org.sagebionetworks.repo.model.EntityRef;
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
import org.sagebionetworks.repo.model.table.Dataset;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DrsManagerImplUnitTest {

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

    private static final String ENTITY_ID = "syn1";
    private static final long ENTITY_VERSION = 1L;
    private static final String ENTITY_NAME = "Test File";
    private static final String ENTITY_DESCRIPTION = "Drs Test File";
    private static final String DATA_FILE_HANDLE_ID = "123456";
    private static final String FILE_CHECKSUM = "HexOfMd5";
    private static final Long USER_ID = 1L;

    @Test
    public void testGetDrsServiceInformation() {
        when(stackConfiguration.getStack()).thenReturn("dev");
        when(stackConfiguration.getStackInstance()).thenReturn("417.0.1");
        final ServiceInformation serviceInformation = drsManager.getServiceInformation();
        verify(stackConfiguration).getStack();
        verify(stackConfiguration).getStackInstance();
        assertNotNull(serviceInformation);
        assertEquals(createExpectedServiceInformation(), serviceInformation);
    }

    @Test
    public void testGetBlobDrsObject() {
        final FileEntity file = getFileEntity();
        final FileHandle fileHandle = getFileHandle();
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(file);
        when(fileHandleManager.getRawFileHandleUnchecked(any())).thenReturn(fileHandle);
        when(userManager.getUserInfo(any())).thenReturn(userInfo);

        final DrsObject drsObject = drsManager.getDrsObject(USER_ID, file.getId() + "." + ENTITY_VERSION, false);
        verify(entityManager).getEntityForVersion(userInfo, "1", ENTITY_VERSION, null);
        verify(fileHandleManager).getRawFileHandleUnchecked(file.getDataFileHandleId());
        verify(userManager).getUserInfo(USER_ID);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), file.getId() + "." + ENTITY_VERSION);
    }

    @Test
    public void testGetBundleDrsObject() {
        final Dataset dataset = getDataset();
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(dataset);
        when(userManager.getUserInfo(any())).thenReturn(userInfo);

        final DrsObject drsObject = drsManager.getDrsObject(USER_ID, dataset.getId() + "." + ENTITY_VERSION, false);
        verify(entityManager).getEntityForVersion(userInfo, "1", ENTITY_VERSION, null);
        verify(userManager).getUserInfo(USER_ID);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), dataset.getId() + "." + ENTITY_VERSION);
    }

    @Test
    public void testGetBundleDrsObjectWithExpand() {
        final Dataset dataset = getDataset();
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(dataset);
        when(userManager.getUserInfo(any())).thenReturn(userInfo);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            drsManager.getDrsObject(USER_ID, dataset.getId() + "." + ENTITY_VERSION, true);
            verify(entityManager).getEntityForVersion(userInfo, "1", ENTITY_VERSION, null);
            verify(userManager).getUserInfo(USER_ID);
        });
        assertEquals("Nesting of bundle is not supported.", exception.getMessage());
    }

    @Test
    public void testGetBlobDrsObjectWithInvalidID() {
        final String id = "syn1";
        final String expectedErrorMessage = "Object id should include version. e.g syn123.1";
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            drsManager.getDrsObject(USER_ID, id, false);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void testGetInvalidTypeOfDrsObject() {
        final Project project = getProject("syn1.1", "project");
        when(entityManager.getEntityForVersion(any(), any(), any(), any())).thenReturn(project);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            drsManager.getDrsObject(USER_ID, project.getId(), false);
            verify(entityManager).getEntityForVersion(userInfo, "1", ENTITY_VERSION, null);
        });
        assertEquals("DRS API only supports FileEntity and Datasets.", exception.getMessage());
    }

    private FileEntity getFileEntity() {
        final FileEntity file = new FileEntity();
        file.setId(ENTITY_ID);
        file.setName(ENTITY_NAME);
        file.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        file.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        file.setDescription(ENTITY_DESCRIPTION);
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

    private Project getProject(final String id, final String name) {
        final Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("test");
        project.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        project.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        return project;
    }

    private Dataset getDataset() {
        final Dataset dataset = new Dataset();
        dataset.setId(ENTITY_ID);
        dataset.setName(ENTITY_NAME);
        dataset.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dataset.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dataset.setDescription(ENTITY_DESCRIPTION);
        final List<EntityRef> entityRefList = new ArrayList<>();
        final EntityRef entityRef = new EntityRef();
        entityRef.setEntityId(ENTITY_ID);
        entityRef.setVersionNumber(1L);
        entityRefList.add(entityRef);
        dataset.setItems(entityRefList);
        return dataset;
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
