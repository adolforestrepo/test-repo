package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.ContainerType;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACE;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.security.ContentPermission;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.SecurityService;

public class ProjectCreateCAsCTDOActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  private static final String VOLUME_STRING = "Volume";
private static String MAGAZINES_NAME = "Magazines";
  private static String CTDO_NAME = "CTDO";
  protected Log wfLog;

  @Override
  public void execute(WorkflowContext context) throws Exception {

    wfLog = context.getWorkflowLog();
    User user = context.getAuthorizationService().getSystemUser();
    ContentAssembly contentCA = null;

       
    String volume = context.getVariableAsString(ATD_VAR_VOLUME_NUMBER);
    String docxId = context.getVariableAsString(ATD_VAR_DOCX_ID);
    ContentAssemblyService caSrv = context.getContentAssemblyService();
    if (volume.equals("99")) {

      ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
      caCreateOp.setSilentIfExists(true);

      caCreateOp.setType(ContainerType.FOLDER.getSystemName());
      ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME, caCreateOp);
      ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(), CTDO_NAME, caCreateOp);
      caCreateOp.setType(ContainerType.CA.getSystemName());
      ContentAssembly articleCA = caSrv.createContentAssembly(user, tpmCa.getId(), context.getVariableAsString(ATD_VAR_SOURCE_FILENAME), caCreateOp);

      caSrv.attach(user, articleCA.getId(), docxId, new ObjectAttachOptions());
      context.setVariable(ATD_VAR_CA_ID, articleCA.getId());
      contentCA = articleCA;

    } else {

      ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
      caCreateOp.setSilentIfExists(true);

      caCreateOp.setType(ContainerType.FOLDER.getSystemName());
      ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME, caCreateOp);
      ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(), CTDO_NAME, caCreateOp);
      ContentAssembly volumeCa = caSrv.createContentAssembly(user, tpmCa.getId(), VOLUME_STRING+" "+volume, caCreateOp);

      caCreateOp.setType(ContainerType.CA.getSystemName());
      ContentAssembly monthCa = caSrv.createContentAssembly(user, volumeCa.getId(), "Issue " + context.getVariableAsString(ATD_VAR_MONTH), caCreateOp);
    
  	/** Changes related to lastest isue igestin old articles */
		String sourceFilename = context
				.getVariableAsString(ATD_VAR_SOURCE_FILENAME);
		ContentAssembly articleCA = null;
		String canodeId = AstdActionUtils.getCAnodeIdbyName(monthCa,
				sourceFilename);

		if (!canodeId.isEmpty()) {
			articleCA = caSrv.getContentAssembly(user,
					AstdActionUtils.convertCAnodeToCA(context, canodeId));
		} else {
			articleCA = caSrv.createContentAssembly(user, monthCa.getId(),
					context.getVariableAsString(ATD_VAR_SOURCE_FILENAME),
					caCreateOp);
		}
		/** end of changes */
      caSrv.attach(user, articleCA.getId(), docxId, new ObjectAttachOptions());
      context.setVariable(ATD_VAR_CA_ID, articleCA.getId());
      
      contentCA = articleCA;

    }
    
    MoListWorkflowObject newMoList = new MoListWorkflowObject();
    MoWorkflowObject newWFO = new MoWorkflowObject(contentCA.getId());
    newMoList.addMoObject(newWFO);
    context.setRSuiteContents(newMoList);
    
    
  }

  public final static ACL getAclForResubmittedApplicationMo(ExecutionContext context) throws RSuiteException {
    SecurityService securityService = context.getSecurityService();
    ACE cptAdminAce = securityService.constructACE(Role.ROLE_NAME_RSUITE_USER_ADMIN, ContentPermission.values());
    ACE cptStaffAce = securityService.constructACE(Role.ROLE_NAME_ANY, ContentPermission.EDIT);

    return securityService.constructACL(new ACE[] {cptAdminAce, cptStaffAce});
  }
}
