package com.reallysi.rsuite.advisors.mo;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.DefaultManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.XmlApiManager;

/**
 * For conversion_configuration documents, sets the config-id alias.
 */
public class ConvConfigManagedObjectAdvisor extends
		DefaultManagedObjectAdvisor implements ManagedObjectAdvisor {
	
	static final Log log = LogFactory.getLog(ConvConfigManagedObjectAdvisor.class);

	public static final String ALIAS_TYPE_CONFIG_ID = "config-id";

	public void adviseDuringInsert(ExecutionContext context,  
			ManagedObjectAdvisorContext insertContext)
    		throws RSuiteException {
		setConfigIdAlias(context, insertContext);
	}

	protected void setConfigIdAlias(ExecutionContext context,
			ManagedObjectAdvisorContext insertContext) throws RSuiteException {
		File file = insertContext.getNonXmlFile();
		if (file == null) {
			// Must be XML
			Element elem = insertContext.getElement();
			if ("conversion_configuration".equals(elem.getNodeName())) {
				XmlApiManager xmlMgr = context.getXmlApiManager();
				XPathEvaluator eval = xmlMgr.getXPathEvaluator();
				String id = eval.executeXPathToString("*[contains(@class, ' topic/title ')]", elem);
				if (id != null && !"".equals(id)) {
					log.info("adviseDuringInsert(): Setting " + ALIAS_TYPE_CONFIG_ID + " alias to \"" + id + "\"...");
					insertContext.getAliasContainer().addAlias(id, ALIAS_TYPE_CONFIG_ID);
				} else {
					log.warn("No title or empty title string for conversion configuration document \"" + insertContext.getExternalFileName() + "\"");
				}
			}
		}
	}

	/**
	 * Set an alias of type "filename" if not already set.
	 */
	public void adviseDuringUpdate(
			ExecutionContext context,
            ManagedObjectAdvisorContext updateContext)
            throws RSuiteException {
		setConfigIdAlias(context, updateContext);
	}
	
}
