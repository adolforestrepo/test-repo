package org.astd.rsuite.domain;

public enum ArticlePubCode {

  TD("TD", "T and D"),
  TPM("TPM", "The Public Manager"),
  CT("CT", "CTDO"), ;

  /**
   * Two-letter publication code of article.
   */
  private String pubCode;

  /**
   * Description of the code
   */
  private String pubDesc;

  ArticlePubCode(
      String pubCode, String pubDesc) {
    this.pubCode = pubCode;
    this.pubDesc = pubDesc;
  }

  /**
   * Returns true if the specified string is a defined pub code.
   * 
   * @param code Pub code to check.
   * @return <tt>true</tt> if <var>code</var> is a defined pub code; otherwise <tt>false</tt>.
   */
  public static boolean isPubCode(String code) {
    ArticlePubCode result = ArticlePubCode.valueOf(code);
    return (result != null);
  }

  public static String getPubDesc(String code) {
    ArticlePubCode result = ArticlePubCode.valueOf(code);
    if (result == null)
      return null;
    return result.getPubDesc();
  }

  public String getPubCode() {
    return this.pubCode;
  }

  public String getPubDesc() {
    return this.pubDesc;
  }
}
