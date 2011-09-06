package org.sagebionetworks.web.client;

import com.google.gwt.i18n.client.DateTimeFormat;

public class DisplayConstants {
	
	/*
	 * DEMO FLAG
	 * Set this flag if you want demo content shown
	 */
	public static boolean showDemoHtml = false;
	public static final String TURN_DEMO_ON_TOKEN = "d";
	public static final String TURN_DEMO_OFF_TOKEN = "nd";
	
	/*
	 * Dimensions
	 */
	public static final int DEFAULT_GRID_COLUMN_WIDTH_PX = 150;
	public static final int DEFULAT_GRID_LAYER_COLUMN_WIDTH_PX = 100;	
	public static final int DEFULAT_GRID_DATE_COLUMN_WIDTH_PX = 85;
	
	public static final int MAX_COLUMNS_IN_GRID = 100;
	public static final int DESCRIPTION_SUMMARY_LENGTH = 450; // characters for summary
	
	public static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getFormat("dd-MMM-yyyy");
	public static final DateTimeFormat DATE_FORMAT_SERVICES = DateTimeFormat.getFormat("yyyy-MM-dd");	
	
	
	/*
	 * Text constatns
	 */
	public static final String FOLLOW_DATASET_HTML = "Following a dataset allows you to be notified when the dataset has new comments, additional layers or data, is used in a project, or referenced by a new publication.<br/><br/>Click Confirm to Follow this dataset. You can adjust your notifications settings through your Profile.";  
	public static final String FOLLOW_PROJECT_HTML = "Following a project allows you to be notified when the project has new comments, additional layers or data, is used in a project, or referenced by a new publication.<br/><br/>Click Confirm to Follow this project. You can adjust your notifications settings through your Profile.";
	
	public static final String TEMP_MSKCC_DESCRIPTION = "Genetic and epigenetic alterations have been identified that lead to transcriptional Annotation of prostate cancer genomes provides a foundation for discoveries that can impact disease understanding and treatment. Concordant assessment of DNA copy number, mRNA expression, and focused exon resequencing in the 218 prostate cancer tumors represented in this dataset haveidentified the nuclear receptor coactivator NCOA2 as an oncogene in approximately 11% of tumors. Additionally, the androgen-driven TMPRSS2-ERG fusion was associated with a previously unrecognized, prostate-specific deletion at chromosome 3p14 that implicates FOXP1, RYBP, and SHQ1 as potential cooperative tumor suppressors. DNA copy-number data from primary tumors revealed that copy-number alterations robustly define clusters of low- and high-risk disease beyond that achieved by Gleason score.";
	
	public static final String CREATE_DATASET_TEXT = "Please fill out the initial descriptive fields for the dataset you would like to create. You may add detailed annotations and upload Layers to this dataset by editing it once it is created.";
	public static final String EDIT_DATASET_TEXT = "Edit the dataset's details by altering the fields below and then clicking the Save button. To cancel, click the Close button."; 
	public static final String CREATE_LAYER_TEXT = "Please fill out the initial descriptive fields for the layer you would like to create. You may add detailed annotations and upload an actual data file to this layer by editing it once it is created.";
	public static final String EDIT_LAYER_TEXT = "Edit the layer's details by altering the fields below and then clicking the Save button. To cancel, click the Close button.";
	public static final String CREATE_PROJECT_TEXT = "Please fill out the initial descriptive fields for the project you would like to create.";
	public static final String EDIT_PROJECT_TEXT = "Edit the project's details by altering the fields below and then clicking the Save button. To cancel, click the Close button."; 
	public static final String EDIT_ANNOTATIONS_TEXT = "Edit Annotations for this object. You can add new annotations by using the form fields at the bottom.";
	
	public static final String LOGOUT_TEXT = "You have been logged out of Synapse.";
	public static final String LOGOUT_SSO_TEXT = "To prevent others from accessing your account in this browser, you should log out of your Google account as well.<br/><a href=\"https://mail.google.com/a/sagebase.org/?logout&hl=en\">Logout of your Sage Google account by clicking here</a>"; // <br/><br/><a href=\"https://www.google.com/accounts/Logout\">Logout of your Google account by clicking here</a>
	public static final String PERMISSIONS_INHERITED_TEXT = "Pemissions not editable as they are being inherited by this resource's parent. (ex: A layer inheriting a dataset's permissions)";
	public static final String PERMISSIONS_CREATE_NEW_ACL_TEXT = "By creating local sharing settings you ignore the permissions that are inherited from its parent.";
	
