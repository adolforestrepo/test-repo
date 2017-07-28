package com.reallysi.rsuite.webservice;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ConfigurationProperties;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
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
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.tools.ditaot.DitaOtAntTaskCommand;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Takes a publication content assembly (book or periodical) and transforms
 * to XHTML 
 * from the XML files. 
 *
 */
public class XhtmlGenerationWebService extends DefaultRemoteApiHandler {

	private static Log log = LogFactory.getLog(XhtmlGenerationWebService.class);

	@Override
	public RemoteApiResult execute (
			RemoteApiExecutionContext context,
			CallArgumentList args) throws RSuiteException {
		log.info("execute(): Starting...");
		
		// FIXME: Handle multiple input MOs.

        User user = context.getAuthorizationService()
        		.findUser(context.getPrincipal().getName());
        Session session = context.getSession();
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

		DitaOtOptions ditaotOptions = new DitaOtOptions( context, user, "xhtml");
		ditaotOptions.setSession( session);
        try {
			ditaotOptions.setMessageWriter(writer);
			ditaotOptions.setLog(log);

			ConfigurationProperties configProps = context.getRSuiteServerConfiguration().getConfigurationProperties();
			String tempPath = configProps.getProperty( "ditaot.html.generation.dir", context.getRSuiteServerConfiguration().getTmpDir().getAbsolutePath());
			File ditaot_xhtml = new File( tempPath); 
	        File outdir = new File( ditaot_xhtml, "transformXmlMoToXhtml_" + mo.getId());
	        outdir.mkdirs();
	        ditaotOptions.setOutputDir(outdir);

			if (parentMo != null) {
				// Should be a container
				log.info("execute(): Parent container: " + RSuiteUtils.formatMoId(parentMo));
				ContentAssemblyNodeContainer container = RSuiteUtils
						.getContentAssemblyNodeContainer(
								context, 
								user, 
								parentMo.getId());
				//ditaotOptions.pushCaNode(container);
			} else {
				log.warn("execute(): No parent container managed object.");
			}

			setArgumentIfSpecified( ditaotOptions, "args.copycss", args.getFirstString( "copyCss"));
			setArgumentIfSpecified( ditaotOptions, "args.css", args.getFirstString("css"));
			setArgumentIfSpecified( ditaotOptions, "args.cssroot", args.getFirstString("cssRoot"));
			setArgumentIfSpecified( ditaotOptions, "args.csspath", args.getFirstString("cssPath"));
			setArgumentIfSpecified( ditaotOptions, "args.hdf", args.getFirstString("htmlHead"));
			setArgumentIfSpecified( ditaotOptions, "args.ftr", args.getFirstString("htmlFooter"));
			setArgumentIfSpecified( ditaotOptions, "args.hdr", args.getFirstString("htmlHeader"));
			setArgumentIfSpecified( ditaotOptions, "args.indexshow", args.getFirstString("indexShow"));
			setArgumentIfSpecified( ditaotOptions, "args.outext", args.getFirstString("outExt"));
			setArgumentIfSpecified( ditaotOptions, "args.xhtml.toc", args.getFirstString("index"));
			setArgumentIfSpecified( ditaotOptions, "args.arttlb", args.getFirstString("artLabel"));

			DitaOtAntTaskCommand ditaot = new DitaOtAntTaskCommand();
			
			// The applyToolkitProcessToMos() method manages the export of the 
			// MOs to the file system, so at this point we don't know what the
			// filename of the exported map is.
			List<ManagedObject> moList = new ArrayList<ManagedObject>();
			moList.add( mo);
			log.info("execute(): Generating XHTML from MO...");
			ditaot.execute(moList, ditaotOptions);
			log.info("execute(): XHTML generated");
			StoredReport report = ditaotOptions.getStoredReport();
			ditaotOptions.addInfoMessage( "ditaot", mo.getDisplayName(), "<a target='log' href='/rsuite/rest/v1/report/generated/"+report.getId()+"?skey='"+session.getKey()+">Process Details</a>");
        } catch (RSuiteException e) {
        	ditaotOptions.addFailureMessage(
        			"RSuiteException",
        			mo.getDisplayName(),
        			"Unexpected RSuite exception: " + e.getMessage(),
        			e
        			);
		} catch (Throwable t) {
			log.error("execute(): Unexpected " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        	ditaotOptions.addFailureMessage(
        			"RSuiteException",
        			mo.getDisplayName(),
        			t.getClass().getSimpleName() + ": " +t.getMessage()
        			);
		}
        ProcessMessageContainer messages = ditaotOptions.getMessages();
        
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
	    		"XHTML Generation", 
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
	
	public void setArgumentIfSpecified( DitaOtOptions options, String propName, String value) {
		if ( StringUtils.isNotEmpty( value)) {
			options.setProperty( propName, value);
		}
	}

}
