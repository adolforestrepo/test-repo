package com.reallysi.rsuite.workflow.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.utils.DitaMapUtils;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * When applied to a content assembly node, finds the ancestor or self
 * publication node and then checks to see if there is already a DITA 
 * map with the expected filename alias. If there is not, calls 
 * the specified transform to generate the map. If there is, uses it.
 * <p>If applied to an XML MO that is a DITA map, simply uses that map.</p>
 * <p>If applied to any other type of MO, throws an exception.</p>
 *
 *
 */
public class EnsureMapForPublicationActionHandler extends BaseWorkflowAction {

	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		wfLog.info("Starting...");
		
		ManagedObject mapMo = null;
		MoListWorkflowObject mosToProcess = context.getMoListWorkflowObject();

		String xsltUrlString = getParameterWithDefault(DitaOtOptions.XSLT_URI_PARAM, DitaOtOptions.DEFAULT_CA_TO_MAP_TRANSFORM_URL);
		String caNodeMoId = resolveVariablesAndExpressions(getParameter(DitaOtOptions.CA_NODE_ID_PARAM));
        String forceNewMapStr = resolveVariables(getParameterWithDefault(DitaOtOptions.FORCE_NEW_MAP_PARAM, "false"));
		
		UserAgent userAgent = new UserAgent("rsuite-workflow-process");
		// Create a session so the transform can use the Web service via the
		// external HTTP access. This is a workaround for the fact that as of
		// RSuite 3.6.3 there is no internal URL for accessing plugin-provided
		// Web services.
		Session session = context.getSessionService().
				createSession("Realm", userAgent,
				              "http://" + context.getRSuiteServerConfiguration().getHostName() +
				              ":" + context.getRSuiteServerConfiguration().getPort() +
				              "/rsuite/rest/v1", context.getAuthorizationService().getSystemUser());
		String skey = session.getKey();

		List<MoWorkflowObject> moWfList = context.getMoListWorkflowObject().getMoList();
		List<ManagedObject> moList = new ArrayList<ManagedObject>();
		for (MoWorkflowObject moWf : moWfList) {
			ManagedObject wfMo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), moWf.getMoid());
			moList.add(wfMo);
		}

		if (mosToProcess.isEmpty()) { 
    		// No context object. There might be a caNodeId parameter.
		    // If not, processing will fail.
			try {
			    mapMo = DitaMapUtils.getMapMoForContainer(
	    		        context,
	                    wfLog,
	                    moList,
	                    xsltUrlString,
	                    caNodeMoId,
	                    forceNewMapStr, 
	                    skey);
        	} catch (Exception e) {
        		throw new BusinessRuleException(e.getMessage());
        	}
		} else {
		    // MO could be a container or a map.
		    for (MoWorkflowObject moWFO : mosToProcess.getMoList()) {
		        String moId = moWFO.getMoid();
		        ManagedObject candMo = context.getManagedObjectService()
		                .getManagedObject(context.getAuthorizationService().getSystemUser(), 
		                                  moId);
		        if (candMo.isNonXml()) continue;
		        if (DitaUtil.isDitaMap(candMo.getElement())) {
		            mapMo = candMo;
		            break;
		        }
		        if (candMo.isAssemblyNode()) {
		        	try {
			            mapMo = DitaMapUtils.getMapMoForContainer(
			                    context,
			                    wfLog,
			                    moList,
			                    xsltUrlString,
			                    caNodeMoId,
			                    forceNewMapStr,
			                    skey);
		        	} catch (Exception e) {
		        		throw new BusinessRuleException(e.getMessage());
		        	}
		            break;
		        }
		    }
		}
		
		if (mapMo == null) {
		    throw new BusinessRuleException("No DITA map in the workflow context. Cannot continue.");
		}
		
		wfLog.info("Setting object " + RSuiteUtils.formatMoId(mapMo) + " as workflow context object.");
		moWfList.add(new MoWorkflowObject(mapMo.getId()));
		context.setMoListWorkflowObject(new MoListWorkflowObject(moWfList));
		
		wfLog.info("Done.");
	}


	
	public void setForceNewMap(String forceNewMap) {
		setParameter(DitaOtOptions.FORCE_NEW_MAP_PARAM, forceNewMap);
	}

	public void setXsltUri(String xsltUri) {
		setParameter(DitaOtOptions.XSLT_URI_PARAM, xsltUri);
	}

	public void setCaNodeId(String caNodeId) {
		setParameter(DitaOtOptions.CA_NODE_ID_PARAM, caNodeId);
	}

	
}