	/*
	 * Buttons, titles and labels
	 */
	public static final String TITLE_TERMS_OF_USE = "Terms of Use";
	public static final String TITLE_LAYER_PREVIEW = "Layer Preview";
	public static final String BUTTON_SEE_TERMS_OF_USE = "See Terms of Use";
	public static final String BUTTON_FOLLOW_DATASET = "Follow this Dataset";
	public static final String TITLE_SHARING_PANEL = "Sharing Settings";
	public static final String LABEL_SHARING_PANEL_EXISTING = "Permissions";
	public static final String BUTTON_PERMISSIONS_CREATE_NEW_ACL = "Create Local Sharing Settings";
	public static final String MENU_PERMISSION_LEVEL_IS_OWNER = "Is Owner";
	public static final String MENU_PERMISSION_LEVEL_CAN_VIEW = "Can view";
	public static final String MENU_PERMISSION_LEVEL_CAN_EDIT = "Can edit";
	public static final String MENU_PERMISSION_LEVEL_CAN_ADMINISTER = "Administrator";
	public static final String LABEL_PERMISSION_TEXT_ADD_PEOPLE = "Add People";

		
	/*
	 * Service Constants (move to another file?)
	 */
	public static final String SINGLE_SIGN_ON_USERID = "SSO";
	public static final String OPEN_ID_SAGE_LOGIN_BUTTON_TEXT = "Login with a Sagebase.org Account";
	public static final String OPEN_ID_PROVIDER_GOOGLE_VALUE = "https://www.google.com/accounts/o8/id";
	public static final String OPEN_ID_PROVIDER_SAGE_VALUE = "https://www.google.com/accounts/o8/site-xrds?hd=sagebase.org";

	public static final String SERVICE_PARENT_ID_KEY = "parentId";
	public static final String SERVICE_ETAG_KEY = "etag";
	public static final String SERVICE_STATUS_KEY = "status";
	public static final String SERVICE_LAYER_TYPE_KEY = "type";
	
	public static final String SERVICE_HEADER_ETAG_KEY = "ETag";

	public static final String SYNAPSE_ID_PREFIX = "Synapse Id: ";	
	public static final String NODE_DESCRIPTION_KEY = "description";
	public static final String BUTTON_LOGIN_AGAIN = "Go to Synapse Login Page";
	public static final String LABEL_LOGOUT_TEXT = "Logout";
	public static final String BUTTON_CANCEL = "Cancel";
	public static final String BUTTON_ADD_ANNOTATION = "Add Annotation";
	public static final String TEXT_NO_DOWNLOADS = "There are no downloads available.";
	public static final String BUTTON_SETUP_API_PASSWORD = "Create Synapse Password";
	public static final String BUTTON_SAVING = "Saving";
	public static final String BUTTON_CHANGE_PASSWORD = "Change Password";
	public static final String BUTTON_CHANGE_USER_INFO = "Change Your Profile";
	public static final String LABEL_SINGLE_SIGN_ON_LOGGING_IN = "Logging you in.";
	public static final String ERROR_GETTING_PERMISSIONS_TEXT = "READ ONLY MODE. Reason: An error occured in retrieving your level of access.";
	public static final String ERROR_FAILED_PERSIST_AGREEMENT_TEXT = "Your license acceptance was not saved. You will need to sign it again in the future.";	
	public static final String ERROR_USER_ALREADY_EXISTS = "There was a problem creating your account: The email address provided is already in use. If you have forgotten your password, please use the \"Forgot Password\" button from the login page.";
	public static final String ERROR_GENERIC = "An error occured. Please try again.";
	public static final String ERROR_SAVE_MESSAGE = "An error occuring attempting to save. Please try again.";
	public static final String ERROR_BAD_REQUEST_MESSAGE = "An unknown communication error occured. Please reload the page.";
	public static final String ERROR_DUPLICATE_ENTITY_MESSAGE = "An entity with this name already exists. Please enter a different name.";
	public static final String ERROR_INVALID_ENTITY_NAME = "Name contains contains an invalid character";
	public static final String ERROR_TITLE_LOGIN_REQUIRED = "Login Required";
	public static final String ERROR_LOGIN_REQUIRED = "You will need to login for access to that resource.";
	public static final String ERROR_TITLE_VALIDATION_ERROR = "Validation Error";
	public static final String ERROR_ALL_FIELDS_REQUIRED = "All fields are required.";
	public static final String BUTTON_REGISTER = "Register";
	public static final String TITLE_ADD_ANNOTATION = "Add Annotation";
	

