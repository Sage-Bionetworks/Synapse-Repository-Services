package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.FILES_CANNOT_HAVE_CHILDREN;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.LOCATIONABLE_HAS_MORE_THAN_ONE_LOCATION;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.NOT_LOCATIONABLE;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.SOME_VERSIONS_HAVE_FILES_OTHERS_DO_NOT;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityGroup;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Summary;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class EntityTypeConverterImpl implements EntityTypeConverter {
	
	
	/**
	 * See PLFM-3248
	 */
	public static String[] AnnotationKeysToRemove = new String[]{
		"uri",
		"annotations",
		"accessControlList",
		"concreteType",
		"attachments",
		"locations",
		"md5",
		"contentType",
		"s3Token",
		"versionNumber",
		"versionLabel",
		"versionComment",
		"versionUrl",
		"versions",
		"groups"
	};
	
	static private Logger log = LogManager.getLogger(EntityTypeConverterImpl.class);

	private static final String FILENAME = "filename=";
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	NodeDAO nodeDao;	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	LocationHelper locationHelper;
	@Autowired
	UserManager userManager;
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	V2WikiManager wikiManager;
	
	
	/**
	 * For spring
	 */
	public EntityTypeConverterImpl(){};
	
	/**
	 * For tests
	 * @param nodeDao
	 * @param authorizationManager
	 */
	public EntityTypeConverterImpl(NodeDAO nodeDao,
			AuthorizationManager authorizationManager, EntityManager entityManager,AmazonS3Client s3Client, LocationHelper locationHelper, FileHandleDao fileHandleDao) {
		super();
		this.nodeDao = nodeDao;
		this.authorizationManager = authorizationManager;
		this.entityManager = entityManager;
		this.s3Client = s3Client;
		this.locationHelper = locationHelper;
		this.fileHandleDao = fileHandleDao;
	}



	@WriteTransaction
	@Override
	public LocationableTypeConversionResult convertOldTypeToNew(UserInfo user, String entityId) throws UnauthorizedException, DatastoreException, NotFoundException {
		LocationableTypeConversionResult results = new LocationableTypeConversionResult();
		results.setEntityId(entityId);
		try {
			// Lookup the entity
			Entity entity = entityManager.getEntity(user, entityId);
			results.setCreatedBy(entity.getCreatedBy());
			results.setOriginalType(entity.getClass().getName());
			
			if(!(entity instanceof Locationable) && !(entity instanceof Summary)){
				NOT_LOCATIONABLE.throwException();
			}
			UserInfo.validateUserInfo(user);
			// Must have update permission on the entity.
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(user, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));

			// First get the version for the entity.
			String newEtag = nodeDao.lockNodeAndIncrementEtag(entity.getId(), entity.getEtag(), ChangeType.UPDATE);
			
			String resultType = null;
			if(entity instanceof Locationable){
				resultType = convertLocationable(user, entity, newEtag);
			}else if(entity instanceof Summary){
				resultType = convertSummary(user, entity, newEtag);
			}else{
				NOT_LOCATIONABLE.throwException();
			}
			results.setNewType(resultType);
			results.setSuccess(true);
			return results;
		} catch (Exception e) {
			log.error("Failed on: "+entityId, e);
			results.setSuccess(false);
			results.setErrorMessage(e.getMessage());
			return results;
		}
	}

	private String convertSummary(UserInfo user, Entity entity, String newEtag) throws UnsupportedEncodingException, IOException, NotFoundException {
		Summary summary = (Summary) entity;
		// First create a wiki for this entity
		String description = entity.getDescription();
		if(description == null){
			description = "";
		}
		String summaryMarkdown = SummaryMarkdownUtils.generateSummaryMarkdown(summary);
		V2WikiPage wiki = createWiki(summary, description+summaryMarkdown);
		
		return convertToFolder(user, entity, newEtag, new LinkedList<VersionData>());
	}
	


	public String convertLocationable(UserInfo user, Entity entity,
			String newEtag) throws NotFoundException,
			UnsupportedEncodingException, IOException {
		Locationable locationable = (Locationable) entity;
		// Create a wiki if there is a description or file attachments
		createWikiIfNeeded(locationable);
		// Try to create files handles for each version of the entity.
		List<VersionData> pairs = createFileHandleForForEachVersion(user, locationable);
		
		// Studies and objects with no file data are converted to folders, while all other types are converted to files.
		String resultType = null;
		if(entity instanceof Study || pairs.isEmpty()){
			resultType = convertToFolder(user, locationable, newEtag, pairs);
		}else{
			resultType = convertToFile(user, locationable, newEtag, pairs);
		}
		return resultType;
	}

	private V2WikiPage createWikiIfNeeded(Entity entity) throws NumberFormatException, NotFoundException, UnsupportedEncodingException, IOException {
		// we need a wiki if there is a description or attachments
		if(entity.getDescription() != null || entity.getAttachments() != null){
			String markDown = entity.getDescription();
			if(markDown == null){
				markDown = "See attachments:";
			}
			return createWiki(entity, markDown);
		}
		return null;
	}

	public V2WikiPage createWiki(Entity entity, String markDown)
			throws UnsupportedEncodingException, IOException, NotFoundException {
		V2WikiPage page = new V2WikiPage();
		// Create the file handle for the markdown
		S3FileHandle markDownHandle = fileHandleManager.createCompressedFileFromString(entity.getCreatedBy(), entity.getModifiedOn(), markDown);
		page.setMarkdownFileHandleId(markDownHandle.getId());
		if(entity.getAttachments() != null){
			// create a file handle for each attachment.
			for(AttachmentData ad: entity.getAttachments()){
				S3FileHandle attachHandle = fileHandleManager.createFileHandleFromAttachmentIfExists(entity.getId(), entity.getCreatedBy(),
						entity.getModifiedOn(), ad);
				if (attachHandle == null) {
					// If the original attachment does not exist create a placeholder.
					attachHandle = fileHandleManager.createNeverUploadedPlaceHolderFileHandle(entity.getCreatedBy(), entity.getModifiedOn(), ad.getName());
				}
				if(page.getAttachmentFileHandleIds() == null){
					page.setAttachmentFileHandleIds(new LinkedList<String>());
				}
				page.getAttachmentFileHandleIds().add(attachHandle.getId());
			}
		}
		UserInfo creator = getCreatorAsAdmin(entity.getCreatedBy());
		// Create the wiki page with all attachments.
		return wikiManager.createWikiPage(creator, entity.getId(), ObjectType.ENTITY, page);
	}

	/**
	 * Convert to a File.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	private String convertToFile(UserInfo user, Locationable entity, String newEtag, List<VersionData> pairs) throws DatastoreException, UnauthorizedException, NotFoundException {
		// We can only convert to files if this entity has no children.
		if(!nodeDao.getChildrenIds(entity.getId()).isEmpty()){
			FILES_CANNOT_HAVE_CHILDREN.throwException();
		}
		for(VersionData pair: pairs){
			Long verionNumber = pair.getVersionNumber();
			String fileHandleId = pair.getFileHandle().getId();
			NamedAnnotations newAnnos = convertAnnotations(entity, verionNumber);
			// replace this version
			nodeDao.replaceVersion(entity.getId(), verionNumber, newAnnos, fileHandleId);
		}
		// the last step is to change the type
		nodeDao.changeNodeType(entity.getId(), newEtag, "file");
		// get the changed entity.
		return FileEntity.class.getName();
	}

	/**
	 * Convert to a Folder.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	private String convertToFolder(UserInfo user, Entity entity, String newEtag, List<VersionData> pairs) throws DatastoreException, UnauthorizedException, NotFoundException {
		// The latest version is fist but we need to process these in the original order.
		FileEntity child = new FileEntity();
		child.setParentId(entity.getId());
		child.setName(entity.getName());
		Collections.reverse(pairs);
		for(VersionData pair: pairs){
			String fileHandleId = pair.getFileHandle().getId();
			// Get the user that modified this version.
			UserInfo modifiedUser =  getCreatorAsAdmin(pair.getModifiedBy());
			//  create the child
			child.setDataFileHandleId(fileHandleId);
			child.setVersionComment(pair.getVersionComments());
			child.setVersionLabel(pair.getVersionLabel());
			if(child.getId() == null){
				// Create the child
				String id = entityManager.createEntity(modifiedUser, child, null);
				child = entityManager.getEntity(user, id, FileEntity.class);
			}else{
				// update the child
				entityManager.updateEntity(modifiedUser, child, true, null);
				child = entityManager.getEntity(user,child.getId(), FileEntity.class);
			}
		}
		// Replace the annotations for each version
		List<Long> versions = nodeDao.getVersionNumbers(entity.getId());
		if(versions != null){
			for(Long versionNumber: versions){
				NamedAnnotations newAnnos = convertAnnotations(entity, versionNumber);
				// replace this version
				nodeDao.replaceVersion(entity.getId(), versionNumber, newAnnos, null);
			}
		}
		// the last step is to change the type
		nodeDao.changeNodeType(entity.getId(), newEtag, "folder");
		// Return the entity in its new form.s
		return Folder.class.getName();
	}

	public NamedAnnotations convertAnnotations(Entity entity,
			Long verionNumber) throws NotFoundException {
		// Re-write this version
		NamedAnnotations annos = nodeDao.getAnnotationsForVersion(entity.getId(), verionNumber);
		NamedAnnotations newAnnos = new NamedAnnotations();
		// Copy the additional annos first
		newAnnos.getAdditionalAnnotations().addAll(annos.getAdditionalAnnotations());
		Annotations oldPrimary = annos.getPrimaryAnnotations();
		// Override with any of the primary annotations.
		// But first remove all entity field names
		for(String key: AnnotationKeysToRemove){
			oldPrimary.deleteAnnotation(key);
		}
		newAnnos.getAdditionalAnnotations().addAll(oldPrimary);
		return newAnnos;
	}
	
	/**
	 * Create a user that can act as an admin.
	 * @param userId
	 * @return
	 * @throws NumberFormatException
	 * @throws NotFoundException
	 */
	private UserInfo getCreatorAsAdmin(String userId) throws NumberFormatException, NotFoundException{
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(userId));
		return createAdminUserInfoCopy(userInfo);
	}

	/**
	 * Create an Admin copy of a user.
	 * @param userInfo
	 * @return
	 */
	private static UserInfo createAdminUserInfoCopy(UserInfo userInfo) {
		UserInfo asAdmin = new UserInfo(true, userInfo.getId());
		asAdmin.setGroups(userInfo.getGroups());
		// Also make the user certified
		asAdmin.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		return asAdmin;
	}

	/**
	 * Create a FileHandle for each version.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	@WriteTransaction
	@Override
	public List<VersionData> createFileHandleForForEachVersion(UserInfo user, Entity entity) throws DatastoreException,
			NotFoundException, UnsupportedEncodingException, IOException {
		// We need to create file handle for each version.
		List<VersionData> pairs = new LinkedList<VersionData>();
		List<Long> versions = nodeDao.getVersionNumbers(entity.getId());
		if(versions != null){
			// Gather file handles for each version
			for(Long versionNumber: versions){
				Locationable location = (Locationable) entityManager.getEntityForVersion(user, entity.getId(), versionNumber, entity.getClass());
				if(location.getLocations() == null || location.getLocations().isEmpty()){
					continue;
				}
				if(location.getLocations().size() > 1){
					LOCATIONABLE_HAS_MORE_THAN_ONE_LOCATION.throwException();
				}
				LocationData data = location.getLocations().get(0);
				FileHandle fileHandle = null;
				if(LocationTypeNames.awss3.equals(data.getType())){
					// S3 file handle.
					fileHandle = createFileHandleFromPathIfExists(location, data);
					if (fileHandle == null) {
						fileHandle = fileHandleManager.createNeverUploadedPlaceHolderFileHandle(location.getModifiedBy(), location.getModifiedOn(), location.getName());
					}
				}else{
					// external file handle
					ExternalFileHandle efh = new ExternalFileHandle();
					efh.setContentType(location.getContentType());
					efh.setExternalURL(data.getPath());
					efh.setFileName(location.getName());
					setCreatedOnAndBy(location, efh);
					fileHandle = fileHandleDao.createFile(efh);
				}
				pairs.add(new VersionData(versionNumber, fileHandle, location.getModifiedBy(), location.getVersionLabel(), location.getVersionComment()));
			}
			// The number of pairs must match the number of versions
			if(pairs.size() > 0 && pairs.size() != versions.size()){
				SOME_VERSIONS_HAVE_FILES_OTHERS_DO_NOT.throwException();
			}
		}
		return pairs;
	}

	/**
	 * 
	 * CreatedOn needs to match the when the version was modified. CreatedBy
	 * need to match the user that created the version.
	 * 
	 * @param location
	 * @param efh
	 */
	public void setCreatedOnAndBy(Locationable location, FileHandle efh) {
		efh.setCreatedBy(location.getModifiedBy());
		efh.setCreatedOn(location.getModifiedOn());
	}

	@Override
	public S3FileHandle createFileHandleFromPathIfExists(Locationable location, LocationData data) throws NotFoundException {
		String key = locationHelper.getS3KeyFromS3Url(data.getPath());
		// The keys do not start with "/"
		if(key.startsWith("/")){
			key = key.substring(1);
		}
		String bucket = StackConfiguration.getS3Bucket();
		// Can we find this object with the key?
		try {
			ObjectMetadata meta = s3Client.getObjectMetadata(bucket, key);
			S3FileHandle handle = new S3FileHandle();
			handle.setBucketName(bucket);
			handle.setKey(key);
			handle.setContentType(meta.getContentType());
			handle.setContentMd5(meta.getContentMD5());
			handle.setContentSize(meta.getContentLength());
			handle.setFileName(extractFileNameFromKey(data.getPath()));
			
			// Override contentType and md5 when it is null.
			if(location.getContentType() != null){
				handle.setContentType(location.getContentType());
			}
			if(location.getMd5() != null){
				handle.setContentMd5(location.getMd5());
			}
			if(handle.getFileName() == null){
				handle.setFileName(location.getName());
			}
			setCreatedOnAndBy(location, handle);
			return fileHandleDao.createFile(handle);
		} catch (AmazonServiceException e) {
			if (AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode()) || AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode())) {
				return null;
			} else {
				log.error("Unknown S3 error, handling as not found: " + e.getMessage(), e);
				return null;
			}
		}
	}
	
	/**
	 * Extract the file name from the keys
	 * @param key
	 * @return
	 */
	public static String extractFileNameFromKey(String key){
		if(key == null){
			return null;
		}
		String[] slash = key.split("/");
		if(slash.length > 0){
			return slash[slash.length-1];
		}
		return null;
	}
	
	/**
	 * Attempt to extract the filename from the contentDisposition
	 * @param contentDisposition
	 * @return
	 */
	public static String extractFileNameFromContentDisposition(String contentDisposition){
		if(contentDisposition == null){
			return null;
		}
		String[] semiSplit = contentDisposition.split(";");
		for(String semi: semiSplit){
			if(semi.contains(FILENAME)){
				return semi.substring(FILENAME.length()+1).trim();
			}
		}
		return null;
	}
	
}
