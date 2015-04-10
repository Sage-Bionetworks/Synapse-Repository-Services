package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.FILES_CANNOT_HAVE_CHILDREN;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.NOT_LOCATIONABLE;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * Unit test for as much as possible.
 * @author John
 *
 */
public class EntityTypeConverterImplUnitTest {

	NodeDAO mockNodeDao;	
	AuthorizationManager mockAuthorizationManager;
	EntityManager mockEntityManager;
	AmazonS3Client mockS3Client;
	LocationHelper mockLocationHelper;
	EntityTypeConverterImpl typeConverter;
	FileHandleDao mockFileHandleDao;
	Entity entity;
	UserInfo nonAdmin;
	UserInfo admin;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockLocationHelper = Mockito.mock(LocationHelper.class);
		mockFileHandleDao = Mockito.mock(FileHandleDao.class);
		typeConverter = new EntityTypeConverterImpl(mockNodeDao, mockAuthorizationManager, mockEntityManager, mockS3Client, mockLocationHelper, mockFileHandleDao);
		entity = new Study();
		entity.setId("syn123");
		nonAdmin = new UserInfo(false);
		admin = new UserInfo(true);
		when(mockEntityManager.getEntity(nonAdmin, entity.getId())).thenReturn(entity);
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, null));
		when(mockFileHandleDao.createFile(any(S3FileHandle.class))).then(new Answer<S3FileHandle>() {
			@Override
			public S3FileHandle answer(InvocationOnMock invocation)
					throws Throwable {
				return (S3FileHandle) invocation.getArguments()[0];
			}
		});
	}
	
	@Test
	public void testUnauthorized() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(false, "because I said so"));
		// should fail
		LocationableTypeConversionResult result = typeConverter.convertOldTypeToNew(nonAdmin, entity.getId());
		assertFalse(result.getSuccess());
		verify(mockNodeDao, never()).lockNodeAndIncrementEtag(anyString(), anyString(), any(ChangeType.class));
	}
	
	@Test
	public void testAuthorized() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, null));
		// should fail
		LocationableTypeConversionResult result = typeConverter.convertOldTypeToNew(nonAdmin, entity.getId());
		assertTrue(result.getSuccess());
		// entity must be locked, etag checked and a change message setup.
		verify(mockNodeDao).lockNodeAndIncrementEtag(anyString(), anyString(), any(ChangeType.class));
	}
	
	/**
	 * Only locationable entities can be converted.
	 */
	@Test
	public void testNotLocationable() throws UnauthorizedException, DatastoreException, NotFoundException{
		when(mockEntityManager.getEntity(nonAdmin, "1")).thenReturn(new Project());
		when(mockEntityManager.getEntity(nonAdmin, "2")).thenReturn(new TableEntity());
		when(mockEntityManager.getEntity(nonAdmin, "3")).thenReturn(new Folder());
		when(mockEntityManager.getEntity(nonAdmin, "4")).thenReturn(new FileEntity());
		String[] ids = new String[]{"1","2","3","4"};
		for(String id: ids){
			LocationableTypeConversionResult result = typeConverter.convertOldTypeToNew(nonAdmin, id);
			assertFalse(result.getSuccess());
			assertEquals(NOT_LOCATIONABLE.name(), result.getErrorMessage());
		}
	}
	
	/**
	 * Studies can have children because they will be converted to folders.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testStudyHasChildren() throws DatastoreException, NotFoundException{
		// A study can have children.
		Study study = new Study();
		study.setId("syn123");
		when(mockEntityManager.getEntity(nonAdmin, study.getId())).thenReturn(study);
		when(mockNodeDao.getChildrenIds(study.getId())).thenReturn(new HashSet<String>(Arrays.asList("456","789")));
		LocationableTypeConversionResult result = typeConverter.convertOldTypeToNew(nonAdmin, study.getId());
		assertNotNull(result);
		assertEquals("Study should have been converted to a folder",Folder.class.getName(), result.getNewType());
	}
	
	@Ignore
	@Test
	public void testDataHasChildren() throws DatastoreException, NotFoundException{
		Data data = new Data();
		data.setId("syn123");
		when(mockEntityManager.getEntity(nonAdmin, data.getId())).thenReturn(data);
		// Only studies are allowed to have children.
		when(mockNodeDao.getChildrenIds(data.getId())).thenReturn(new HashSet<String>(Arrays.asList("456","789")));
		LocationableTypeConversionResult result = typeConverter.convertOldTypeToNew(nonAdmin, data.getId());
		assertFalse(result.getSuccess());
		assertEquals(FILES_CANNOT_HAVE_CHILDREN.name(), result.getErrorMessage());
	}
	
	@Test
	public void testCreateFileHandleFromPathNotFound(){
		when(mockS3Client.getObjectMetadata(anyString(), anyString())).thenThrow(new AmazonServiceException("Not found"));
		String path = "/some/path/fileName.txt";
		LocationData data = new LocationData();
		data.setPath(path);
		when(mockLocationHelper.getS3KeyFromS3Url(path)).thenReturn(path);
		assertNull(typeConverter.createFileHandleFromPathIfExists(new Study(), data));
	}
	
	@Test
	public void testExtractFileNameFromContentDisposition(){
		assertEquals(null, EntityTypeConverterImpl.extractFileNameFromContentDisposition(null));
		assertEquals(null, EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment"));
		assertEquals(null, EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment;"));
		assertEquals(null, EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment; inline;"));
		assertEquals("genome.jpeg", EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment; filename=genome.jpeg; inline;"));
		assertEquals("genome.jpeg", EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment; filename=genome.jpeg "));
		assertEquals("genome.jpeg", EntityTypeConverterImpl.extractFileNameFromContentDisposition("attachment; filename= genome.jpeg ;"));
	}
	
	@Test
	public void testExtractFileNameFromKey(){
		assertEquals(null, EntityTypeConverterImpl.extractFileNameFromKey(null));
		assertEquals("foo.bar", EntityTypeConverterImpl.extractFileNameFromKey("/123/456/foo.bar"));
		assertEquals("foo.bar", EntityTypeConverterImpl.extractFileNameFromKey("123/456/foo.bar"));
	}
	
	/**
	 * See: PLFM-3228
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreateFileHandleFromRelativePath() throws Exception{
		String key = "some/path/fileName.txt";
		// Add a slash at the front for the path.
		String path = "/"+key;
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(10101);
		metadata.setContentMD5("md5");
		metadata.setContentType("text/plain");
		metadata.setContentDisposition("attachment; filename=foo.txt");
		// Can be found without the slash
		when(mockS3Client.getObjectMetadata(StackConfiguration.getS3Bucket(), key)).thenReturn(metadata);
		// cannot be found with the slash.
		when(mockS3Client.getObjectMetadata(StackConfiguration.getS3Bucket(), path)).thenThrow(new AmazonServiceException("Not found"));
		when(mockLocationHelper.getS3KeyFromS3Url(path)).thenReturn(path);
		LocationData data = new LocationData();
		data.setPath(path);
		// The call
		S3FileHandle handle = typeConverter.createFileHandleFromPathIfExists(new Study(), data);
		assertNotNull(handle);
		assertEquals(StackConfiguration.getS3Bucket(), handle.getBucketName());
		assertEquals(key, handle.getKey());
		assertEquals(metadata.getContentMD5(), handle.getContentMd5());
		assertEquals(""+metadata.getContentLength(), ""+handle.getContentSize());
		assertEquals(metadata.getContentType(), handle.getContentType());
		assertEquals("fileName.txt", handle.getFileName());
	}
	
	/**
	 * See: PLFM-3228
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreateFileHandleFromFullPath() throws Exception{
		String key = "some/path/fileName.txt";
		String path = "https://s3.amazonaws.com/proddata.sagebase.org/"+key+"?Expires=AWS_SIGNATURE";
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(10101);
		// Can be found without the slash
		when(mockS3Client.getObjectMetadata(StackConfiguration.getS3Bucket(), key)).thenReturn(metadata);
		// cannot be found with the slash.
		when(mockS3Client.getObjectMetadata(StackConfiguration.getS3Bucket(), path)).thenThrow(new AmazonServiceException("Not found"));
		when(mockLocationHelper.getS3KeyFromS3Url(path)).thenReturn(key);
		LocationData loc = new LocationData();
		loc.setPath(path);
		// The call
		S3FileHandle handle = typeConverter.createFileHandleFromPathIfExists(new Study(), loc);
		assertNotNull(handle);
	}

}
