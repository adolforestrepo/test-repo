package com.reallysi.tools.dita.conversion.beans;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.conversion.TransformationOptions;

/**
 * Convert MS Word docx file into DITA XML.
 * 
 * FIXME: Get rid of org.dita4publshers.rsuite.workflow.actions.beans.TransformSuppportBean
 * and replace with use of rsuite-dita-ot-15 version.
 */
public class ATDDocx2XmlBean extends TransformSupportBean {

	// NOTE: These are copied from XmlApiManagerImpl in RSuite code.
	public static final String RSUITE_SERVERURL_XSLT_PARAMETER = "rsuite.serverurl";
	public static final String RSUITE_SESSIONKEY_XSLT_PARAMETER = "rsuite.sessionkey";
		
	
	private String styleMapUri;
	private String rootMapUrl;
	private static Log log = LogFactory.getLog(ATDDocx2XmlBean.class);
	private String displayName = "";
	
    /**
     * Construct new docx-to-xml converter.
     * @param   context             RSuite remote api context.
     * @param   xsltUriString       DOX XSLT to use.
     * @param   styleMapUriString   Style mapping file.
     * @param   rootMapUrl          Root map.
     * @throws  RSuiteException     If an error occurs.
     */
	public ATDDocx2XmlBean(
			ExecutionContext context, 
			URI xsltUri, 
			String styleMapUriString, 
			String rootMapUrl, 
			String displayName) 
					throws RSuiteException {
		super(context, xsltUri);
		
		this.styleMapUri = styleMapUriString;		
		this.rootMapUrl = rootMapUrl;
		this.displayName = displayName;
	}

