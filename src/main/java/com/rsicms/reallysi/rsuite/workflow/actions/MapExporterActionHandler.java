/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.ProcessedWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.PluginVersionConstants;
import com.reallysi.tools.dita.RSuiteDitaHelper;
import com.reallysi.tools.ditaot.DitaOtOptions;

/**
 * Takes one or more DITA map managed objects and exports the map and its dependencies to
 * the file system. 
 */
public class MapExporterActionHandler extends RSuiteDitaSupportActionHandlerBase {

	/**
	 * 
	 */
	public static final String EXPORTED_MAP_FILES_VARNAME = "mapFiles";

	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		wfLog.info("Using rsuite-dita-support version " + PluginVersionConstants.getVersion() + ".");

		
		File tempDir = new File(File.createTempFile("gargage", ""), "mapExporter_" + context.getWorkflowInstanceId());
		FileUtils.deleteQuietly(tempDir);
		tempDir.mkdirs(); // Assume this succeeds
		
		String outputPath = getParameterWithDefault(DitaOtOptions.OUTPUT_PATH_PARAM, tempDir.getAbsolutePath());
		outputPath = resolveVariablesAndExpressions(outputPath);
		
		wfLog.info("Output path is \"" + outputPath + "\"");
		
		MoListWorkflowObject mos = context.getMoListWorkflowObject();
		if (mos == null) {
			wfLog.warn("No managed objects in the workflow context. Nothing to do");
			return;
		}
		
		ManagedObjectService moSvc = context.getManagedObjectService();
		
		boolean errorOccured = false;
		String errorMessage = "";
		ArrayList<ProcessedWorkflowObject> errorMos = new ArrayList<ProcessedWorkflowObject>();
		ArrayList<ProcessedWorkflowObject> exportedMaps = new ArrayList<ProcessedWorkflowObject>();
		
		
		for (MoWorkflowObject moObj : mos.getMoList()) {
			ManagedObject mo = moSvc.getManagedObject(context.getAuthorizationService().getSystemUser(), 
			                                          moObj.getMoid());
			Element root = mo.getElement();
			if (DitaUtil.isDitaMap(root)) {
				try {
					FileWorkflowObject exportedMapFile = RSuiteDitaHelper.exportMapOrTopic(context, wfLog, outputPath, mo);
					exportedMaps.add(exportedMapFile);
				} catch (Exception e) {
					errorOccured = true;
					errorMessage = errorMessage + "Failed MO: [" + moObj.getMoid() + "]\r\n" + e + "\r\n\r\n";
					errorMos.add(moObj);
					e.printStackTrace();
					wfLog.error(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
				}
			} else {
				wfLog.info("Managed object [" + mo.getId() + "] " + mo.getDisplayName() + " does not appear to be a DITA map, skipping.");
			}
		}
		
		context.setVariable(EXPORTED_MAP_FILES_VARNAME, exportedMaps);
		
		if(errorOccured) {
			wfLog.error("Errors occurred during map export, throwing exception");
			context.setAttribute("errorMos", errorMos.toString());
			wfLog.error("Errors from map exports");
			// FIXME: This next bit is just a placeholder for more complete error handling.
			BusinessRuleException e = new BusinessRuleException(errorMos.toString());
			e.setFailureDetail(getParameter(FAILURE_DETAIL_PARAM));
			e.setTaskname("Export DITA maps");
			throw e;
		} else {
			wfLog.info("Maps exported successfully");
		}

		wfLog.info("Done");
    }


	public void setOutputPath(String outputPath) {
		this.setParameter(DitaOtOptions.OUTPUT_PATH_PARAM, outputPath);
	}

    
}
