package com.astd.rsuite.bean;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.astd.indesign.builders.InDesignFromDitaMapBuilder;
import com.astd.indesign.builders.Map2InDesignOptions;
import org.dita2indesign.indesign.inx.model.InDesignDocument;
import org.dita2indesign.indesign.inx.writers.InxWriter;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;

public class XML2InDesignBean extends TransformSupportBean {
	private String styleCatalogUri;
				
	public XML2InDesignBean(WorkflowExecutionContext context, String xsltUriString, String styleMapUri) throws RSuiteException{
		super(context, xsltUriString);
		this.styleCatalogUri = styleMapUri;
	}


	public void generateInDesignFromMap(URL mapFile, File resultInxFile,
			Map2InDesignOptions options) throws Exception{
		InDesignFromDitaMapBuilder builder = new InDesignFromDitaMapBuilder();
		InDesignDocument doc = builder.buildMapDocument(mapFile, options);
		InxWriter writer = new InxWriter(resultInxFile);
		writer.write(doc);
				
	}
	
	public void generateInDesignFromTopic(ManagedObject mo, File resultInxFile, LoggingSaxonMessageListener logger) throws Exception {
		Log wfLog = context.getWorkflowLog();

		File tempDir = null;
		try {
			tempDir = getTempDir("generateInDesignFromTopic", false);
		} catch (Exception e) {
			String msg = "Failed to get a temporary directory: " + e.getMessage(); 
			logAndThrowRSuiteException(wfLog, e, msg);
		} 
		
        Map<String, String> params = new HashMap<String, String>();
		params.put("styleCatalogUri", styleCatalogUri);
		params.put("outputPath", tempDir.getAbsolutePath());
		
		applyTransform(mo.getElement().getOwnerDocument(), resultInxFile, params, logger, wfLog, tempDir);
		
	}

}
