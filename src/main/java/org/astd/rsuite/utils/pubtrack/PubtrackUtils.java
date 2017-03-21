package org.astd.rsuite.utils.pubtrack;

import static org.astd.rsuite.utils.pubtrack.PubtrackUtils.CriterionType.EQUALITY;
import static org.astd.rsuite.utils.pubtrack.PubtrackUtils.CriterionType.GREATER_THAN_OR_EQUAL;
import static org.astd.rsuite.utils.pubtrack.PubtrackUtils.CriterionType.LESS_THAN_OR_EQUAL;
import static org.astd.rsuite.utils.pubtrack.PubtrackUtils.PubtrackMappingProcessColumn.PUBTRACK_METADATA_NAME;
import static org.astd.rsuite.utils.pubtrack.PubtrackUtils.PubtrackMappingProcessColumn.PUBTRACK_METADATA_VALUE;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * A collection of static utility methods related to Pubtrack.
 */
public class PubtrackUtils {

  private static final String EMPTY_SPACE = " ";
  private static final String DOT = ".";

  private static final String SIMPLE_HQL_PROCESS_TABLE = "Process";
  private static final String SIMPLE_HQL_PROCESS_TABLE_INSTANCE = "p";
  private static final String SIMPLE_HQL_SELECT = "from ".concat(SIMPLE_HQL_PROCESS_TABLE).concat(
      EMPTY_SPACE).concat(SIMPLE_HQL_PROCESS_TABLE_INSTANCE).concat(" where");

  public static final String PUBTRACK_PROCESS_INSTANCE = SIMPLE_HQL_PROCESS_TABLE_INSTANCE.concat(
      DOT);

  /**
   * 
   */
  public enum PubtrackMappingProcessColumn {
    PUBTRACK_PROCESS_ID("processId"),
    PUBTRACK_EXTERNAL_ID("externalId"),
    PUBTRACK_DATE_STARTED("dtStarted"),
    PUBTRACK_DATE_COMPLETED("dtCompleted"),
    PUBTRACK_PROCESS_NAME("name"),
    PUBTRACK_METADATA_NAME("metaData.name"),
    PUBTRACK_METADATA_VALUE("metaData.value");

    private String columnName;

    private PubtrackMappingProcessColumn(
        String columnName) {
      this.columnName = columnName;
    }

    public String getColumnName() {
      return columnName;
    }
  }

  /**
   * Define available comparison operators
   */
  public enum CriterionType {
    EQUALITY,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL
  }

  public enum PubtrackResultOrder {
    ASC,
    DESC
  }

  /**
   * Quote the provided value.
   * 
   * @param val The value to quote.
   * @return The quoted value.
   */
  private static String getQuotedValue(String val) {
    return new StringBuilder("'").append(val).append("'").toString();
  }

  /**
   * Add a criterion to the provided query.
   * 
   * @param query
   * @param metadataCriterion
   * @param name
   * @param criterionType
   * @param values
   */
  public static void addCriterion(StringBuilder query, boolean metadataCriterion, String name,
      CriterionType criterionType, String... values) {
    if (query != null && StringUtils.isNotBlank(name) && values != null && values.length > 0) {

      if (query.length() > SIMPLE_HQL_SELECT.length()) {
        query.append(" and ");
      }

      if (metadataCriterion) {
        query.append(EMPTY_SPACE).append(PUBTRACK_PROCESS_INSTANCE.concat(PUBTRACK_METADATA_NAME
            .getColumnName()).concat(" = ")).append(getQuotedValue(name)).append(" and ".concat(
                PUBTRACK_PROCESS_INSTANCE).concat(PUBTRACK_METADATA_VALUE.getColumnName()).concat(
                    " = "));
      } else {
        query.append(EMPTY_SPACE).append(PUBTRACK_PROCESS_INSTANCE.concat(name));
      }

      switch (criterionType) {
        case EQUALITY:
          if (values.length == 1) {
            query.append(" = ").append(getQuotedValue(values[0]));
          } else {
            query.append(" in (");
            boolean first = true;
            for (String val : values) {
              if (first)
                first = false;
              else
                query.append(", ");
              query.append(getQuotedValue(val));
            }
            query.append(")");
          }
          break;
        case GREATER_THAN_OR_EQUAL:
          query.append(" >= ").append(getQuotedValue(values[0]));
          break;
        case LESS_THAN_OR_EQUAL:
          query.append(" <= ").append(getQuotedValue(values[0]));
          break;
      }

    }
  }

  /**
   * Add an equality criterion to the provided query.
   * 
   * @param query Query to add to.
   * @param metadataCriterion Submit true for process or metadata criterion.
   * @param name Column name.
   * @param values Column value(s).
   */
  public static void addEqualityCriterion(StringBuilder query, boolean metadataCriterion,
      String name, String... values) {
    addCriterion(query, metadataCriterion, name, EQUALITY, values);
  }

  /**
   * Add an equality criterion to the provided query.
   * 
   * @param query Query to add to.
   * @param metadataCriterion Submit true for process or metadata criterion.
   * @param name Column name.
   * @param values Column value(s).
   */
  public static void addEqualityCriterion(StringBuilder query, boolean metadataCriterion,
      String name, List<String> values) {
    if (values != null) {
      addEqualityCriterion(query, metadataCriterion, name, values.toArray(new String[0]));
    }
  }

  /**
   * Add a greater than or equal criterion to the provided query.
   * 
   * @param query
   * @param metadataCriterion
   * @param name
   * @param value
   */
  public static void addGreaterThanOrEqualCriterion(StringBuilder query, boolean metadataCriterion,
      String name, String value) {
    addCriterion(query, metadataCriterion, name, GREATER_THAN_OR_EQUAL, value);
  }

  /**
   * Add a less than or equal criterion to the provided query.
   * 
   * @param query
   * @param metadataCriterion
   * @param name
   * @param value
   */
  public static void addLessThanOrEqualCriterion(StringBuilder query, boolean metadataCriterion,
      String name, String value) {
    addCriterion(query, metadataCriterion, name, LESS_THAN_OR_EQUAL, value);
  }

  /**
   * @return The beginning of an HQL query for processes. Includes "WHERE" but not any constraints.
   */
  public static StringBuilder getLeadOffProcessQuery() {
    return new StringBuilder(SIMPLE_HQL_SELECT);
  }

  public static void addOrderByCriterion(StringBuilder query, List<PubtrackSortOrder> orderBy) {

    StringBuilder orderByConfig = new StringBuilder();
    for (PubtrackSortOrder pubtrackSortOrder : orderBy) {
      if (orderByConfig.length() > 0) {
        orderByConfig.append(", ");
      }

      StringBuilder orderByColumnsConfig = new StringBuilder();
      for (PubtrackMappingProcessColumn processColumn : pubtrackSortOrder.getProcessColumns()) {
        if (orderByColumnsConfig.length() > 0) {
          orderByColumnsConfig.append(",");
        }

        orderByColumnsConfig.append(PUBTRACK_PROCESS_INSTANCE.concat(processColumn
            .getColumnName()));
      }

      orderByConfig.append(orderByColumnsConfig);

      orderByConfig.append(EMPTY_SPACE.concat(pubtrackSortOrder.getResultOrder().toString()));
    }

    if (StringUtils.isNotEmpty(orderByConfig) && !query.toString().contains("order by")) {
      query.append(" order by ");
    }

    query.append(orderByConfig);
  }

}
