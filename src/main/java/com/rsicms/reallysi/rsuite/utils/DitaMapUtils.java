package com.reallysi.rsuite.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ContentAssemblyItemFilter;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.DitaMapContainerItemFilter;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.conversion.ToDitaTransformationOptions;
import com.reallysi.tools.dita.conversion.TransformationOptions;
import com.reallysi.tools.dita.conversion.beans.TransformSupportBean;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

import net.sf.saxon.s9api.Serializer;

/**
 * Methods to work with DITA maps.
 */
public class DitaMapUtils {


    public static ManagedObject getMapMoForContainer(ExecutionContext context, Log log, List<ManagedObject> moList, 
    		String xsltUrlString, String caNodeMoId, String forceNewMapStr, String skey) 
    				throws Exception {
		boolean forceNewMap = false;
		if (forceNewMapStr != null ) {
			if ("true".equals(forceNewMapStr.toLowerCase().trim()) || "yes".equals(forceNewMapStr.toLowerCase().trim())) {
				forceNewMap = true;
			}
		}

		// NOTE: Throws an exception is there is no caNodeId param and no context items.
		ManagedObject mo = getContainer(context, log, moList, caNodeMoId); 
		if (mo == null) {
			// Must be a content MO
			mo = context.getManagedObjectService().
					getManagedObject(context.getAuthorizationService().getSystemUser(), moList.get(0).getId());
			if (mo.isNonXml()) {
				throw new Exception("Managed object [" + mo.getId() + "] is a non-XML MO. Cannot process it.");
			}
			Element elem = mo.getElement();
			if (!DitaUtil.isDitaMap(elem)) {
				throw new Exception("Managed object [" + mo.getId() + "] is an XML managed object but is not a DITA map. Cannot process it.");
			}
			log.info("Incoming context object is a map, no need to generate a new one.");
			return mo;
		}
		
		// At this point, the context MO is a content assembly. See if there is already a map for it.
		// If not, generate one.
		ContentAssemblyNodeContainer container = RSuiteUtils.getContentAssemblyNodeContainer(context, 
		                                                                                     context.getAuthorizationService().getSystemUser(),
		                                                                                     mo.getId());
		List<ManagedObject> mapMos = getMapMOs(context,
		                                       context.getAuthorizationService().getSystemUser(),
		                                       container);
		log.info("Map list size is " + mapMos.size());
		if (mapMos.size() == 0) {
			mo = generateMapForCa(context, log, container, xsltUrlString, skey);
		} else if (!forceNewMap && mapMos.size() == 1) {
			// If there is exactly one map, use it if map creation is not forced.
			log.info("There is exactly one map in the container and map creation is not forced, using existing map.");
			mo = mapMos.get(0);
		} else {
			if (forceNewMap) {
				log.info("Found existing map for content assembly but forceNewMap parameter set to true, so regenerating...");
				mo = generateMapForCa(context, log, container, xsltUrlString, skey);
			} else {
				mo = mapMos.get(0);
				if (mapMos.size() > 1) {
					log.warn("Found " + mapMos.size() + " managed objects that are maps in the folder. Only one will be output.");
					log.warn("  MO IDs:");
					int i = 0;
					for (ManagedObject mapMo : mapMos) {
						log.warn(i++ + "   [" + mapMo.getId() + "] " + mapMo.getDisplayName());
					}
					log.warn("Using first MO [" + mo.getId() + "]");
				}
			}
			
		}
        return mo;
    }

    public static List<ManagedObject> getMapMOs(
			ExecutionContext context,
			User user,
			ContentAssemblyNodeContainer container) throws RSuiteException {
		List<? extends ContentAssemblyItem> mapItems = new ArrayList<ContentAssemblyItem>();
		ContentAssemblyItemFilter ditaMapFilter = new DitaMapContainerItemFilter(context);
		mapItems = context.getContentAssemblyService()
				.getChildObjectsFiltered(user, container, ditaMapFilter);
		List<ManagedObject> mapMOs = new ArrayList<ManagedObject>();
		for (ContentAssemblyItem item : mapItems) {
			if (item.getObjectType().equals(ObjectType.MANAGED_OBJECT_REF)) {
				ManagedObject mo = context.getManagedObjectService()
						.getManagedObject(user, item.getId());
				try {
					mo = RSuiteUtils.getRealMo(context, user, mo);
				} catch (Exception e) {
					// Any exception here is probably the result of a deleted
					// MO or some other data problem that we don't want to hose
					// up this process.
				}
				mapMOs.add(mo);
			}
		}
		
		//If didn't find a map in the folder, look to find a map associated with the folder moid, as generated by the ck dita topic editor plugin   
		if (mapMOs.size() == 0) {
			ManagedObject caMo = context.getManagedObjectService().getManagedObject(user, container.getId());
			caMo = RSuiteUtils.getRealMo(context, user, caMo);
			Alias[] aliases = caMo.getAliases("filename");
			String folderAlias = "";
			if (aliases.length > 0) {
				folderAlias = aliases[0].getText();
			}
			if (folderAlias.isEmpty()) {
				folderAlias = caMo.getId() + ".ditamap";
			}
			ManagedObject mapMo = context.getManagedObjectService().getObjectByAlias(user, folderAlias);
			if (mapMo != null) {
				mapMOs.add(mapMo);
			}
		}
				
		return mapMOs;
	}

