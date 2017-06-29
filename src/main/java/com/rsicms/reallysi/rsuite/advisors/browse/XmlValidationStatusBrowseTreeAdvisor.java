package com.reallysi.rsuite.advisors.browse;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.browse.DefaultBrowseTreeAdvisor;
import com.reallysi.rsuite.api.vfs.BrowseContext;
import com.reallysi.rsuite.api.vfs.BrowseTreeNode;
import com.reallysi.tools.dita.RSuiteDitaSupportConstants;

/**
 * Reflects the validation status of the node if the XML validation status
 * LMD is set.
 */
public class XmlValidationStatusBrowseTreeAdvisor extends DefaultBrowseTreeAdvisor {
	
	public static Log log = LogFactory.getLog(XmlValidationStatusBrowseTreeAdvisor.class);

	@Override
	public void adjustCachedBrowseTreeNode(
			BrowseContext context,
			BrowseTreeNode browseTreeNode, 
			ManagedObject mo, 
			ManagedObject referencedMo)
	throws RSuiteException {
		User user = context.getUser();
		if (user == null) {
			user = context.getAuthorizationService().getSystemUser();
		}
		if (mo != null) {
			String label = mo.getDisplayName();
			try {
				String validationStatus = mo.getLayeredMetadataValue(RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD);
				// If we're an assembly node and we're not already labeled as invalid, check our descendants
				// so we can reflect any invalid descendants.
				// FIXME: The browse tree doesn't get updated properly to allow ancestor nodes
				// to accurately reflect the states of descendant nodes. Will need to work out
				// how to work around this.
				if (false && mo.isAssemblyNode() && 
				    !RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID.equals(validationStatus)) {
					// Ancestor nodes should reflect validation status of descendants nodes.
					List<? extends ManagedObject> items = mo.listDescendantManagedObjects();
					if (items != null) {
						for (ManagedObject item : items) {
							String status = item.getLayeredMetadataValue(RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD);
							if (RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID.equals(status)) {
								validationStatus = RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID;
								break;
							}
							if (RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_VALID.equals(status)) {
								validationStatus = RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_VALID;
							}
						}
					}
				}
				
				if (validationStatus != null) {
					if (RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID.equals(validationStatus)) {
						label = "<span style='color: red;'>[X]</span> " + label;
					} else if (RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_VALID.equals(validationStatus)){
						label = "<span style='color: green;'>[√]</span> " + label;
					} else {
						label = "<span style='color: black;'>[?]</span> " + label;
					}
				}
			} catch (Exception e) {
				log.error("Exception doing browse tree advising",e);
			}
			
			browseTreeNode.setLabel(label);
		}
	}

}
