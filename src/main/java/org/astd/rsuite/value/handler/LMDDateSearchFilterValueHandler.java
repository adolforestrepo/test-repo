package org.astd.rsuite.value.handler;

import java.util.Arrays;

import com.reallysi.rsuite.api.search.SearchFieldDefinition;
import com.reallysi.rsuite.api.search.SearchFieldTargetComponent;
import com.reallysi.rsuite.api.search.SearchFieldType;
import com.reallysi.rsuite.api.search.SearchFieldValueType;
import com.reallysi.rsuite.api.search.faceted.DateFacetValueHandler;
import com.reallysi.rsuite.api.search.faceted.MetadataSearchScope;

/**
 * Extend this class to offer the same search filter choices as system dates for a specific LMD date
 * search filter.
 * <p>
 * This implementation leverages RSuite's <code>DateFacetValueHandler</code> for a specific piece
 * LMD definition.
 * <p>
 * At least through RSuite 4.1.4, RSuite only calls the default constructor, meaning this class
 * should not be configured in the plugin descriptor. Rather, an extension of this class should be.
 * The extension should only provide the default constructor, and call one of this class'
 * constructor(s), which know how to pull the Java portion of this trick off. As needed, additional
 * constructors should be added herein. Not everything may be parameterized at this time.
 * <p>
 * Further, to have complete control over the metadata scope and the search field value type, one
 * must use <code>SearchFieldDefinition.constructSearchFieldDefinition()</code>.
 * <p>
 * Here's the plugin descriptor portion of the trick. Some of these attribute values absolutely do
 * matter. This example assumes PlannedPubDateSearchFilterValueHandler is a subclass of this class.
 * 
 * <pre>
 * &lt;facet name="plannedPubDate" 
 *     label="Planned Pub Date" 
 *     controlType="daterange" 
 *     maxDisplay="10" 
 *     width="20" 
 *     allowMultiple="false" 
 *     exactMatch="false" 
 *     allowUserEntry="true" 
 *     omitValues="false" 
 *     metadataScope="global"
 *     valueHandler="com.acme.rsuite.value.handler.PlannedPubDateSearchFilterValueHandler" /&gt;
 * </pre>
 * <p>
 * Because the value handler provides an instance of <code>SearchFieldDefinition</code>, a search
 * field definition is not required in the plugin descriptor.
 * 
 * @since RSuite 4.1.4 (RCS-2976)
 */
public class LMDDateSearchFilterValueHandler
    extends DateFacetValueHandler {

  /**
   * Construct a LMD data search filter value handler that is a filter but does not have a data type
   * and should not be cached.
   * 
   * @param searchFilterName
   * @param description
   * @param corelatedLmdName
   * @param valueType
   * @param metadataSearchScope
   */
  public LMDDateSearchFilterValueHandler(
      String searchFilterName, String description, String corelatedLmdName,
      SearchFieldValueType valueType, MetadataSearchScope metadataSearchScope) {
    super(searchFilterName, SearchFieldDefinition.constructSearchFieldDefinition(searchFilterName,
        null, // data type name
        description, SearchFieldType.LMD_NAME_VALUE, valueType, corelatedLmdName, 0, // cache in
                                                                                     // seconds
        true, // is a facet?
        Arrays.asList(SearchFieldTargetComponent.constructLmdSearchFieldTargetComponent(
            corelatedLmdName))), metadataSearchScope);

  }

}