	public static ManagedObject generateMapForCa(ExecutionContext context, Log log, ContentAssemblyNodeContainer caNode, String xsltUrlString, String skey) 
																				throws RSuiteException {
		ManagedObject mapMo = null;
    	try {
    		User user = context.getAuthorizationService().getSystemUser();
    		    		
    		String baseName = caNode.getDisplayName();
    		String alias = constructMapFilenameAlias(context, user, baseName);

    		log.info("Generating map for OT transform...");
    		mapMo = applyTransformToCa(
    				context, 
    				user, 
    				caNode, 
    				skey,
					xsltUrlString, 
					baseName, 
					alias,
					"false",
					log);
        	
    		
    	} catch (Exception e) {
     		throw new RSuiteException(0, "Exception applying transform to content assembly: " + e.getMessage(), e);
    	}
		return mapMo;
	}

	public static String constructMapFilenameAlias(
			ExecutionContext context, 
			User user,
			String baseName) throws RSuiteException {
		// If CA node is a generic node, then look up to the ancestor publication node
		// to get the appropriate display name.
		// This is a little weak as it could result in name collitions,
		// but's probably sufficient.
		String alias = baseName.replaceAll("[\\s,\"\'\u2018\u2019\u201C\u201D]", "_") + ".ditamap";
		return alias;
	}
	
	public static ManagedObject applyTransformToCa(
			ExecutionContext context,
			User user, 
			ContentAssemblyNodeContainer caNode,
			String sessionKey,
			String xsltUrlString, 
			String baseName,
			String alias,
			String debugParam,
			Log log) throws RSuiteException {
		log.info("Generating a map for a folder during OT transform using: xslt:" + xsltUrlString + " / skey:" + sessionKey);
		ManagedObject caNodeMo = context.getManagedObjectService().getManagedObject(user, caNode.getId());
		LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(log);		
		Map<String, String> params = new HashMap<String, String>();
		params.put("debug", debugParam); 
		params.put("rsuiteSessionKey", sessionKey);
		params.put("rsuiteHost", context.getRSuiteServerConfiguration().getHostName());
		params.put("rsuitePort", context.getRSuiteServerConfiguration().getPort());
		log.info("rsuiteHost=" + context.getRSuiteServerConfiguration().getHostName());
        log.info("rsuitePort=" + context.getRSuiteServerConfiguration().getPort());
		params.put("mapTitle", baseName);
		
		TransformationOptions options = new ToDitaTransformationOptions();
		options.setSaxonLogger(logger);
		try {
			options.setTransformer(context.getXmlApiManager().getSaxonXsltTransformer(new URI(xsltUrlString), logger));
		} catch (Exception e) {
			String msg = "Failed to get transformer URI: " + e.getMessage(); 
			TransformSupportBean.logAndThrowRSuiteException(log, e, msg);
		} 
	
		TransformSupportBean tsBean = new TransformSupportBean(context, xsltUrlString);
		Source source = new DOMSource(caNodeMo.getElement());
		RSuiteServerConfiguration serverConfig = context.getRSuiteServerConfiguration();
		source.setSystemId("http://" + serverConfig.getHostName() + ":" + serverConfig.getPort() + "/rsuite/rest/v1/content/" + caNodeMo.getId());
	
		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		Serializer dest = new Serializer();
		dest.setOutputStream(resultStream);
		
		String displayName = caNode.getDisplayName(); 
		File tempDir = null;
		try {
			tempDir = context.getRSuiteServerConfiguration().getTmpDir();
		} catch (Exception e) {
			String msg = "Failed to get a temporary directory: " + e.getMessage(); 
			TransformSupportBean.logAndThrowRSuiteException(log, e, msg);
		} 
	
		log.info("Transforming CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId() + "] using transform \"" + xsltUrlString + "\"...");
		tsBean.applyTransform(displayName, source, dest, params, options, log);
	
