package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.drs.AccessId;
import org.sagebionetworks.repo.model.drs.AccessIdBuilder;
import org.sagebionetworks.repo.model.drs.AccessMethod;
import org.sagebionetworks.repo.model.drs.AccessMethodType;
import org.sagebionetworks.repo.model.drs.Checksum;
import org.sagebionetworks.repo.model.drs.ChecksumType;
import org.sagebionetworks.repo.model.drs.Content;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.OrganizationInformation;
import org.sagebionetworks.repo.model.drs.PackageInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DrsManagerImpl implements DrsManager {
    public static final String REVERSE_DOMAIN_NOTATION = "org.sagebase.prod.repo-prod";
    public static final String SERVICE_NAME = "Sage Bionetworks Synapse DRS API";
    public static final String CONTACT_URL = "https://sagebionetworks.jira.com/servicedesk/customer/portal/9";
    public static final String DOCUMENTATION_URL = "https://docs.synapse.org";
    public static final String REGISTERED_HOSTNAME = "repo-prod.prod.sagebase.org";
    public static final String HTTPS = "https";
    public static final String DRS = "drs";
    public static final String DRS_GROUP = "org.ga4gh";
    public static final String DRS_ARTIFACT = "drs";
    public static final String DRS_VERSION = "1.2.0";
    public static final String ORGANIZATION_NAME = "Sage Bionetworks";
    public static final String ORGANIZATION_URL = "https://www.sagebionetworks.org";
    public static final String DESCRIPTION = "This service provides implementation of DRS specification for " +
            "accessing FileEntities and Datasets within Synapse.";
    public static final String CHECKSUM_TYPE = "md5";
    public static final String DELIMETER = "_";
    public static final String ILLEGAL_ARGUMENT_ERROR_MESSAGE = "DRS API only supports FileEntity and Datasets.";
    private static final LocalDate localDate = LocalDate.of(2022, 8, 01);
    public static final Date CREATED_AT = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    public static final Date UPDATED_AT = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    private final StackConfiguration stackConfiguration;
    private final FileHandleManager fileHandleManager;
    private final EntityManager entityManager;
    private final UserManager userManager;

    @Autowired
    public DrsManagerImpl(final StackConfiguration stackConfiguration, final FileHandleManager fileHandleManager,
                          final EntityManager entityManager, final UserManager userManager) {
        super();
        this.stackConfiguration = stackConfiguration;
        this.fileHandleManager = fileHandleManager;
        this.entityManager = entityManager;
        this.userManager = userManager;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        final String baseURL = String.format("%s://%s", HTTPS, REGISTERED_HOSTNAME);
        final ServiceInformation result = new ServiceInformation();
        result.setId(REVERSE_DOMAIN_NOTATION);
        result.setName(SERVICE_NAME);
        final PackageInformation drsPackageInformation = new PackageInformation();
        drsPackageInformation.setGroup(DRS_GROUP);
        drsPackageInformation.setArtifact(DRS_ARTIFACT);
        drsPackageInformation.setVersion(DRS_VERSION);
        result.setType(drsPackageInformation);
        result.setDescription(DESCRIPTION);
        final OrganizationInformation organization = new OrganizationInformation();
        organization.setName(ORGANIZATION_NAME);
        organization.setUrl(ORGANIZATION_URL);
        result.setOrganization(organization);
        result.setContactUrl(CONTACT_URL);
        result.setDocumentationUrl(DOCUMENTATION_URL);
        result.setCreatedAt(CREATED_AT);
        result.setUpdatedAt(UPDATED_AT);
        result.setEnvironment(stackConfiguration.getStack());
        result.setVersion(stackConfiguration.getStackInstance());
        result.setUrl(baseURL);
        return result;
    }

    @Override
    public DrsObject getDrsObject(final Long userId, final String id, final Boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException, UnsupportedOperationException {
        final UserInfo userInfo = userManager.getUserInfo(userId);
        final DrsObject result = new DrsObject();
        final IdAndVersion idAndVersion = IdAndVersion.parse(id);

        final Entity entity = entityManager.getEntityForVersion(userInfo, idAndVersion.getId().toString(),
                getVersion(idAndVersion), null);

        result.setId(id);
        result.setName(entity.getName());
        final String drsURI = DRS + "://" + REGISTERED_HOSTNAME + "/";
        result.setSelf_uri(drsURI + id);
        result.setCreated_time(entity.getCreatedOn());
        result.setUpdated_time(entity.getModifiedOn());
        result.setDescription(entity.getDescription());

        if (entity instanceof FileEntity) {
            final FileEntity file = (FileEntity) entity;
            // Drs user are allowed to see metadata so access check is not required and
            // moreover the metadata provided is not sensitive like s3 url etc.
            final FileHandle fileHandle = fileHandleManager.getRawFileHandleUnchecked(file.getDataFileHandleId());
            result.setSize(fileHandle.getContentSize());
            result.setVersion(file.getVersionNumber() == null ? null : file.getVersionNumber().toString());
            result.setMime_type(fileHandle.getContentType());
            final List<Checksum> checksums = new ArrayList();
            final Checksum checksum = new Checksum();
            checksum.setChecksum(fileHandle.getContentMd5());
            checksum.setType(ChecksumType.md5);
            checksums.add(checksum);
            result.setChecksums(checksums);
            final List<AccessMethod> accessMethods = new ArrayList<>();
            final AccessId accessId = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                    .setSynapseIdWithVersion(id).setFileHandleId(file.getDataFileHandleId()).build();
            final AccessMethod accessMethod = new AccessMethod();
            accessMethod.setType(AccessMethodType.https);
            accessMethod.setAccess_id(accessId.toString());
            accessMethods.add(accessMethod);
            result.setAccess_methods(accessMethods);
        } else if (entity instanceof Dataset) {
            if (expand) {
                throw new IllegalArgumentException("Nesting of bundle is not supported.");
            }

            final Dataset dataset = (Dataset) entity;
            result.setVersion(dataset.getVersionLabel());
            List<Content> contentList = new ArrayList<>();
            dataset.getItems().forEach(entityRef -> {
                final Content content = new Content();
                final String fileIdWithVersion = entityRef.getEntityId() + "." + entityRef.getVersionNumber();
                content.setId(fileIdWithVersion);
                // Name of file should be unique in the bundle, So file id with version is used as name.
                content.setName(fileIdWithVersion);
                content.setDrs_uri(drsURI + fileIdWithVersion);
                contentList.add(content);
            });
            result.setContents(contentList);
            //TODO checksum and size of dataset should be filed once added in meta data of dataset
        } else {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_ERROR_MESSAGE);
        }
        return result;
    }

    private Long getVersion(final IdAndVersion idAndVersion) throws IllegalArgumentException {
        return idAndVersion.getVersion().orElseThrow(() -> new IllegalArgumentException("Object id should include version. e.g syn123.1"));
    }
}
