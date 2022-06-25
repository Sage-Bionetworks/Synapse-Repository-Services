package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.util.MimeTypeUtils;

@ExtendWith(MockitoExtension.class)
public class TemplatedMessageSenderImplTest {

	@Mock
	private VelocityEngine mockVelocityEngine;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private Template mockTemplate;

	@InjectMocks
	private TemplatedMessageSenderImpl templatedMessageSender;

	private String templateFile;
	private UserInfo sender;
	private Map<String, Object> context;
	private Set<String> recipients;
	private String subject;
	private S3FileHandle fileHandle;
	private Charset charset;
	private String mimeType = MimeTypeUtils.TEXT_HTML_VALUE;

	@BeforeEach
	public void before() {
		templateFile = "file/some.vt";
		sender = new UserInfo(false, 123L);
		context = Map.of("one", 1L, "two", 2L);
		recipients = Set.of("33", "44");
		subject = "The Subject";
		charset = StandardCharsets.UTF_8;
		mimeType = MimeTypeUtils.TEXT_HTML_VALUE;
		
		fileHandle = new S3FileHandle().setId("55");
	}

	@Test
	public void testSendMessage() throws UnsupportedEncodingException, IOException {
		when(mockVelocityEngine.getTemplate(any(), any())).thenReturn(mockTemplate);
		when(mockFileHandleManager.createCompressedFileFromString(any(), any(), any(), any())).thenReturn(fileHandle);
		String messageBody = "some message body";
		setupMockMergeTemplate(messageBody);

		// call under test
		templatedMessageSender.sendMessage(createTemplate());

		MessageToUser expectedMtU = new MessageToUser().setFileHandleId(fileHandle.getId()).setSubject(templateFile)
				.setSubject(subject).setRecipients(recipients).setFileHandleId(fileHandle.getId())
				.setWithProfileSettingLink(true).setWithUnsubscribeLink(true).setIsNotificationMessage(true);
		boolean overrideNotificationSettings = false;
		verify(mockMessageManager).createMessage(sender, expectedMtU, overrideNotificationSettings);

		verify(mockVelocityEngine).getTemplate(templateFile, StandardCharsets.UTF_8.toString());
		ArgumentCaptor<VelocityContext> contextCaptor = ArgumentCaptor.forClass(VelocityContext.class);
		verify(mockTemplate).merge(contextCaptor.capture(), any());
		VelocityContext context = contextCaptor.getValue();
		assertNotNull(context);
		assertEquals(1L, context.get("one"));
		assertEquals(2L, context.get("two"));

		verify(mockFileHandleManager).createCompressedFileFromString(eq(sender.getId().toString()), any(),
				eq(messageBody), eq(MimeTypeUtils.TEXT_HTML_VALUE.toString()));
	}
	
	@Test
	public void testSendMessageWithNullTempalte() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(null);
		}).getMessage();
		assertEquals("MessageTemplate is required.", message);	
	}
	
	@Test
	public void testSendMessageWithNullTempalteFile() {
		templateFile = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.templateFile is required.", message);	
	}
	
	@Test
	public void testSendMessageWithNullSender() {
		sender = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.sender is required.", message);	
	}
	
	@Test
	public void testSendMessageWithNullRecipients() {
		recipients = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.recipients is required.", message);	
	}
	
	@Test
	public void testSendMessageWithNullRecipientsEmpty() {
		recipients = Collections.emptySet();
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.recipients must contain at least one recipient", message);	
	}
	
	
	@Test
	public void testSendMessageWithNullCharset() {
		charset = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.templateCharSet is required.", message);	
	}
	
	@Test
	public void testSendMessageWithNullMimeType() {
		mimeType = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			templatedMessageSender.sendMessage(createTemplate());
		}).getMessage();
		assertEquals("MessageTemplate.messageBodyMimeType is required.", message);	
	}
	
	
	void setupMockMergeTemplate(String messageBody) {
	    doAnswer(invocation -> {
	        StringWriter writer = invocation.getArgument(1);
	        writer.append(messageBody);
	        return null;
	    }).when(mockTemplate).merge(any(), any());
	}

	MessageTemplate createTemplate() {
		return new MessageTemplate() {

			@Override
			public Map<String, Object> getTemplateContext() {
				return context;
			}

			@Override
			public String getTemplateFile() {
				return templateFile;
			}

			@Override
			public UserInfo getSender() {
				return sender;
			}

			@Override
			public Set<String> getRecipients() {
				return recipients;
			}

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public Charset getTemplateCharSet() {
				return charset;
			}

			@Override
			public String getMessageBodyMimeType() {
				return mimeType;
			}

		};
	}

}