		File tempFile = new File(tempDir, alias);
		try {
			IOUtils.copy(new ByteArrayInputStream(resultStream.toByteArray()), new FileOutputStream(tempFile));
		} catch (Exception e) {
			String msg = "Failed to copy generated map to result for upload: " + e.getMessage(); 
			TransformSupportBean.logAndThrowRSuiteException(log, e, msg);
		} 
		log.info("Result saved to file " + tempFile.getAbsolutePath());
		
		ManagedObject loadedMo = loadResultToRSuite(context, user, caNode, resultStream, alias, log);
		return loadedMo;
	}

	/**
	 * @param context
	 * @param ca
	 * @param resultStream
	 * @param log
	 * @return 
	 * @throws RSuiteException 
	 */
	public static ManagedObject loadResultToRSuite(
			ExecutionContext context, 
			User user, 
			ContentAssemblyNodeContainer ca, 
			ByteArrayOutputStream resultStream, 
			String alias, 
			Log log) throws RSuiteException {
		ManagedObject existingMo = context.getManagedObjectService().getObjectByAlias(user, alias);
		ManagedObject loadedMo = null;
		if (existingMo == null) {
	    	ObjectInsertOptions insertOptions = new ObjectInsertOptions(alias, new String[] {alias,}, null, false);
	    	insertOptions.setContentAssembly(ca);
	    	ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());
	    	loadedMo = context.getManagedObjectService().load(user, loadSource, insertOptions);
	    	context.getContentAssemblyService().attach(user, ca.getId(), loadedMo, null);
	    	log.info("Transform result loaded as object [" + loadedMo.getId() + "]");
		} else {
			loadedMo = existingMo;
			ObjectUpdateOptions updateOptions = new ObjectUpdateOptions();
			ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());
			boolean isLocked = context.getManagedObjectService().isCheckedOut(user, existingMo.getId());
			if (!isLocked) {
				try {
					context.getManagedObjectService().checkOut(user, existingMo.getId());
				} catch (RSuiteException e) {
			    	log.info("Failed to check out managed object " + existingMo.getDisplayName() + " [" + existingMo.getId() + "]");
					throw e;
				}
			}
			if (context.getManagedObjectService().isCheckedOutButNotByUser(user, existingMo.getId())) {
				throw new RSuiteException(0, "Existing publication map " + existingMo.getDisplayName() + " [" + existingMo.getId() + "] checked out be a different user");
			}
			
	    	context.getManagedObjectService().update(user, existingMo.getId(), loadSource, updateOptions);
	    	log.info("Managed object  [" + existingMo.getId() + "] updated with transform result");
	    	if (!isLocked) {
	    		ObjectCheckInOptions checkInOptions = new ObjectCheckInOptions();
	    		checkInOptions.setVersionType(VersionType.MINOR);
	    		checkInOptions.setVersionNote("Generated by XSLT transform from CA " + ca.getDisplayName() + " [" + ca.getId() + "]");
	    		context.getManagedObjectService().checkIn(user, existingMo.getId(), checkInOptions); 
	    	}
			StringWriter writer = new StringWriter();
			PrintWriter messageWriter = new PrintWriter(writer);
	    	RSuiteUtils.attachIfNotInCa(
	    			context, 
	    			user, 
	    			ca, 
	    			messageWriter, 
	    			loadedMo);
            String msg = writer.toString();
            if (msg != null && !"".equals(msg.trim())) {
                log.info(msg);
            }

		}
		
		return loadedMo;
	
	}

	private static ManagedObject getContainer(ExecutionContext context, Log log, List<ManagedObject> moList, String caNodeMoId)
			throws Exception {
		if (caNodeMoId == null || caNodeMoId.isEmpty()) {
			if (moList.size() == 0) {
				throw new Exception(caNodeMoId + " not set and no managed objects in the context. Nothing to do.");
			}
			
			if (moList.size() > 1) {
				log.warn("Multiple MOs in the context. Processing the first one");			
			}
			ManagedObject mo = moList.get(0);
			caNodeMoId = mo.getId();
		}

		ContentAssemblyNodeContainer container = RSuiteUtils.getContentAssemblyNodeContainer(context,
		                                                                                     context.getAuthorizationService().getSystemUser(),
		                                                                                     caNodeMoId);
		if (container == null) {
			return null;
		}
		
		return context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), 
		                                                          container.getId());
	}
	

	public static String getMapAliasForCaNode(
			ExecutionContext context,
			User user, 
			ContentAssemblyItem ca) throws RSuiteException {
		String baseName = ca.getDisplayName();
		return constructMapFilenameAlias(context, user, baseName);
	}
	

}
