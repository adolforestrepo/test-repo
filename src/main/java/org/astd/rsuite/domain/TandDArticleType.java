package org.astd.rsuite.domain;

/**
 * Article types used in article filenames.
 */
public enum TandDArticleType {

  FE("FE", "Features"),
  IN("IN", "Intelligence"),
  RE("RE", "Re:Search"),
  EW("EW", "Economy Watch"),
  FM("FM", "Fundamentals"),
  EN("EN", "Editor's Note"),
  TR("TR", "Trends"),
  TK("TK", "Technology"),
  LV("LV", "Long View"),
  DV("DV", "Development"),
  BP("BP", "Learning Blueprint"),
  SL("SL", "Solutions"),
  RX("RX", "Workplace Rx"),
  BK("BK", "Books"),
  BX("BX", "Books"),
  CP("CP", "Career Path"),
 SS("SS", "Special Section"),
 IF("IF", "In Focus"),
 CR("CR", "Career GPS"),
 SX("SX", "Success With Less"),
 QT("QT", "Quick Tips"),
 OW("OW", "One to Watch"),
 WE("WE", "Web Exclusive");
	

  /**
   * Two-letter code indicating the type of article
   */
  private String typeCode;

  /**
   * Description of the type
   */
  private String typeDesc;

  TandDArticleType(
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
      TandDArticleType result = TandDArticleType.valueOf(cand);
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
