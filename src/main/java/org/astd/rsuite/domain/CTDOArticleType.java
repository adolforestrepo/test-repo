package org.astd.rsuite.domain;

/**
 * Article types used in article filenames.
 */
public enum CTDOArticleType {

  FE("FE", "Feature"),
  IN("IN", "Inside This Issue"),
  ST("ST", "State of Talent Development"),
  OW("OW", "TD Our Way"),
  AI("AI", "Angst Index"),
  PI("PI", "Prove it"),
  GB("GB", "Giving Back"),
  HX("HX", "Career Hacks"),
  DB("DB", "Debate"),
  CN("CN", "Confessions From the C-Suite"),
  OT("OT", "Other"), ;

  /**
   * Two-letter code indicating the type of article
   */
  private String typeCode;

  /**
   * Description of the type
   */
  private String typeDesc;

  CTDOArticleType(
      String typeCode, String typeDesc) {
    this.typeCode = typeCode;
    this.typeDesc = typeDesc;
  }

  /**
   * Returns true if the specified string is a defined type code.
   * 
   * @param cand
   * @return
   */
  public static boolean isTypeCode(String cand) {
    try {
      CTDOArticleType result = CTDOArticleType.valueOf(cand);
      return (result != null);
    } catch (Throwable t) {
      return false;
    }
  }

  public String getTypeCode() {
    return this.typeCode;
  }

  public String getTypeDesc() {
    return this.typeDesc;
  }

}
