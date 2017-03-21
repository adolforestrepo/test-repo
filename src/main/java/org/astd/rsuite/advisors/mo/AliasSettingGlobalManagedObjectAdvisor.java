package org.astd.rsuite.advisors.mo;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.AliasContainer;
import com.reallysi.rsuite.api.control.DefaultManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * Sets the filename of managed objects as an alias of type "filename". For XML managed objects this
 * should be on insert. For non-XML managed objects this should be on insert and update. Also sets
 * the alias "basename" to the base file name. Note that the base filename need not be unique as
 * different objects of different types may have the same base name. This provides a bit of
 * indirection between references to logical objects by base name and the specific MO or variant to
 * use for a given resolution instance.
 * <p>
 * This is an alternative to rsuite-alias-setting-mo-advisor-plugin.jar.
 */
public class AliasSettingGlobalManagedObjectAdvisor
    extends DefaultManagedObjectAdvisor
    implements ManagedObjectAdvisor {

  private static Log log = LogFactory.getLog(AliasSettingGlobalManagedObjectAdvisor.class);

  public static final String ALIAS_TYPE_FILENAME = "filename";
  public static final String ALIAS_TYPE_BASENAME = "basename";

  @Override
  public void adviseDuringInsert(ExecutionContext context,
      ManagedObjectAdvisorContext insertContext) throws RSuiteException {
    // Exclude sub-MOs
    if (insertContext.isRootObjectOfOperation()) {
      try {
        String aliasSuffix = "";
        String externalFilename = insertContext.getExternalFileName();
        if (log.isDebugEnabled()) {
          log.debug(ProjectMessageResource.getMessageText("alias.advisor.debug.processing.mo",
              "Insert", (insertContext.getNonXmlFile() != null ? "non-" : ""), insertContext
                  .getId(), externalFilename));
        }
        if (StringUtils.isNotBlank(externalFilename)) {
          // The external filename may be a path, but we only ever want
          // the base name.
          String filename = FilenameUtils.getName(externalFilename);
          String basename = FilenameUtils.getBaseName(filename);
          String ext = FilenameUtils.getExtension(filename);
          if ("".equals(ext)) {
            return;
          }
          AliasContainer aliasContainer = insertContext.getAliasContainer();
          aliasContainer.addAlias(basename + aliasSuffix + ("".equals(ext) ? "" : ".") + ext,
              ALIAS_TYPE_FILENAME);
          aliasContainer.addAlias(basename + aliasSuffix, ALIAS_TYPE_BASENAME);
        }
      } catch (Exception e) {
        log.warn("Exception encountered while adding aliases", e);
      }
    }
  }

  @Override
  public void adviseDuringUpdate(ExecutionContext context,
      ManagedObjectAdvisorContext updateContext) throws RSuiteException {
    // Exclude sub-MOs
    if (updateContext.isRootObjectOfOperation()) {
      try {
        String externalFilename = updateContext.getExternalFileName();
        if (log.isDebugEnabled()) {
          log.debug(ProjectMessageResource.getMessageText("alias.advisor.debug.processing.mo",
              "Update", (updateContext.getNonXmlFile() != null ? "non-" : ""), updateContext
                  .getId(), externalFilename));
        }
        if (StringUtils.isNotBlank(externalFilename)) {
          // Have to copy existing aliases to the alias container
          // or they won't be set on update.
          AliasContainer aliasContainer = updateContext.getAliasContainer();
          String basename = FilenameUtils.getBaseName(externalFilename);
          String filename = FilenameUtils.getName(externalFilename); // Ensure just the filename.

          String moID = updateContext.getId();
          ManagedObject mo = context.getManagedObjectService().getManagedObject(updateContext
              .getUser(), moID);
          Alias[] aliases = mo.getAliases();
          // Copy existing aliases into the alias container.
          for (Alias alias : aliases) {
            aliasContainer.addAlias(alias.getText(), alias.getType());
          }
          String aliasSuffix = "";
          String ext = FilenameUtils.getExtension(filename);
          if ("".equals(ext)) {
            return;
          }
          aliasContainer.addAlias(filename + aliasSuffix, ALIAS_TYPE_FILENAME);
          aliasContainer.addAlias(basename + aliasSuffix, ALIAS_TYPE_BASENAME);
        }
      } catch (Exception e) {
        log.warn("Exception encountered while updating aliases", e);
      }
    }
  }


}
