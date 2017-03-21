package org.astd.rsuite.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.utils.MOUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.TransformationDirection;
import com.reallysi.rsuite.api.transformation.ManagedObjectTransformer;
import com.reallysi.rsuite.api.transformation.TransformationContext;
import com.reallysi.rsuite.api.transformation.TransformationProvider;

/**
 * A transformation provider for this plugin. It was introduced to automatically determine when
 * incoming XHTML needs to be transformed into DITA, as is needed during save actions from the
 * CKEditor.
 */
public class ProjectTransformationProvider
    implements TransformationProvider {

  private static final Log log = LogFactory.getLog(ProjectTransformationProvider.class);

  @Override
  public List<ManagedObjectTransformer> getTransformsForMo(ManagedObject mo,
      TransformationContext context) throws RSuiteException {
    List<ManagedObjectTransformer> transformers = new ArrayList<ManagedObjectTransformer>();

    String transformName = context.getTransformationName();
    TransformationDirection direction = context.getTransformationDirection();

    log.info(ProjectMessageResource.getMessageText("transform.provider.info.request",
        transformName == null ? "unnamed" : transformName, direction.getName(), MOUtils
            .getDisplayName(mo), mo.getId()));

  /*    
   * Disabling this transformer as there is a new custom web service in place to perform the same transformation on-demand.
   * 
   * if (XhtmlToDitaTransformer.doesTransformerApply(context, mo)) {
      transformers.add(new XhtmlToDitaTransformer(context, mo));
    } */

    if (DitaToXhtmlTransformer.doesTransformerApply(context, mo)) {
      transformers.add(new DitaToXhtmlTransformer(context, mo));
    }

    return transformers;
  }

}
