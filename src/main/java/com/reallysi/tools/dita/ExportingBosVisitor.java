/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.tools.dita;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;

/**
 *
 */
public class ExportingBosVisitor extends BosVisitorBase {

	/**
	 * @param context 
	 * @param log
	 */
	public ExportingBosVisitor(ExecutionContext context, Log log) {
		super(context, log);
	}

	/* (non-Javadoc)
	 * @see com.reallysi.tools.dita.BosVisitor#visit(com.reallysi.tools.dita.DitaBoundedObjectSet)
	 */
	public void visit(BoundedObjectSet bos)
			throws RSuiteException {
		log.info("Exporting BOS " + bos.size() + " members...");
		super.visit(bos);
		log.info("Export done.");
	}

	/* (non-Javadoc)
	 * @see com.reallysi.tools.dita.BosVisitor#visit(com.reallysi.tools.dita.DitaBosMember)
	 */
	public void visit(BosMember member) throws RSuiteException {
		File exportFile = member.getFile();
		if (exportFile == null) {
			throw new RSuiteException("BOS member " + member + " has no associated file. This should not happen.");
		}
		File exportDir = member.getFileSystemDirectory();
		try {
			if (!exportDir.exists()) {
				boolean r = exportDir.mkdirs();
				if (!r) {
					throw new RSuiteException(0, "Failed to create export directory \"" + exportDir.getAbsolutePath() + "\"");
				}
			}
			exportFile = new File(exportDir, member.getFileName());
			OutputStream outStream = new FileOutputStream(exportFile);

			log.info("Exporting member " + member + " to file \"" + exportFile.getAbsolutePath());

			if (member.isXml()) {
				rewritePointers(member);
				applyDitaFilter((XmlBosMember)member, outStream);
			} else {
				IOUtils.copy(member.getInputStream(), outStream);
			}
		} catch (Exception e) {
			throw new RSuiteException(0, "Exception exporting BOS member " + member + ": " + e.getMessage(),e);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.reallysi.tools.dita.DitaBosMember#rewritePointers()
	 */
	public void rewritePointers(BosMember member) throws RSuiteException {
		// Find all pointers and then look up our dependencies by those values
		log.info("rewritePointers(): Handling member " + member.toString() + "...");
		NodeList nl;
		ManagedObject mo = member.getManagedObject();
		if (mo == null) {
			throw new RSuiteException(0, "BOS member had no managed object. Cannot rewrite pointers. All members should have associated MOs at this point in the processing.");
		}
		
		try {
			nl = (NodeList) DitaUtil.allHrefsAndConrefsAndObjectDataRefs.evaluate(mo.getElement(), XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RSuiteException(0, "Unexpected exception evaluating XPath expression \"" + DitaUtil.allHrefsAndConrefsAndObjectDataRefs);
		}
		log.info("rewritePointers():   Found " + nl.getLength() + " hrefs, conrefs, or data references.");
		if (nl.getLength() > 0) {
			// Get local pointer to the DOM so we can mutate it.
			
			for (int i = 0; i < nl.getLength(); i++) {
				Element ref = (Element)nl.item(i);
				if (((ref.hasAttribute("href") || ref.hasAttribute("conref")) && DitaUtil.isLocalScope(ref)) ||
						ref.hasAttribute("data")
						) {
				    String attName = "href";
				    // If conref and href, then worry about the conref. Should be a rare case.
				    if(ref.hasAttribute("conref")) {
				    	attName = "conref";
				    } else if (ref.hasAttribute("data")) {
				    	attName = "data";
				    }
					String origUri = ref.getAttribute(attName);
					if (origUri.startsWith("#")) {
						continue; // Pointer to the same document, no need to rewrite
					}
					String fragmentId = "";
					if (origUri.contains("#")) {
						String[] parts = origUri.split("#");
						fragmentId = "#" + parts[1];
					}

					BosMember depMember = member.getDependency(origUri);
					if (depMember == null) {
						log.warn("rewritePointers():   Local reference with @" + attName + " value \"" + origUri + "\" failed to result in a dependency lookup. This should not happen unless the reference is to the same object.");
						continue;
					}
					File myDir = member.getFileSystemDirectory();
					File targetDir = depMember.getFileSystemDirectory();
					String path = targetDir.getAbsolutePath();
					String base = myDir.getAbsolutePath();					
					String relative = DataUtil.getRelativePath(path, base);
					if (!"".equals(relative) && !relative.endsWith("/"))
						relative += "/";
					String newUri = relative + depMember.getFileName();
					newUri += fragmentId;
					log.info("rewritePointers():   Rewriting \"" + origUri + "\" to \"" + newUri + "\"");
					ref.setAttribute(attName, newUri);
				}
			}
			log.info("rewritePointers(): Done");
		}
	}



	private void applyDitaFilter(XmlBosMember member, OutputStream outStream)
			throws SAXException, ParserConfigurationException,
			SAXNotRecognizedException, SAXNotSupportedException,
			RSuiteException, TransformerException {
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		// Because XML coming from RSuite is always fully normalized, there's no need
		// to process any DTD.
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		InputStream inStream = member.getInputStream();
		
		Source source = new SAXSource(reader, new InputSource(inStream));
		XsltTransformer transformer;
		String uri = null;
		LoggingSaxonMessageListener messageListener = context.getXmlApiManager().newLoggingSaxonMessageListener(log);
		try {
			//uri = "rsuite:/res/plugin/rsuite-dita-support/export/dita-cleanup.xsl";
			uri = "rsuite:/res/plugin/astd-plugin/xslt/export/dita-cleanup.xsl";
			// transformer = context.getXmlApiManager().getTransformer(new URI(uri));
			transformer = context.getXmlApiManager().getSaxonXsltTransformer(new URI(uri), messageListener);
		} catch (URISyntaxException e) {
			throw new RSuiteException("Unexpected exception getting transformer for URI \"" + uri + "\"");
		}

		try {
			transformer.setSource(source);
			Serializer destination = new Serializer();
			destination.setOutputStream(outStream);
			DocumentType docType = member.getElement().getOwnerDocument().getDoctype();
			if (docType != null) {
				destination.setOutputProperty(Serializer.Property.DOCTYPE_PUBLIC,docType.getPublicId());
				destination.setOutputProperty(Serializer.Property.DOCTYPE_SYSTEM,docType.getSystemId());				
			}
			transformer.setDestination(destination);
			transformer.transform();
		} catch (SaxonApiException e) {
			throw new RSuiteException("SaxonApiException exception doing transform: " + e.getMessage());			
		}
	}

	/* (non-Javadoc)
	 * @see com.reallysi.tools.dita.BosVisitor#visit(com.reallysi.tools.dita.DitaBosMember)
	 */
	public void visit(DitaBosMember ditaBosMember) throws RSuiteException {
		visit((XmlBosMember)ditaBosMember);
	}

	/* (non-Javadoc)
	 * @see com.reallysi.tools.dita.BosVisitor#visit(com.reallysi.tools.dita.DitaMapBosMember)
	 */
	public void visit(DitaMapBosMember bosMember) throws RSuiteException {
		visit((DitaBosMember)bosMember);
		
	}

}
