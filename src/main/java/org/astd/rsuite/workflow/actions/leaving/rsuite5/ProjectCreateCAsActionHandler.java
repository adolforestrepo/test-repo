package org.astd.rsuite.workflow.actions.leaving.rsuite5;

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
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.SecurityService;

public class ProjectCreateCAsActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  private static String MAGAZINES_NAME = "Magazines";
  private static String TPM_NAME = "TPM";
  protected Log wfLog;

  @Override
  public void execute(WorkflowContext context) throws Exception {

    wfLog = context.getWorkflowLog();
    User user = context.getAuthorizationService().getSystemUser();

    String volume = context.getVariableAsString(ATD_VAR_VOLUME_NUMBER);
    String docxId = context.getVariableAsString(ATD_VAR_DOCX_ID);
    ContentAssemblyService caSrv = context.getContentAssemblyService();
    if (volume.equals("99")) {

      ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
      caCreateOp.setSilentIfExists(true);

      caCreateOp.setType(ContainerType.FOLDER.getSystemName());
      ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME, caCreateOp);
      ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(), TPM_NAME, caCreateOp);
      caCreateOp.setType(ContainerType.CA.getSystemName());
      ContentAssembly articleCA = caSrv.createContentAssembly(user, tpmCa.getId(), context.getVariableAsString(ATD_VAR_SOURCE_FILENAME), caCreateOp);

      caSrv.attach(user, articleCA.getId(), docxId, new ObjectAttachOptions());
      context.setVariable(ATD_VAR_CA_ID, articleCA.getId());

    } else {

      ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
      caCreateOp.setSilentIfExists(true);

      caCreateOp.setType(ContainerType.FOLDER.getSystemName());
      ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME, caCreateOp);
      ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(), TPM_NAME, caCreateOp);
      ContentAssembly volumeCa = caSrv.createContentAssembly(user, tpmCa.getId(), volume, caCreateOp);

      caCreateOp.setType(ContainerType.CA.getSystemName());
      ContentAssembly monthCa = caSrv.createContentAssembly(user, volumeCa.getId(), "Issue " + context.getVariableAsString(ATD_VAR_MONTH), caCreateOp);
      ContentAssembly articleCA = caSrv.createContentAssembly(user, monthCa.getId(), context.getVariableAsString(ATD_VAR_SOURCE_FILENAME), caCreateOp);

      caSrv.attach(user, articleCA.getId(), docxId, new ObjectAttachOptions());
      context.setVariable(ATD_VAR_CA_ID, articleCA.getId());

    }
  }

  public final static ACL getAclForResubmittedApplicationMo(ExecutionContext context) throws RSuiteException {
    SecurityService securityService = context.getSecurityService();
    ACE cptAdminAce = securityService.constructACE(Role.ROLE_NAME_RSUITE_USER_ADMIN, ContentPermission.values());
    ACE cptStaffAce = securityService.constructACE(Role.ROLE_NAME_ANY, ContentPermission.EDIT);

    return securityService.constructACL(new ACE[] {cptAdminAce, cptStaffAce});
  }
}
