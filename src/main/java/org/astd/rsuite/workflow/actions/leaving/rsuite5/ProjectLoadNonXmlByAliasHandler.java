package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

/**
 * Action handler to load a non-XML document by alias.
 * <p>
 * The alias is either determined by the <tt>alias</tt> parameter or the base filename of the
 * incoming file. If the alias already exists, the MO associated with the alias will be updated. If
 * the alias does not exist, a new MO is created for the file and an alias is created for it.
 * </p>
 * <p>
 * This action handler expects the file workflow object in the workflow context to contain the file
 * to be loaded, e.g., via a hotfolder-based workflow or as the output of a transformation performed
 * earlier in the workflow.
 * </p>
 **/
public class ProjectLoadNonXmlByAliasHandler
    extends ProjectLoadByAliasBaseActionHandler
    implements TempWorkflowConstants {
  @Override
  public void execute(WorkflowContext context) throws Exception {
    Log wfLog = context.getWorkflowLog();

    File inFile = null;
    try {
      logActionHandlerParameters(wfLog);

      inFile = getFileToLoad(context);
      if (treatAsXml(inFile.getName())) {
        String msg = "Input file \"" + inFile.getName()
            + "\" is treated as XML but is being loaded as nonXML";
        reportAndThrowRSuiteException(msg);
      }
      String parentMoId = getParentMoId(context);
      wfLog.info("Loading nonxml to: [" + parentMoId + " [");
      ObjectSource src = new NonXmlObjectSource(inFile);
      doLoadOrUpdate(context, wfLog, inFile, src, parentMoId);
    } catch (Exception e) {
      String inFileName = (inFile != null ? inFile.getAbsolutePath() : "");
      reportAndThrowRSuiteException("Exception loading non-XML File " + inFileName + ": " + e
          .getMessage());
    } finally {
      wfLog.info("DONE: Load NonXML by Alias");
    }
  }

}
