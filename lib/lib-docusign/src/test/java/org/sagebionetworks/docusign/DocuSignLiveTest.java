package org.sagebionetworks.docusign;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sagebionetworks.StackConfigurationSingleton;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.LockInformation;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.TemplateInformation;
import com.docusign.esign.model.TemplateSummary;

/**
 * A manual harness for exercising envelope correction against a live DocuSign account, one step at a
 * time. Nothing here asserts, so nothing here runs in a build: the class is inert unless
 * {@code -Ddocusign.live=true} is given, and each step prints what DocuSign reported so it can be
 * read, with a verdict line where an expectation can be checked mechanically.
 * <p>
 * It exists because the behaviors this client depends on cannot be established with a mock. Two
 * silent faults were found this way and are now guarded by {@code DocuSignClientTest}: entering
 * DocuSign's "correct" state is refused without an edit lock, and adding a recipient ignores the tabs
 * nested in it. Keep this harness for the next such question rather than rebuilding it.
 * <p>
 * Steps run in separate JVMs, so what one step learns is written to a properties file under
 * {@code target/} for the next step to read. Run them in order:
 *
 * <pre>
 * MVN="mvn -o test -pl lib/lib-docusign -Djacoco.skip=true \
 *      -Ddocusign.live=true \
 *      -Ddocusign.live.templateId=4d402940-9918-8474-80bc-1f62ec9d01fd \
 *      -Ddocusign.live.pi.email=bruce.hoff+pi@sagebase.org \
 *      -Ddocusign.live.so.email=bruce.hoff+so@sagebase.org \
 *      -Ddocusign.live.collaborator.email=bruce.hoff+c1@sagebase.org"
 *
 * $MVN -Dtest=DocuSignLiveTest#step0_validateTemplate
 * $MVN -Dtest=DocuSignLiveTest#step1_createDraft
 * $MVN -Dtest=DocuSignLiveTest#step2_recordTabsBeforeSend
 * $MVN -Dtest=DocuSignLiveTest#step3_send
 * $MVN -Dtest=DocuSignLiveTest#step4_readEmailTabValue
 * # --- now sign as the principal investigator, from the emailed link ---
 * $MVN -Dtest=DocuSignLiveTest#step5_recordStateAfterPiSigned
 * $MVN -Dtest=DocuSignLiveTest#step6_correctEnvelope
 * $MVN -Dtest=DocuSignLiveTest#step7_verifyAfterCorrection
 * # --- optionally sign as the collaborator and the signing official ---
 * $MVN -Dtest=DocuSignLiveTest#step8_downloadCertificate
 * $MVN -Dtest=DocuSignLiveTest#stepReset_voidAndClearState
 * </pre>
 *
 * The template needs exactly three roles — {@code principal_investigator}, {@code signing_official}
 * and {@code collaborator_1} — with the tabs {@link DocuSignTemplateValidator} requires, and routing
 * order collaborators and PI together, signing official last. The collaborator is deliberately left
 * unbound when the envelope is created, so that sending prunes it and step 6 has a recipient to add
 * back.
 * <p>
 * The {@code stepDiag_*} methods are not part of the sequence. They report what is standing in the way
 * when a step fails: the envelope's status and edit lock, and what the template says its roles' tabs
 * are. {@code stepReset_voidAndClearState} abandons a run so another can start, and tolerates an
 * envelope too far along to void.
 */
