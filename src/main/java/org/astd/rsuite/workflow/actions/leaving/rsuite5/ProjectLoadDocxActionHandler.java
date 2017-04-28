package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.astd.rsuite.utils.MOUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectLoadDocxActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  protected Log wfLog;

  @Override
  public void execute(WorkflowContext context) throws Exception {

    wfLog = context.getWorkflowLog();
    context.setVariable(EXCEPTION_OCCUR, false);
    try {
      String workingFolder = context.getVariableAsString(
          com.reallysi.rsuite.api.workflow.WorkflowConstants.VAR_WORKINGFOLDER_PATH);
      File workingDir = new File(workingFolder);
      File[] files = workingDir.listFiles();
      File file = files[0];

      String name = file.getName();

      ManagedObjectAdvisor moAdvisor = null;
      InputStream is = new FileInputStream(file);
      ManagedObject sourceMO = MOUtils.load(context, context.getAuthorizationService()
          .getSystemUser(), name, is, moAdvisor);

      wfLog.info("Loaded source mo " + name + " with id " + sourceMO.getId());

      context.setVariable(ATD_VAR_DOCX_ID, sourceMO.getId());
    } catch (Exception e) {
      context.setVariable(EXCEPTION_OCCUR, true);
    }

  }
}
