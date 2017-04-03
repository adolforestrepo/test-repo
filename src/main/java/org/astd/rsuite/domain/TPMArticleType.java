package org.astd.rsuite.domain;

/**
 * Article types used in article filenames.
 */
public enum TPMArticleType {

  FE("FE", "Feature"),
  ED("FR", "From the Editor"),
  CC("CC", "Chairman’s Corner"),
  IN("IN", "Insights"),
  SH("SH", "On Our Shelf"),
  AA("AA", "Agency Application"),
  DP("DP", "Data Points"),
  BL("BL", "Bulletin"),
  HZ("HZ", "On the Horizon"),
  PP("PP", "Partner Pages"),
  HL("HL", "ATD Highlights"),
  SP("SP", "Spotlight"), ;

  /**
   * Two-letter code indicating the type of article
   */
  private String typeCode;

  /**
   * Description of the type
   */
  private String typeDesc;

  TPMArticleType(
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
      TPMArticleType result = TPMArticleType.valueOf(cand);
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
