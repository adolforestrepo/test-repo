package com.reallysi.rsuite.webservice;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.content.ContentDisplayObject;
import com.reallysi.rsuite.api.content.ContentObjectPath;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.tools.dita.conversion.CaToInDesignHelper;
import com.reallysi.tools.dita.conversion.Docx2Xml;
import com.reallysi.tools.dita.conversion.Docx2XmlOptions;
import com.reallysi.tools.dita.conversion.InxGenerationOptions;
import com.reallysi.tools.dita.conversion.Xml2InDesign;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Takes a publication content assembly (book or periodical) and transforms
 * all the DOCX files into XML files and then attempts to generate InDesign
 * from the XML files. 
 *
 */
public class InDesignGenerationWebService extends DefaultRemoteApiHandler {

	private static Log log = LogFactory.getLog(InDesignGenerationWebService.class);

	@Override
	public RemoteApiResult execute (
			RemoteApiExecutionContext context,
			CallArgumentList args) throws RSuiteException {
		log.info("execute(): Starting...");
		
		// FIXME: Handle multiple input MOs.

        User user = context.getAuthorizationService()
        		.findUser(context.getPrincipal().getName());
    	ManagedObject mo = args.getFirstManagedObject(user);
        String targetId = mo.getTargetId();
        if (targetId != null) {
            mo = context.getManagedObjectService().getManagedObject(user, targetId);
        }
    	List<ContentObjectPath> contentObjectPaths = args.getContentObjectPaths(user);
    	ContentObjectPath firstPath = contentObjectPaths.get(0);
    	
    	ManagedObject parentMo = null;
    	ContentDisplayObject parentObject = null;
    	if (firstPath.getSize() > 1) {
    	    parentObject = firstPath.getPathObjects().get(firstPath.getSize() - 2);
    	    parentMo = parentObject.getManagedObject();
    	    targetId = parentMo.getTargetId();
    	    if (targetId != null) {
    	        parentMo = context.getManagedObjectService().getManagedObject(user, targetId);
    	    }
    	} else {
    	    // No parent
    	}
		log.info("execute(): Effective MO: " + RSuiteUtils.formatMoId(mo));
        
        /**
         * Stream to hold report output
         */
        StringWriter stringWriter = new StringWriter();
		InxGenerationOptions generationOptions = new InxGenerationOptions();

		// XMLStreamWriter won't work with current RSuite tomcat version.
        // XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(printWriter);
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.write(
        		"<h1>" + 
        		"Conversion Report for " + RSuiteUtils.formatMoId(mo) +
        		"</h1>"
        		);
        writer.write("<div");
        writer.flush(); // Make sure everything is written to the underlying stream.

        try {
	        	
	
			generationOptions.setMessageWriter(writer);
			generationOptions.setIsBuildInDesignDoc(false);
			generationOptions.setSession(context.getSession());
			generationOptions.setUser(user);
			generationOptions.setLog(log);
			generationOptions.setContextPath(firstPath);

	        File outdir = new File( context.getRSuiteServerConfiguration().getTmpDir(), "transformXmlMoToInCopy_" + mo.getId());
	        outdir.mkdirs();
	        generationOptions.setOutputDir(outdir);

			if (parentMo != null) {
				// Should be a container
				log.info("execute(): Parent container: " + RSuiteUtils.formatMoId(parentMo));
				ContentAssemblyNodeContainer container = RSuiteUtils
						.getContentAssemblyNodeContainer(
								context, 
								user, 
								parentMo.getId());
				generationOptions.pushCaNode(container);
			} else {
				log.warn("execute(): No parent container managed object.");
			}

			if (mo.isAssemblyNode()) {
				containerToInCopy(context, mo.getId(), generationOptions, user);
			} else {
				log.info("execute(): Generating InCopy from MO...");
				moToInCopy(context, mo, generationOptions);
				log.info("execute(): InCopy generated");
			}
	        
        
        } catch (RSuiteException e) {
        	generationOptions.addFailureMessage(
        			"RSuiteException",
        			mo.getDisplayName(),
        			"Unexpected RSuite exception: " + e.getMessage(),
        			e
        			);
		} catch (Throwable t) {
			log.error("execute(): Unexpected " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        	generationOptions.addFailureMessage(
        			"RSuiteException",
        			mo.getDisplayName(),
        			t.getClass().getSimpleName() + ": " +t.getMessage()
        			);
		}
        ProcessMessageContainer messages = generationOptions.getMessages();
        
        writer.write("</div>");
        if (messages.hasFailures()) {
        	writer.write("<p");
        	writer.write(" style=\"color: red;\"");
        	writer.write(">");
        	writer.write("Failures during conversion.");
        	writer.write("</p>\n");
        }
        
    	writer.write("<h2>" + "Messages" + "</h2>");

		messages.reportMessagesByObjectAsHtml(context.getSession().getKey(), writer);
        
        writer.flush();
        writer.close();
        
        MessageType messageType = MessageType.SUCCESS;
        if (messages.hasFailures()) {
        	messageType = MessageType.ERROR;
        }
        
        log.info("execute(): Returning MessageDialogResult");
	    MessageDialogResult result = new MessageDialogResult(
	    		messageType,
	    		"InDesign Generation", 
                stringWriter.toString()	    		
                );
	    
	    if (parentMo != null) {
    	    Map<String, Object> props = new HashMap<String, Object>();
    	    props.put("objects", parentMo.getId());
    	    UserInterfaceAction refreshAction = new UserInterfaceAction("rsuite:refreshManagedObjects");
    	    refreshAction.setProperties(props);
    	    result.addAction(refreshAction);
	    }
	    
        return result;
	}

	private void moToInCopy(
			RemoteApiExecutionContext context, 
			ManagedObject mo,
			InxGenerationOptions generationOptions) throws RSuiteException {
		if (mo.isNonXml()) {
			if (!Docx2Xml.isTransformationApplicable(mo)) {
				generationOptions.addFailureMessage(
						"NonXML", 
						mo.getDisplayName(), 
						"Specified managed object is not XML or cannot be transformed into XML (e.g., a DOCX file)");
				return;
			}
			// do process to transform a single DOCX to XML then to incopy
		} else {
			StringBuffer rootPath = new StringBuffer();
			ContentObjectPath objectPath = generationOptions.getContextPath();
			for ( ContentDisplayObject dispObject: objectPath.getPathObjects()) {
				if ( ! "/".equals( dispObject.getDisplayName())) {
					rootPath.append( dispObject.getDisplayName()+"/");
				}
			}
			generationOptions.setXsltParameter( "objectPath", rootPath.toString());
			generationOptions.setXsltParameter( "rsuiteSessionKey", context.getSession().getKey());
			Xml2InDesign.transformXmlMoToInCopy(context, mo, generationOptions);
		}
	}

	private void containerToInCopy(RemoteApiExecutionContext context,
			String moId, InxGenerationOptions generationOptions, User user)
			throws RSuiteException {
		// Verify that a content assembly was selected
		ContentAssemblyNodeContainer ca = RSuiteUtils
						.getContentAssemblyNodeContainer(
								context, 
								user, 
								moId);
		if (ca == null) {
			throw new RSuiteException(0, "Failed to get a content assembly for ID ["+ moId + "]");	        	
		} else {
		    generationOptions.setLinksDirName("links");
		    generationOptions.setUser(user);

		    Docx2XmlOptions docxOptions = new Docx2XmlOptions(generationOptions);
		    CaToInDesignHelper.generateInDesignFromContentAssembly(
		    		context, 
		    		ca, 
		    		generationOptions, 
		    		docxOptions);
		}
	}

}
