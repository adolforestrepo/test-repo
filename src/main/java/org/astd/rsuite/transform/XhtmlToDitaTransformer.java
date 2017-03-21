package org.astd.rsuite.transform;

import java.util.Date;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.utils.MOUtils;
import org.astd.rsuite.utils.TempFileUtils;
import org.astd.rsuite.utils.UserUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.TransformationDirection;
import com.reallysi.rsuite.api.transformation.TransformationContext;
import com.reallysi.rsuite.api.transformation.transformers.BaseXSLTTransformer;
import com.reallysi.tools.dita.DitaUtil;
import com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils;

/**
 * Custom transformer serving different purposes.
 * It can instantiated to apply different transformations, based on the XSLT resource passed as a parameter to the constructor.
 */
public class XhtmlToDitaTransformer
    extends BaseXSLTTransformer
    implements ProjectConstants {

  /**
   * Means to server log.
   */
  private static final Log log = LogFactory.getLog(XhtmlToDitaTransformer.class);

  /**
   * Construct an instance of this transformer.
   * 
   * @param context
   * @param sourceMo
   */
  public XhtmlToDitaTransformer(
      TransformationContext context, ManagedObject sourceMo) {
    super(log, context, sourceMo, "result", "application/xml", "result.xml", XSL_URI_XHTML_TO_DITA);
  }


  @Override
  protected void setTransformerParameters(Transformer transformer) {

    super.setTransformerParameters(transformer);

    // if config is true, then set parameters for write out
    try {
      if ("true".equals(DynamicConfigurationUtils.getSettingValue(context, UserUtils.getUser(context
          .getAuthorizationService(), DYNAMIC_CONFIGURATION_USERNAME),
          DYNAMIC_CONFIGURATION_PARAM_XSLT_AND_MERGE_ALL))) {
        transformer.setParameter("write.out.path.and.name.in", TempFileUtils
            .getTempFilePathForWriteOut(context) + "_xhtml2dita_in.html");
        transformer.setParameter("write.out.path.and.name.out", TempFileUtils
            .getTempFilePathForWriteOut(context) + "_xhtml2dita_out.xml");
      }
    } catch (RSuiteException e) {
      log.error("Error: " + e.getMessage());
    }
  }

  @Override
  public void transform(StreamSource source, StreamResult result) throws RSuiteException {

    // Time the transform
    Date start = new Date();
    log.info(ProjectMessageResource.getMessageText(
        "xhtml2dita.transformer.info.beginning.transform"));
    try {
      super.transform(source, result);
    } finally {
      log.info(ProjectMessageResource.getMessageText("xhtml2dita.transformer.info.elapsed.millis",
          new Date().getTime() - start.getTime()));
    }
  }

  /**
   * Enable caller to ask if this transformer applies.
   * 
   * @param mo
   * @param context
   * @return True if this class believes it applies to the specified MO, in the specified
   *         transformation context.
   */
  public final static boolean doesTransformerApply(TransformationContext context,
      ManagedObject mo) {
    boolean applies = false;
    String direction = context.getTransformationDirection().getName();
    try {
      /*
       * IMPROVE: At present, indicate this transform applies whenever almost any XML MO is being
       * updated. Likely, we'll need to be more selective later. Currently restrict it to non-DITA
       * maps. Should be restricted further.
       */
      applies = mo != null && TransformationDirection.TO_INTERNAL.getName().equalsIgnoreCase(
          direction) && !mo.isNonXml() && !DitaUtil.isDitaMap(mo.getElement());
      log.info(ProjectMessageResource.getMessageText("xhtml2dita.transformer.info.applies.result",
          direction, MOUtils.getDisplayNameQuietly(mo), mo.getId(), applies));
    } catch (Exception e) {
      log.warn(ProjectMessageResource.getMessageText(
          "xhtml2dita.transformer.warn.unable.to.determine.applicability", direction, MOUtils
              .getDisplayNameQuietly(mo), mo.getId()));
    }
    return applies;
  }

}