@EnabledIfSystemProperty(named = "docusign.live", matches = "true")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DocuSignLiveTest {

	private static final String PI = "principal_investigator";
	private static final String SO = "signing_official";
	private static final String COLLABORATOR = "collaborator_1";

	// Deliberately not the signing official's recipient address: if the rendered email tab shows this
	// value the tab accepts what we supply, and if it shows the recipient's address it self-populates.
	private static final String SO_EMAIL_TAB_VALUE = "different-from-recipient@example.com";

	private static final Path STATE_FILE = Paths.get("target", "docusign-live-test.properties");

	private final DocuSignClientConfig config = new StackDocuSignConfigProvider(
			StackConfigurationSingleton.singleton());
	private final DocuSignAccessTokenProvider accessTokenProvider = new DocuSignAccessTokenProvider(config);
	private final DocuSignEnvelopesApi envelopesApi = new DocuSignEnvelopesApiImpl(config, accessTokenProvider);
	private final DocuSignTemplatesApi templatesApi = new DocuSignTemplatesApiImpl(config, accessTokenProvider);
	private final DocuSignClient client = new DocuSignClient(templatesApi, envelopesApi);

	// --- steps -------------------------------------------------------------------------------

	@Test
	public void step0_validateTemplate() {
		String templateId = required("docusign.live.templateId");
		print("basePath", config.getBasePath());
		print("accountId", config.getAccountId());
		print("templateId", templateId);

		// call under test — fails if the template lacks a required role or tab
		client.validateTemplate(templateId);
		System.out.println("template is valid for eDUC use");
	}

	@Test
	public void step1_createDraft() {
		String templateId = required("docusign.live.templateId");

		// the collaborator is left out, so DocuSign instantiates its role unbound
		Map<String, RecipientInfo> recipients = new LinkedHashMap<>();
		recipients.put(PI, new RecipientInfo(required("docusign.live.pi.email"), "Live PI"));
		recipients.put(SO, new RecipientInfo(required("docusign.live.so.email"), "Live SO"));

		Map<RoleLabelKey, String> tabValues = new LinkedHashMap<>();
		tabValues.put(new RoleLabelKey(PI, PI + "_name"), "Live PI");
		tabValues.put(new RoleLabelKey(PI, PI + "_user_name"), "livepi");
		tabValues.put(new RoleLabelKey(PI, PI + "_email"), required("docusign.live.pi.email"));
		tabValues.put(new RoleLabelKey(SO, SO + "_name"), "Live SO");
		tabValues.put(new RoleLabelKey(SO, SO + "_institution"), "Live Institution");
		tabValues.put(new RoleLabelKey(SO, SO + "_email"), SO_EMAIL_TAB_VALUE);

		// call under test
		String envelopeId = client.createEnvelope(templateId, recipients, tabValues);

		putState("envelopeId", envelopeId);
		print("envelopeId", envelopeId);
		print("signing_official_email tab value sent", SO_EMAIL_TAB_VALUE);
	}

	@Test
	public void step2_recordTabsBeforeSend() {
		Envelope envelope = readEnvelopeWithTabs();
		printRecipients("before send", envelope);

		String collaboratorTabs = describeTabs(signer(envelope, COLLABORATOR).getTabs());
		putState("collaboratorTabsBeforeSend", collaboratorTabs);
		System.out.println("\nrecorded " + COLLABORATOR + " tab placement, to compare after it is added back:");
		System.out.println(collaboratorTabs);
	}

	@Test
	public void step3_send() {
		String envelopeId = state("envelopeId");

		// call under test — prunes the unbound collaborator, then sends
		client.sendEnvelope(envelopeId);

		Envelope envelope = readEnvelopeWithTabs();
		printRecipients("after send", envelope);
		System.out.println();
		verdict("the pruned collaborator is gone, tabs and all",
				findSigner(envelope, COLLABORATOR) == null);
		verdict("the principal investigator was reached",
				findSigner(envelope, PI) != null
						&& !"created".equalsIgnoreCase(findSigner(envelope, PI).getStatus()));
		verdict("the signing official has not been reached yet, being at a later routing order",
				findSigner(envelope, SO) != null
						&& "created".equalsIgnoreCase(findSigner(envelope, SO).getStatus()));
	}

	@Test
	public void step4_readEmailTabValue() {
		Envelope envelope = readEnvelopeWithTabs();
		Tabs tabs = signer(envelope, SO).getTabs();
		String rendered = tabValue(tabs, SO + "_email");
		print("signing_official_email value we sent", SO_EMAIL_TAB_VALUE);
		print("signing_official_email value DocuSign reports", rendered);
		print("signing_official recipient email", signer(envelope, SO).getEmail());
		System.out.println();
		putState("soEmailTabBeforeCorrection", rendered);
		// Settled on 2026-09-04: an emailAddress tab ignores a supplied value and populates itself
		// from the recipient's address once that recipient is reached. Recorded here so that step 7
		// can show whether it follows the recipient when the address is changed by a correction.
		verdict("the supplied value was ignored, as expected of an emailAddress tab",
				!SO_EMAIL_TAB_VALUE.equals(rendered));
		System.out.println("  Confirm against the rendered document too: the field should show the"
				+ " recipient's address once the signing official is reached.");
	}

	@Test
	public void step5_recordStateAfterPiSigned() {
		Envelope envelope = readEnvelopeWithTabs();
		printRecipients("after the PI signed", envelope);

		String soTabs = describeTabs(signer(envelope, SO).getTabs());
		putState("soTabsBeforeCorrection", soTabs);
		putState("templatesBeforeCorrection", describeTemplates());
		System.out.println("\nrecorded signing_official tab placement, to compare after the correction:");
		System.out.println(soTabs);
		print("templates applied", state("templatesBeforeCorrection"));
		System.out.println();
		verdict("the PI is completed", isCompleted(signer(envelope, PI)));
		verdict("the signing official is in flight and unsigned",
				!isCompleted(signer(envelope, SO)));
		System.out.println("  Both must hold before step 6 is meaningful. If the signing official has"
				+ " already signed the envelope is complete and can no longer be corrected — run"
				+ " stepReset_voidAndClearState and start again.");
	}

	@Test
	public void step6_correctEnvelope() {
		print("signing official email changing to",
				property("docusign.live.so.email2", required("docusign.live.so.email")));

		// call under test
		client.correctEnvelope(state("envelopeId"), desiredRecipients(), desiredTabValues());

		System.out.println("correction applied without error");
	}

	// The collaborator is added back and the signing official's email is changed in the same
	// correction, so that one run shows what happens to both a created and an updated recipient.
	private Map<String, RecipientInfo> desiredRecipients() {
		Map<String, RecipientInfo> recipients = new LinkedHashMap<>();
		recipients.put(PI, new RecipientInfo(required("docusign.live.pi.email"), "Live PI"));
		recipients.put(SO, new RecipientInfo(
				property("docusign.live.so.email2", required("docusign.live.so.email")), "Live SO"));
		recipients.put(COLLABORATOR,
				new RecipientInfo(required("docusign.live.collaborator.email"), "Live Collaborator"));
		return recipients;
	}

	private Map<RoleLabelKey, String> desiredTabValues() {
		Map<RoleLabelKey, String> tabValues = new LinkedHashMap<>();
		tabValues.put(new RoleLabelKey(PI, PI + "_name"), "Live PI");
		tabValues.put(new RoleLabelKey(PI, PI + "_user_name"), "livepi");
		tabValues.put(new RoleLabelKey(PI, PI + "_email"), required("docusign.live.pi.email"));
		tabValues.put(new RoleLabelKey(SO, SO + "_name"), "Live SO");
		tabValues.put(new RoleLabelKey(SO, SO + "_institution"), "Live Institution");
		tabValues.put(new RoleLabelKey(SO, SO + "_email"), SO_EMAIL_TAB_VALUE);
		tabValues.put(new RoleLabelKey(COLLABORATOR, COLLABORATOR + "_user_name"), "livecollab");
		tabValues.put(new RoleLabelKey(COLLABORATOR, COLLABORATOR + "_name"), "Live Collaborator");
		return tabValues;
	}

	/**
	 * Which of the envelope's status and edit lock is standing in the way. Run this after a correction
	 * fails with {@code EDIT_LOCK_NOT_LOCK_OWNER}: whether the status reached "correct" says where the
	 * failure happened. Status "correct" means our own transition succeeded and a later recipient call
	 * was refused, so the lock that transition created is one we do not present on subsequent calls.
	 * Status still "sent" means the transition itself was refused, so something outside this process —
	 * most likely the envelope open in the DocuSign web console — holds the lock.
	 */
	@Test
	public void stepDiag_showEnvelopeAndLock() {
		Envelope envelope = readEnvelopeWithTabs();
		print("envelope status", envelope.getStatus());
		printRecipients("now", envelope);

		EnvelopesApi api = new EnvelopesApi(authenticatedApiClient());
		try {
			LockInformation lock = api.getLock(config.getAccountId(), state("envelopeId"));
			System.out.println();
			print("lockedByUser", String.valueOf(lock.getLockedByUser()));
			print("lockedByApp", lock.getLockedByApp());
			print("lockType", lock.getLockType());
			print("lockToken", lock.getLockToken());
			print("lockedUntilDateTime", lock.getLockedUntilDateTime());
		} catch (Exception e) {
			System.out.println("\nno lock reported: " + e.getMessage());
		}
		System.out.println("\nClose any DocuSign web console tabs showing this envelope, then re-run.");
	}

	/** Releases the edit lock, which only works if this API user owns it. */
	@Test
	public void stepDiag_deleteLock() {
		EnvelopesApi api = new EnvelopesApi(authenticatedApiClient());
		try {
			api.deleteLock(config.getAccountId(), state("envelopeId"));
			System.out.println("lock released");
		} catch (Exception e) {
			System.out.println("could not release the lock: " + e.getMessage());
		}
	}

	/**
	 * What {@code getTemplate} actually reports for the template's roles — specifically whether the tab
	 * definitions carry placement. This decides why an added recipient arrived with no tabs:
	 * placement present means the copy was sound and {@code POST /recipients} ignored the nested tabs,
	 * so they need a follow-up {@code createTabs} call. Placement absent (dashes for doc/page/x/y)
	 * means {@code getTemplate} does not return it and the tabs must be read per role with
	 * {@code TemplatesApi.listTabs} instead.
	 */
	@Test
	public void stepDiag_showTemplateTabs() {
		for (Signer signer : templatesApi.getTemplate(required("docusign.live.templateId"))
				.getRecipients().getSigners()) {
			System.out.println("\n--- template role " + signer.getRoleName()
					+ " (routingOrder " + signer.getRoutingOrder() + ") ---");
			System.out.println(describeTabs(signer.getTabs()));
		}
	}

	@Test
	public void step7_verifyAfterCorrection() {
		Envelope envelope = readEnvelopeWithTabs();
		printRecipients("after the correction", envelope);

		String templatesNow = describeTemplates();
		print("templates applied before the correction", state("templatesBeforeCorrection"));
		print("templates applied now", templatesNow);

		Signer collaborator = findSigner(envelope, COLLABORATOR);
		String collaboratorTabsNow = collaborator == null ? "<recipient absent>"
				: describeTabs(collaborator.getTabs());
		String soTabsNow = describeTabs(signer(envelope, SO).getTabs());

		System.out.println("\n--- " + COLLABORATOR + " tabs ---");
		System.out.println("before send: " + state("collaboratorTabsBeforeSend"));
		System.out.println("now        : " + collaboratorTabsNow);
		System.out.println("\n--- " + SO + " tabs ---");
		System.out.println("before correction: " + state("soTabsBeforeCorrection"));
		System.out.println("now              : " + soTabsNow);

		System.out.println();
		verdict("Q1: the envelope still reports exactly one applied template after a correction",
				templatesNow.equals(state("templatesBeforeCorrection")) && !templatesNow.contains(","));
		verdict("Q2: updating a recipient preserved its tab placement",
				soTabsNow.equals(state("soTabsBeforeCorrection")));
		System.out.println("  (a FAIL here means buildUpdatedRecipients strips placement on every"
				+ " correction, and needs the same template-copy path as buildNewRecipients)");
		verdict("the added collaborator's tabs match the template placement recorded in step 2",
				collaboratorTabsNow.equals(state("collaboratorTabsBeforeSend")));
		verdict("Q7: the added collaborator was reached despite the PI (a later order) being complete",
				collaborator != null && !"created".equalsIgnoreCase(collaborator.getStatus()));
		verdict("the PI is still completed and was not rewound",
				isCompleted(signer(envelope, PI)));
		print("signing_official_email before the correction", state("soEmailTabBeforeCorrection"));
		print("signing_official_email now", tabValue(signer(envelope, SO).getTabs(), SO + "_email"));
		print("signing_official recipient email now", signer(envelope, SO).getEmail());
		System.out.println("  An emailAddress tab should follow the recipient's new address rather than"
				+ " keep the old one.");

		System.out.println("\nQ3: open the signing official's link from their most recent email —"
				+ " confirm they can still sign now that a lower routing order was inserted.");
	}

	@Test
	public void step8_downloadCertificate() throws IOException {
		// the certificate of completion, rather than the combined document, so this works before the
		// envelope finishes as well as after
		byte[] certificate = envelopesApi.getDocument(state("envelopeId"), "certificate");
		Path out = Paths.get("target", "docusign-live-test-certificate.pdf");
		Files.write(out, certificate);
		print("wrote", out.toAbsolutePath().toString());
		System.out.println("Read the signing order on the certificate: a collaborator added after the"
				+ " signing official countersigned appears after them, despite routing order 1.");
	}

	/**
	 * Abandons the envelope in progress and forgets it, so that a run can be started over from step 1.
	 * Voiding is skipped if the envelope has already reached a state that cannot be voided.
	 */
	@Test
	public void stepReset_voidAndClearState() {
		try {
			client.voidEnvelope(state("envelopeId"), "Live test restarted.");
			System.out.println("voided " + state("envelopeId"));
		} catch (RuntimeException e) {
			System.out.println("could not void " + state("envelopeId") + ": " + e.getMessage());
		}
		clearState();
		System.out.println("cleared " + STATE_FILE.toAbsolutePath() + " — start again at step1_createDraft");
	}

	// --- reading DocuSign -------------------------------------------------------------------

	/**
	 * The envelope including its recipients <em>and their tabs</em>. {@link DocuSignClient} only ever
	 * asks for recipients, so this goes to the SDK directly rather than widening production code for
	 * the sake of an observation.
	 */
	private Envelope readEnvelopeWithTabs() {
		EnvelopesApi envelopesApi = new EnvelopesApi(authenticatedApiClient());
		EnvelopesApi.GetEnvelopeOptions options = envelopesApi.new GetEnvelopeOptions();
		options.setInclude("recipients,tabs");
		try {
			return envelopesApi.getEnvelope(config.getAccountId(), state("envelopeId"), options);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read envelope " + state("envelopeId"), e);
		}
	}

	private String describeTemplates() {
		EnvelopesApi envelopesApi = new EnvelopesApi(authenticatedApiClient());
		try {
			TemplateInformation information = envelopesApi.listTemplates(config.getAccountId(),
					state("envelopeId"));
			List<TemplateSummary> templates = information == null ? null : information.getTemplates();
			if (templates == null || templates.isEmpty()) {
				return "<none>";
			}
			List<String> ids = new ArrayList<>();
			templates.forEach(template -> ids.add(template.getTemplateId()));
			return String.join(",", ids);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to list templates of " + state("envelopeId"), e);
		}
	}

	private ApiClient authenticatedApiClient() {
		ApiClient apiClient = new ApiClient(config.getBasePath());
		apiClient.addDefaultHeader("Authorization", "Bearer " + accessTokenProvider.getAccessToken());
		return apiClient;
	}

	// --- describing what came back ------------------------------------------------------------

	private static void printRecipients(String when, Envelope envelope) {
		System.out.println("\n=== recipients " + when + " (envelope status " + envelope.getStatus() + ") ===");
		Recipients recipients = envelope.getRecipients();
		if (recipients == null || recipients.getSigners() == null) {
			System.out.println("<none reported>");
			return;
		}
		for (Signer signer : recipients.getSigners()) {
			System.out.printf("  role=%-24s recipientId=%-4s routingOrder=%-4s status=%-12s email=%s%n",
					signer.getRoleName(), signer.getRecipientId(), signer.getRoutingOrder(),
					signer.getStatus(), signer.getEmail());
		}
	}

	/**
	 * A canonical, comparable rendering of every tab's label and placement, whatever its type. Two
	 * renderings differ exactly when a tab moved, appeared or disappeared.
	 */
	private static String describeTabs(Tabs tabs) {
		if (tabs == null) {
			return "<no tabs>";
		}
		List<String> descriptions = new ArrayList<>();
		for (Method method : Tabs.class.getMethods()) {
			if (method.getParameterCount() != 0
					|| !method.getName().startsWith("get")
					|| !method.getName().endsWith("Tabs")
					|| !List.class.isAssignableFrom(method.getReturnType())) {
				continue;
			}
			List<?> tabsOfOneType = (List<?>) call(method, tabs);
			if (tabsOfOneType == null) {
				continue;
			}
			for (Object tab : tabsOfOneType) {
				descriptions.add(String.format("%s[%s]@doc%s/page%s/x%s/y%s",
						tab.getClass().getSimpleName(), getString(tab, "getTabLabel"),
						getString(tab, "getDocumentId"), getString(tab, "getPageNumber"),
						getString(tab, "getXPosition"), getString(tab, "getYPosition")));
			}
		}
		Collections.sort(descriptions);
		return String.join(" ", descriptions);
	}

	private static String tabValue(Tabs tabs, String tabLabel) {
		for (Method method : Tabs.class.getMethods()) {
			if (method.getParameterCount() != 0
					|| !method.getName().startsWith("get")
					|| !method.getName().endsWith("Tabs")
					|| !List.class.isAssignableFrom(method.getReturnType())) {
				continue;
			}
			List<?> tabsOfOneType = (List<?>) call(method, tabs);
			if (tabsOfOneType == null) {
				continue;
			}
			for (Object tab : tabsOfOneType) {
				if (tabLabel.equals(getString(tab, "getTabLabel"))) {
					return tab.getClass().getSimpleName() + " value=" + getString(tab, "getValue");
				}
			}
		}
		return "<no tab labelled " + tabLabel + ">";
	}

	private static String getString(Object target, String getterName) {
		try {
			return String.valueOf(call(target.getClass().getMethod(getterName), target));
		} catch (NoSuchMethodException e) {
			return "-";
		}
	}

	private static Object call(Method method, Object target) {
		try {
			return method.invoke(target);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to call " + method.getName(), e);
		}
	}

	private static boolean isCompleted(Signer signer) {
		String status = signer.getStatus();
		return "completed".equalsIgnoreCase(status) || "signed".equalsIgnoreCase(status);
	}

	private static Signer signer(Envelope envelope, String roleName) {
		Signer signer = findSigner(envelope, roleName);
		if (signer == null) {
			throw new IllegalStateException("The envelope has no recipient for role " + roleName);
		}
		return signer;
	}

	private static Signer findSigner(Envelope envelope, String roleName) {
		if (envelope.getRecipients() == null || envelope.getRecipients().getSigners() == null) {
			return null;
		}
		return envelope.getRecipients().getSigners().stream()
				.filter(signer -> roleName.equals(signer.getRoleName()))
				.findFirst().orElse(null);
	}

	// --- plumbing ---------------------------------------------------------------------------

	private static void print(String label, String value) {
		System.out.println(label + ": " + value);
	}

	private static void verdict(String question, boolean asExpected) {
		System.out.println((asExpected ? "PASS  " : "FAIL  ") + question);
	}

	private static String required(String propertyName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("-D" + propertyName + " is required.");
		}
		return value;
	}

	private static String property(String propertyName, String defaultValue) {
		String value = System.getProperty(propertyName);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	// Steps run in separate JVMs, so what one learns has to outlive it.
	private static String state(String key) {
		String value = readState().getProperty(key);
		if (value == null) {
			throw new IllegalStateException("No '" + key + "' recorded yet — run the earlier steps first.");
		}
		return value;
	}

	private static void putState(String key, String value) {
		Properties properties = readState();
		properties.setProperty(key, value);
		try (OutputStream out = Files.newOutputStream(STATE_FILE)) {
			properties.store(out, "DocuSign live test state");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to write " + STATE_FILE, e);
		}
	}

	private static void clearState() {
		try {
			Files.deleteIfExists(STATE_FILE);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to delete " + STATE_FILE, e);
		}
	}

	private static Properties readState() {
		Properties properties = new Properties();
		if (Files.exists(STATE_FILE)) {
			try (InputStream in = Files.newInputStream(STATE_FILE)) {
				properties.load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to read " + STATE_FILE, e);
			}
		}
		return properties;
	}
}
