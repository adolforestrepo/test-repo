/**
 * 
 */
//package com.astd.rsuite.actions;
package org.astd.rsuite.workflow.actions.leaving.rsuite5;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
//import com.reallysi.rsuite.api.workflow.AbstractBaseActionHandler;
//import com.reallysi.rsuite.api.workflow.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
//import com.reallysi.rsuite.api.workflow.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
//import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;

import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;

import org.activiti.engine.delegate.Expression;
//import com.reallysi.tools.StringUtil;
import org.apache.commons.lang3.StringUtils;


//import com.reallysi.rsuite.api.extensions.ExecutionContext;
//import com.reallysi.rsuite.api.extensions.ExecutionContextWrapper;

/**
 * Attempts to export a set of managed objects to the specified
 * server-accessible location.
 */
public class AstdExportObjectsActionHandler extends BaseWorkflowAction implements TempWorkflowConstants
{

	private static final long serialVersionUID = -1L;
	
	 protected Expression EXCEPTION_OCCUR;
	 protected Expression targetVariableName;
	 protected Expression destinationPath;
	 protected Expression useDisplayNames;

	/**
	 * Holds a comma-delimited list of managed object IDs. If omitted,
	 * 
	 * the current workflow MO list is used.
	 */
	public static final String OBJECT_IDS_PARAM = "objectIds";
	
	/**
	 * The server-accessible location to which to export the MOs.
	 */
	public static final String DESTINATION_PATH_PARAM = "destinationPath";
	
	/**
	 * If set to "true", an empty list of MO IDs causes an
	 * exception to be thrown.
	 */
	public static String FAIL_IF_EMPTY_PARAM = "failIfEmpty";
	
	/**
	 * If set to "true", use MO display names as the output filenames.
	 */
	public static String USE_DISPLAY_NAMES_PARAM = "useDisplayNames";
	
    @Override
    public void execute(WorkflowContext context) throws Exception {
    	User user =context.getAuthorizationService().getSystemUser();
        Log wfLog = context.getWorkflowLog();
        wfLog.info("Attempting to pdf...");
        
		MoListWorkflowObject moListWorkflowObject = context.getMoListWorkflowObject();
		
		Boolean failIfListIsEmpty = false;
		String failIfEmpty = getParameter(FAIL_IF_EMPTY_PARAM);
		if (StringUtils.isEmpty(failIfEmpty)) {
			failIfListIsEmpty = failIfEmpty.toLowerCase().equals("true");
		}

		boolean useDisplayNames = false;
		String s = getParameter(USE_DISPLAY_NAMES_PARAM);
		if (StringUtils.isEmpty(s)) {
			useDisplayNames = s.toLowerCase().equals("true");
		}
		
		String destinationPath = resolveVariables(getParameter(DESTINATION_PATH_PARAM));
		if (StringUtils.isEmpty(destinationPath)) {
			reportAndThrowRSuiteException( DESTINATION_PATH_PARAM+" is not set");
		}
		
		// Only use the MO workflow object list if objectIds is not specified:
		String objectIds = resolveVariables( getParameter(OBJECT_IDS_PARAM));
		if (StringUtils.isEmpty(objectIds) && 
	        moListWorkflowObject != null && 
	        moListWorkflowObject.getMoList().size() > 0
	       ){
			List<MoWorkflowObject> moList = moListWorkflowObject.getMoList();
			if(moList != null){
				for(int i = 0; i < moList.size(); i++) {
					MoWorkflowObject obj = moList.get(i);
					String id = obj.getMoid();
					if (id != null) {
						if (objectIds == null) {
							objectIds = obj.getMoid();
						} else {
							objectIds += "," + obj.getMoid();
						}
					}
				}
			} 
		}
		
		if ((StringUtils.isEmpty(objectIds)
				|| StringUtils.isEmpty(objectIds))) {
			if (failIfListIsEmpty) {
				reportAndThrowRSuiteException(
						OBJECT_IDS_PARAM+" is not set");
			} 
		}
		
		File outDir = new File(destinationPath);
		outDir.mkdirs();
		
		StringTokenizer st = new StringTokenizer(objectIds , ",");
		while(st.hasMoreTokens()){
			String id = st.nextToken().trim();
			//ManagedObject mo = getManagedObjectService().getManagedObject(getSystemUser(), id);
			ManagedObject mo = context.getManagedObjectService().getManagedObject(user,id);
			exportMo(context, mo, destinationPath, useDisplayNames);


		}
	}

	/**
	 * Export MO to specified directory.
	 * @param context Workflow context.
	 * @param mo Managed object to export.
	 * @param destinationPath Destination directory.
	 * @throws RSuiteException If an error occurs.
	 */
	private void exportMo(
			WorkflowContext context,
			ManagedObject mo,
			String destinationPath,
			boolean useDisplayNames
	) throws RSuiteException
	{
		Log wfLog = context.getWorkflowLog();
		User user =context.getAuthorizationService().getSystemUser();
		InputStream inStream =context.getManagedObjectService().getBytes(user , mo.getId());
		
		
		
		File outputDir = new File(destinationPath);
		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				String dname = mo.getDisplayName();
				reportAndThrowRSuiteException(
						"Error exporting \""+dname+"\": " +
						"Could not create directory \"" +
						outputDir.getAbsolutePath() + "\"");
			}
		}
		try {
			String fileName = null;
			if (useDisplayNames) {
				fileName = mo.getDisplayName();
			} else {
				fileName = mo.getId() + "." + mo.getContentType();
			}
			File outFile = new File(outputDir, fileName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}
			wfLog.info("Writing MO [" + mo.getId() + "] to file \""
					   + outFile.getAbsolutePath() + ":");
			AstdActionUtils.writeToFile(outFile, inStream);
		} catch (Exception e) {
			reportAndThrowRSuiteException(e.getLocalizedMessage());
		}
	}

	public void setDestinationPath(String s) {
		setParameter(DESTINATION_PATH_PARAM, s);
	}
	public void setObjectIds(String s) {
		setParameter(OBJECT_IDS_PARAM, s);
	}
	public void setFailIfEmpty(String s) {
		setParameter(FAIL_IF_EMPTY_PARAM, s);
	}
	public void setUseDisplayNames(String s) {
		setParameter(USE_DISPLAY_NAMES_PARAM, s);
	}
}