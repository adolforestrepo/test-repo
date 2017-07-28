/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import net.sf.saxon.s9api.Serializer;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.conversion.beans.TransformSupportBean;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Generic action handler for transforming a content assembly or content assembly
 * node into a DITA map that is then imported into RSuite.
 */
public class CaNodeToDitaMapActionHandler extends RSuiteDitaSupportActionHandlerBase {

	private static final long serialVersionUID = -1;

	/**
	 * URI of the XSLT transform to apply to the CA. If not specified, the default
	 * transform is used. 
	 */
	public static final String XSLT_URL_PARAM = "xsltUrl";
	
	/**
	 * MO ID of the CA node to be transformed into a map.
	 */
	public static final String CA_NODE_ID_PARAM = "caNodeId";
	
	
	public static final String DEFAULT_CA_TO_MAP_TRANSFORM_URL = "rsuite:/res/plugin/rsuite-dita-support/canode2map/canode2map_shell.xsl"; 
	
	/**
	 * Controls debugging messages in the transform. Set to "true" turn debugging
	 * messages on.
	 */
	public static final String DEBUG_PARAM = "debug";

	
	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws RSuiteException {
		Log wfLog = context.getWorkflowLog();
		
		ManagedObject caNodeMo = getCaNode(context);
		
		String xsltUrlString = resolveVariablesAndExpressions(getParameterWithDefault(XSLT_URL_PARAM, DEFAULT_CA_TO_MAP_TRANSFORM_URL));
		if ("".equals(xsltUrlString) || xsltUrlString == null) {
			throw new RSuiteException("No transform URL, cannot continue.");
		}
		
		LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(context.getWorkflowLog());
		Map<String, String> params = new HashMap<String, String>();
		params.put("debug", resolveVariablesAndExpressions(getParameterWithDefault(DEBUG_PARAM, "false")));
		String skey = (String) context.getVariable("skey");
		if (skey == null || "".equals(skey)) {
			UserAgent userAgent = new UserAgent("canode-to-dita-map-handler");
			Session session = context.getSessionService().createSession("Realm", userAgent,
			                                                            "http://" + context.getRSuiteServerConfiguration().getHostName() +
			                                                            ":" + context.getRSuiteServerConfiguration().getPort() + "/rsuite/rest/v1",
			                                                            context.getAuthorizationService().getSystemUser());
			skey = session.getKey();
		}
		params.put("rsuiteSessionKey", skey);
		params.put("rsuiteHost", context.getRSuiteServerConfiguration().getHostName());
		params.put("rsuitePort", context.getRSuiteServerConfiguration().getPort());

		TransformSupportBean tsBean = new TransformSupportBean(context, xsltUrlString);
		Source source = new DOMSource(caNodeMo.getElement());
		RSuiteServerConfiguration serverConfig = context.getRSuiteServerConfiguration();
		source.setSystemId("http://" + serverConfig.getHostName() + ":" + serverConfig.getPort() + "/rsuite/rest/v1/content/" + caNodeMo.getId());

		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		Serializer dest = new Serializer();
		dest.setOutputStream(resultStream);
		
		wfLog.info("Transforming CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId() 
		           + "] to a DITA map using transform \"" + xsltUrlString + "\"...");
		tsBean.applyTransform(source, dest, params, logger, wfLog);
		
		wfLog.info("Loading new DITA map to RSuite...");
		ObjectInsertOptions insertOptions = new ObjectInsertOptions("publication_" + caNodeMo.getId() + ".ditamap", null, null, true);
		ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());
	
		ManagedObject mapMo = null;
		try {
			mapMo = context.getManagedObjectService().load(context.getAuthorizationService().getSystemUser(), 
			                                               loadSource, insertOptions);
		} catch (Exception e) {
			throw new RSuiteException(0, "Exception loading generated DITA map to RSuite: " + e.getMessage(), e);
		}
		wfLog.info("Map loaded as MO [" + mapMo.getId() + "]");
		context.getContentAssemblyService().attach(context.getAuthorizationService().getSystemUser(), caNodeMo.getId(),
		                                           mapMo, new ObjectAttachOptions());
		wfLog.info("Map attached to CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId() + "]");
	}

	private ManagedObject getCaNode(WorkflowContext context)
			throws com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException, RSuiteException {
		Log wfLog = context.getWorkflowLog();
		
		String caNodeMoId = resolveVariablesAndExpressions(getParameter(CA_NODE_ID_PARAM));
		if (caNodeMoId == null) {
			List<MoWorkflowObject> moList = context.getMoListWorkflowObject().getMoList();
			if (moList.size() == 0) {
				throw new BusinessRuleException(CA_NODE_ID_PARAM + " not set and no managed objects in the workflow context. Nothing to do.");
			}
			
			if (moList.size() > 1) {
				wfLog.warn("Multiple MOs in the workflow context. Processing the first one");			
			}
			MoWorkflowObject moWFO = moList.get(0);
			caNodeMoId = moWFO.getMoid();
		}

		ContentAssemblyNodeContainer caNode = RSuiteUtils.getContentAssemblyNodeContainer(context,
		                                                                                  context.getAuthorizationService().getSystemUser(),
		                                                                                  caNodeMoId); 
		if (caNode == null) {
			throw new BusinessRuleException("Managed object [" + caNodeMoId + "] does not exist or is not a content assembly node.");
		}
		
		return context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), 
		                                                          caNode.getId());
	}

	
	public void setXsltUrl(String xsltUrl) {
		this.setParameter(XSLT_URL_PARAM, xsltUrl);
	}
	
	public void setDebug(String debug) {
		this.setParameter(DEBUG_PARAM, debug);
	}

	public void setCaNodeId(String caNodeId) {
		this.setParameter(CA_NODE_ID_PARAM, caNodeId);
	}
}