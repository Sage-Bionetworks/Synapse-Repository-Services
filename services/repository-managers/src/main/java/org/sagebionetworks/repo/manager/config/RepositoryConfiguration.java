package org.sagebionetworks.repo.manager.config;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.sagebionetworks.evaluation.dbo.SubmissionFileHandleDBO;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.RowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.tables.TableFileHandleScanner;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBORequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.dbo.form.DBOFormData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RepositoryConfiguration {
	
	private static final String VELOCITY_RESOURCE_LOADERS = "classpath,file";
	private static final String VELOCITY_PARAM_CLASSPATH_LOADER_CLASS = "classpath.resource.loader.class";
	private static final String VELOCITY_PARAM_FILE_LOADER_CLASS = "file.resource.loader.class";
	private static final String VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT = "runtime.references.strict";
	
	/**
	 * @return The velocity engine instance that can be used within the managers
	 */
	@Bean
	public VelocityEngine velocityEngine() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, VELOCITY_RESOURCE_LOADERS); 
		engine.setProperty(VELOCITY_PARAM_CLASSPATH_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_FILE_LOADER_CLASS, FileResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT, true);
		return engine;
	}

	/**
	 * 
	 * @return A general purpose JSON object mapper configured to not fail on unkonwn properties and with the Java time module enabled
	 */
	@Bean
	public ObjectMapper jsonObjectMapper() {
		return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());
	}
	
	@Bean
	public Map<FileHandleAssociateType, FileHandleAssociationProvider> fileHandleAssociationProviderMap(List<FileHandleAssociationProvider> providers) {
		 return providers.stream().collect(Collectors.toMap(p -> p.getAssociateType(), Function.identity()));
	}
	
	@Bean
	public Map<FileHandleAssociateType, FileHandleAssociationScanner> fileHandleAssociationScannerMap(NamedParameterJdbcTemplate jdbcTemplate, TableEntityManager tableEntityManager) {
		Map<FileHandleAssociateType, FileHandleAssociationScanner> scannerMap = new HashMap<>();
		
		scannerMap.put(FileHandleAssociateType.TableEntity, tableEntityFileScanner(tableEntityManager, jdbcTemplate));
		
		scannerMap.put(FileHandleAssociateType.FileEntity, fileEntityFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.SubmissionAttachment, evaluationSubmissionFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.FormData, formFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.MessageAttachment, messageFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.TeamAttachment, teamFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.UserProfileAttachment, userProfileFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.VerificationSubmission, verificationFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.WikiMarkdown, wikiMarkdownFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.WikiAttachment, wikiAttachmentFileScanner(jdbcTemplate));
		
		scannerMap.put(FileHandleAssociateType.AccessRequirementAttachment, accessRequirementFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.DataAccessRequestAttachment, accessRequestFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.DataAccessSubmissionAttachment, accessSubmissionFileScanner(jdbcTemplate));
		
		return scannerMap;
	}
	
	@Bean
	public FileHandleAssociationScanner tableEntityFileScanner(TableEntityManager tableEntityManager, NamedParameterJdbcTemplate jdbcTemplate) {
		// Note: for configuration consistency this bean is not annotated with the @Service annotation (e.g. will not be auto-scanned) but we
		// configure it here as a public bean
		return new TableFileHandleScanner(tableEntityManager, jdbcTemplate);
	}
	
	@Bean
	public FileHandleAssociationScanner fileEntityFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORevision().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner formFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOFormData().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner messageFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOMessageContent().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner teamFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOTeam().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner userProfileFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOUserProfile().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner verificationFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOVerificationSubmissionFile().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner wikiMarkdownFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		// Note: the wiki might also contain attachments, those are stored in the serialized field of the wiki but also in a dedicated table
		// that is actually scanned with the scanner provided by the dedicated wikiAttachmentFileScanner
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new V2DBOWikiMarkdown().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner wikiAttachmentFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		// Note: This table contains all the attachments of a wiki plus the wiki id itself
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new V2DBOWikiAttachmentReservation().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner evaluationSubmissionFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new SubmissionFileHandleDBO().getTableMapping());
	}
	
	@Bean
	public FileHandleAssociationScanner accessRequirementFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(AccessRequirementUtils::readSerializedField, AccessRequirementUtils::extractAllFileHandleIds);
		
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOAccessRequirementRevision().getTableMapping(), DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}
	
	@Bean
	public FileHandleAssociationScanner accessRequestFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(RequestUtils::readSerializedField, RequestUtils::extractAllFileHandleIds);
		
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORequest().getTableMapping(), DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}
	
	@Bean
	public FileHandleAssociationScanner accessSubmissionFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(SubmissionUtils::readSerializedField, SubmissionUtils::extractAllFileHandleIds);
		
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOSubmission().getTableMapping(), DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}
	
}
