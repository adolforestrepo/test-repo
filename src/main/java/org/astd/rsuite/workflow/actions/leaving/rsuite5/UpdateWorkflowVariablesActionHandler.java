package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.Map;

import org.apache.commons.logging.Log;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACE;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.security.ContentPermission;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.api.workflow.activiti.BaseTaskListener;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.SecurityService;

public class UpdateWorkflowVariablesActionHandler extends BaseTaskListener
		implements TempWorkflowConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6450501060624688733L;
	/**
	 * 
	 */
	private static final String ATD_DOCX_EXT = ".docx";
	private static final String SOURCE_FILE_PATH = "rsuiteSourceFilePath";
	protected Log wfLog;

	@Override
	public void execute(WorkflowContext context) throws Exception {

		wfLog = context.getWorkflowLog();
		User user = context.getAuthorizationService().getSystemUser();

		Map<String, Object> variables = context.getWorkflowInstance()
				.getInstanceVariables();

		String contents = context.getRSuiteContents()[0];

		if (contents == null || variables == null) {
			wfLog.error("Error getting workflow instance variables or Content Article.");
			return;
		}

		ContentAssembly ca = context.getContentAssemblyService()
				.getContentAssembly(user, contents);

		String sourceFileName = variables.get(this.ATD_VAR_SOURCE_FILENAME)
				.toString();

		String ca_name = ca.getDisplayName();

		if (sourceFileName.equalsIgnoreCase(ca_name)) {
			wfLog.info("Current sourceFileName is equal to the Article Display anme: "
					+ ca_name);
			return;
		}

		updateVariables(context, ca_name);
		
		// Change te variables acording to the New name

	}

	private void updateVariables(WorkflowContext context, String ca_name) {

		AstdArticleFilename newName = null;
		try {
			newName = new AstdArticleFilename(ca_name);
		} catch (Exception e) {
			wfLog.error("MO does not have a valid article name: " + ca_name
					+ e.getLocalizedMessage());

		}

		String pubCode = newName.pubCode;
		String type = newName.type;
		String volume = newName.volume;
		String issue = newName.issue;
		String author = newName.author;

		context.setVariable(ATD_VAR_SOURCE_FILENAME, ca_name);
		context.setVariable(ATD_VAR_VOLUME_NUMBER, volume);
		context.setVariable(ATD_VAR_ISSUE, issue);
		context.setVariable(ATD_VAR_FULL_FILENAME, ca_name + ATD_DOCX_EXT);
		context.setVariable(SOURCE_FILE_PATH, ca_name + ATD_DOCX_EXT);
		context.setVariable(ATD_VAR_PUB_CODE, pubCode);
		context.setVariable(ATD_VAR_FILENAME_AUTHOR, author);
		context.setVariable(ATD_VAR_MONTH, volume); // TODO: is it true that it
													// is always equest to the
													// volume
		context.setVariable(ATD_VAR_ARTICLE_TYPE, type);

		wfLog.info("Variable sourceFileName set to: " + ca_name);
		
		//Change Task description
		context.getTask().setDescription("XXXXX");
		

	}

	public final static ACL getAclForResubmittedApplicationMo(
			ExecutionContext context) throws RSuiteException {
		SecurityService securityService = context.getSecurityService();
		ACE cptAdminAce = securityService.constructACE(
				Role.ROLE_NAME_RSUITE_USER_ADMIN, ContentPermission.values());
		ACE cptStaffAce = securityService.constructACE(Role.ROLE_NAME_ANY,
				ContentPermission.EDIT);

		return securityService.constructACL(new ACE[] { cptAdminAce,
				cptStaffAce });
	}
}
