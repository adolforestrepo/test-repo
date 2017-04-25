package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
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
      String workingFolder = context.getVariableAsString(com.reallysi.rsuite.api.workflow.WorkflowConstants.VAR_WORKINGFOLDER_PATH);
      File workingDir = new File(workingFolder);
      File[] files = workingDir.listFiles();
      File file = files[0];
      
      String name = file.getName();
      
      String[] aliases = new String[] {name};
      ObjectInsertOptions options = new ObjectInsertOptions(name, aliases, null, false);
      ManagedObject sourceMO = context.getManagedObjectService().load(context.getAuthorizationService().getSystemUser(), new NonXmlObjectSource(file), options);

      wfLog.info("Loaded source mo " + name + " with id " + sourceMO.getId());
      context.setVariable(ATD_VAR_DOCX_ID, sourceMO.getId());
    } catch (Exception e) {
      context.setVariable(EXCEPTION_OCCUR, true);
    }

  }
}
