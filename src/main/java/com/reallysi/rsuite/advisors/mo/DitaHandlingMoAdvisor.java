package com.reallysi.rsuite.advisors.mo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.DefaultManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * Modifies the incoming data during update to make any defaulted
 * attributes explicit.
 *
 */
public class DitaHandlingMoAdvisor extends DefaultManagedObjectAdvisor {
	
	static final Log log = LogFactory.getLog(DitaHandlingMoAdvisor.class);
	
	public void adviseDuringUpdate(
			ExecutionContext executionContext,
			ManagedObjectAdvisorContext context) throws RSuiteException {
		log.info("adviseDuringUpdate(): Starting...");
		Element root = context.getElement();
		
		// At this point we could run an identity transform on the
		// the DOM to add all defaulted attributes.
		log.info("adviseDuringUpdate(): Done.");
	}

}