	private void unzip(File zipInFile, File outputDir) throws IOException{
		
		ZipFile zipFile = new ZipFile(zipInFile);
		ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipInFile));
		ZipEntry entry = (ZipEntry) zipInputStream.getNextEntry();
		
		File curOutDir = outputDir;
		while (entry != null) {
		
			if (entry.isDirectory()) {
				// This is not robust, just for demonstration purposes.
				curOutDir = new File(curOutDir, entry.getName());
				curOutDir.mkdirs();
				continue;
			}
			
			File outFile = new File(curOutDir, entry.getName());
			File tempDir = outFile.getParentFile();
			if (!tempDir.exists()) tempDir.mkdirs();
			outFile.createNewFile();
			BufferedOutputStream outstream = new BufferedOutputStream(new FileOutputStream(outFile));

			int n;
		 	byte[] buf = new byte[1024];
			while ((n = zipInputStream.read(buf, 0, 1024)) > -1)
                outstream.write(buf, 0, n);
			outstream.flush();
			outstream.close();
			zipInputStream.closeEntry();
			entry = zipInputStream.getNextEntry();
		}
		zipInputStream.close();
			
		zipFile.close();
	}
	
	/**
	 * Returns the SaxonLogger used to capture the transformation log. From the
	 * logger you can get the log string.
	 * @param docxFile
	 * @param resultFile
	 * @param logger 
	 * @param userParams Optional parameters to pass to the style sheet.
	 * @param options 
	 * @return
	 * @throws Exception
	 */
	public void generateXmlS9Api(
            File docxFile,
            File resultFile,
            LoggingSaxonMessageListener logger,
            Map<String, String> userParams, 
            TransformationOptions options
    ) throws RSuiteException
    {
		File tempDir = null;
		try {
			tempDir = getTempDir("generateXmlS9Api", false);
		} catch (Exception e) {
			String msg = "Failed to get a temporary directory: " + e.getMessage(); 
			logAndThrowRSuiteException(log, e, msg);
		} 

		try {
			try {
				unzip(docxFile, tempDir);
			} catch (IOException e) {
				String msg = "Failed to unzip the DOCX file" + docxFile.getAbsolutePath() + "\": " + e.getMessage();			
				logAndThrowRSuiteException(log, e, msg);
			}
	
			File documentXml = new File(new File(tempDir, "word"), "document.xml");
			if (!documentXml.exists()) {
				String msg = "Failed to find document.xml within DOCX package. This should not happen.";
				logAndThrowRSuiteException(log, null, msg);
			}
			// If necessary, rename graphics from the DOCX media dir to reflect
			// filename prefix used to ensure filenames are unique.
	        reworkGraphics(tempDir, logger, userParams);
			
			Map<String, String> params = new HashMap<String, String>();
			String skey = null;
			if (options.getSession() != null) {
				skey = options.getSession().getKey();
			}
			
			params.put(RSUITE_SESSIONKEY_XSLT_PARAMETER, skey);
			String serverUrl = "http://" + context.getRSuiteServerConfiguration().getHostName() + ":" +
					context.getRSuiteServerConfiguration().getPort();
			params.put(RSUITE_SERVERURL_XSLT_PARAMETER, serverUrl);

			
			File resultDir = resultFile.getParentFile();
			File topicsDir = new File(resultDir, "topics");
			topicsDir.mkdirs();
	
			// Copy the media folder from the DocX to the generated topics directory
			File mediaSrcDir = new File(new File(tempDir, "word"), "media");
			if (mediaSrcDir.exists()) {
				// FIXME: Parameterize this directory
				File mediaTargetDir = new File(topicsDir, "media");
				try {
					FileUtils.copyDirectory(mediaSrcDir, mediaTargetDir);
				} catch (IOException e) {
					String msg = "Failed to copy media subfolder from DOCX file" + docxFile.getAbsolutePath() + "\": " + e.getMessage();			
					logAndThrowRSuiteException(log, e, msg);
				}
				// If there is a configured filename prefix then rename each file in the copied
				// media directory to use the prefix:
				
				try {
					params.put("mediaDirUri", mediaTargetDir.toURI().toURL().toExternalForm());
				} catch (MalformedURLException e) {
					throw new RuntimeException("Unexpected MalformedURLException:  " + e.getMessage());
				}
			}
			
	
			params.put("styleMapUri", styleMapUri);
			try {
				params.put("outputDir", resultDir.toURI().toURL().toExternalForm());
			} catch (MalformedURLException e) {
				throw new RuntimeException("Unexpected MalformedURLException:  " + e.getMessage());
			}
			params.put("rootMapUrl", rootMapUrl);
			if (userParams != null)
				params.putAll(userParams);
			// params.put("debug", "true");
			
			applyTransform(this.displayName, documentXml, resultFile, params, options, log);
		} catch (RSuiteException t) {
			throw t;
		} finally {
			try {
				FileUtils.deleteDirectory(tempDir);
			} catch (Throwable e) {
				log.error(e.getClass().getSimpleName() + " deleting temporary directory \"" + tempDir.getAbsolutePath() + "\".",e);
			}
		}
    }

    private static final String MEDIA_PATH =
        "word" + File.separator + "media";

    private void reworkGraphics(
            File dir,
            LoggingSaxonMessageListener logger,
            Map<String, String> params
    ) throws RSuiteException {
        log.info("Attempting to rename graphics...");

        File mediaDir = new File(dir, MEDIA_PATH);
        if (!mediaDir.exists()) {
            log.info(
                "DOCX does not contain media directory, skipping renaming");
            return;
        }
        File[] fileList = mediaDir.listFiles();
        if (fileList == null) {
            log.info("DOCX media directory is empty, skipping renaming");
            return;
        }

        String prefix = params.get("graphicFileNamePrefix");
        if (prefix == null || "".equals(prefix)) {
            log.info("graphicFileNamePrefix not set, skipping renaming");
            return;
        }

        // Rename graphic filenames
        // It is up to the Word-to-DITA transform to generate
        // image references that reflect the prefix correctly.
        for (File f : fileList) {
            if (f.isDirectory()) {
                continue;
            }
            String basename = f.getName();
            String newBasename = prefix + basename;
            File newFile = new File(f.getParentFile(), newBasename);
            if (!f.renameTo(newFile)) {
                logAndThrowRSuiteException(log, null,
                        "Unable to rename \""+f+"\" to \""+newFile+"\"");
            }
        }
    }

}
