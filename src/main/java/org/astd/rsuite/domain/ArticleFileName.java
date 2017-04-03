package org.astd.rsuite.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticleFileName {

  private static final String regex = "^((TD|TPM|CT)(\\w{2})(\\d{2})(\\d{2})(\\d{2})([^.]*))(\\.[^\\s.]+)?$";
  private static final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

  private String pubCode = null;
  private String fullFileName = null;
  private String sourceFileName = null;
  private String type = null;
  private String volume = null;
  private String issue = null;
  private String sequence = null;
  private String author = null;
  private String extension = null;

  public ArticleFileName(
      String name) {
    populateFields(name);
  }

  protected void populateFields(String name) {
    Matcher matcher = pattern.matcher(name);

    if (!matcher.matches()) {
      throw new IllegalArgumentException(name + " does not match the defined naming convention");
    }

    matcher.reset();
    matcher.find();

    setFullFileName(matcher.group(0));
    sourceFileName = matcher.group(1);
    pubCode = matcher.group(2);
    type = matcher.group(3);
    volume = matcher.group(4);
    issue = matcher.group(5);
    sequence = matcher.group(6);
    author = matcher.group(7);
    extension = matcher.group(8);

    if (!checkpubtype(pubCode, type))
      throw new IllegalArgumentException("Invalid article type \"" + type + "\" for \"" + name + "\"");
  }

  /**
   * Get basename.
   */
  public String toString() {
    return getArticleBasename();
  }

  /**
   * Get article basename.
   * 
   * @return Article basename.
   */
  public String getArticleBasename() {
    return pubCode + type + volume + issue + sequence + author;
  }

  /**
   * Check TD or TPM pubcode *
   * 
   * @param pubtype
   * @param type
   * @return boolean
   */
  private boolean checkpubtype(String pubtype, String type) {

    boolean result = false;
    switch (ArticlePubCode.valueOf(pubtype)) {
      case TD:
        result = TandDArticleType.isTypeCode(type);
        break;

      case TPM:
        result = TPMArticleType.isTypeCode(type);
        break;

      case CT:
        result = CTDOArticleType.isTypeCode(type);
        break;

      default:
        break;
    }

    return result;
  }

  public String getPubCode() {
    return pubCode;
  }

  public void setPubCode(String pubCode) {
    this.pubCode = pubCode;
  }

  public String getFullFileName() {
    return fullFileName;
  }

  public void setFullFileName(String fullFileName) {
    this.fullFileName = fullFileName;
  }

  public String getSourceFileName() {
    return sourceFileName;
  }

  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getVolume() {
    return volume;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public String getSequence() {
    return sequence;
  }

  public void setSequence(String sequence) {
    this.sequence = sequence;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

}