	/*
	 * Style names
	 */
	public static final String STYLE_NAME_GXT_GREY_BACKGROUND = "gxtGreyBackground";
	
	
	/*
	 * Demo strings
	 */
	public static final String DEMO_COMMENTS = "<div id=\"scrollable_item\">							<span class=\"scrollable_header\">								<img src=\"static/images/down_arrow.png\" class=\"right\" />								<span class=\"scroll_text\">New analysis using this dataset in the Federation Warburg Project</span>								<span class=\"author\"><a href=\"\">Xudong D.</a></span>								<span class=\"date\">21-Nov-2010</span>							</span>							<span class=\"scrollable_comments\">								<span class=\"scrollable_comment\">									<span class=\"comment_text\">The paper references 149 matched normal samples.  Do you have this data?</span>									<span class=\"comment_author\">Sam S.</span>									<span class=\"comment_date\">22-Dec-2010</span>								</span>								<span class=\"scrollable_comment\">									<span class=\"comment_text\">We have only able to obtain 29 matched normal samples.</span>									<span class=\"comment_author\">Matt F.</span>									<span class=\"comment_date\">22-Nov-2010</span>								</span>							</span>						</div>						<div id=\"scrollable_item\">							<span class=\"scrollable_header\">								<img src=\"static/images/down_arrow.png\" class=\"right\" />								<span class=\"scroll_text\">New analysis using this dataset in the MetaGEO Project</span>								<span class=\"author\"><a href=\"\">Brig M</a></span>								<span class=\"date\">01-Apr-2011</span>							</span>							<span class=\"scrollable_comments\">								<span class=\"scrollable_comment\">									<span class=\"comment_text\">Look out, there is a large batch effect in this data.  Have been working on a way to remove it.</span>									<span class=\"comment_author\">Brig M.</span>									<span class=\"comment_date\">02-Apr-2011</span>								</span>								<span class=\"scrollable_comment\">									<span class=\"comment_text\">Appreciate the help.  We can post new version of the normalized data.</span>									<span class=\"comment_author\">Matt F.</span>									<span class=\"comment_date\">02-Apr-2011</span>								</span>							</span>						</div>";
	public static final String DEMO_ANALYSIS = "<table width=\"100%\" class=\"detail\">							<tr>								<td class=\"analysis\"><a href=\"network_overview.html\">Network Generation</a></td>								<td class=\"project\"><a href=\"project_details.html\">Federation Warburg</a></td>								<td class=\"last_modified\">12-Apr-2011</td>							</tr>							<tr class=\"gray\">								<td class=\"analysis\"><a href=\"network_overview.html\">Network Survey</a></td>								<td class=\"project\"><a href=\"project_details.html\">Federation Warburg</a></td>								<td class=\"last_modified\">23-Mar-2011</td>							</tr>							<tr>								<td class=\"analysis\"><a href=\"network_overview.html\">Interactome</a></td>								<td class=\"project\"><a href=\"project_details.html\">Federation Warburg</a></td>								<td class=\"last_modified\">04-Mar-2011</td>							</tr>							<tr class=\"gray\">								<td class=\"analysis\"><a href=\"network_overview_metageo.html\">GSE 21034</a></td>								<td class=\"project\"><a href=\"project_details_metageo.html\">MetaGEO</a></td>								<td class=\"last_modified\">2-Apr-2011</td>							</tr> <tr> <td class=\"analysis\"><a href=\"analysis_predictor.html\">Biomarker Prediction</a></td> <td class=\"project\"><a href=\"project_details_predictor.html\">Predictive Modeling</a></td> <td class=\"last_modified\">07-Jul-2011</td> </tr>						</table>";
	public static final String DEMO_OVERVIEW = "Genetic and epigenetic alterations have been identified that lead to transcriptional Annotation of prostate cancer genomes provides a foundation for discoveries that can impact disease understanding and treatment. Concordant assessment of DNA copy number, mRNA expression, and focused exon resequencing in the 218 prostate cancer tumors represented in this dataset haveidentified the nuclear receptor coactivator NCOA2 as an oncogene in approximately 11% of tumors. Additionally, the androgen-driven TMPRSS2-ERG fusion was associated with a previously unrecognized, prostate-specific deletion at chromosome 3p14 that implicates FOXP1, RYBP, and SHQ1 as potential cooperative tumor suppressors. DNA copy-number data from primary tumors revealed that copy-number alterations robustly define clusters of low- and high-risk disease beyond that achieved by Gleason score.";
	
	
	


}

