package org.astd.reallysi.rsuite.api.workflow;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StringUtil
{
  private static final String zeroes = "000000000000000";
  private static final String asterisks = "************************************";
  
  public static boolean equals(String[] paramArrayOfString1, String[] paramArrayOfString2)
  {
    if (((paramArrayOfString1 == null) && (paramArrayOfString2 != null)) || ((paramArrayOfString1 != null) && (paramArrayOfString2 == null))) {
      return false;
    }
    if (paramArrayOfString1.length != paramArrayOfString2.length) {
      return false;
    }
    for (int i = 0; i < paramArrayOfString1.length; i++) {
      if (!paramArrayOfString1[i].equals(paramArrayOfString2[i])) {
        return false;
      }
    }
    return true;
  }
  
  public static boolean equalsWithoutOrder(String[] paramArrayOfString1, String[] paramArrayOfString2)
  {
    if (((paramArrayOfString1 == null) && (paramArrayOfString2 != null)) || ((paramArrayOfString1 != null) && (paramArrayOfString2 == null))) {
      return false;
    }
    if (paramArrayOfString1.length != paramArrayOfString2.length) {
      return false;
    }
    HashSet localHashSet1 = new HashSet();
    HashSet localHashSet2 = new HashSet();
    String str;
    for (str : paramArrayOfString1) {
      localHashSet1.add(str);
    }
    for (str : paramArrayOfString2) {
      localHashSet2.add(str);
    }
    for (str : paramArrayOfString1) {
      if (!localHashSet2.contains(str)) {
        return false;
      }
    }
    for (str : paramArrayOfString2) {
      if (!localHashSet1.contains(str)) {
        return false;
      }
    }
    return true;
  }
  
  public static boolean equals(String paramString1, String paramString2)
  {
    if (paramString1 == null) {
      return paramString2 == null;
    }
    return paramString1.equals(paramString2);
  }
  
  public static boolean isNullOrEmpty(String paramString)
  {
    return (paramString == null) || ("".equals(paramString));
  }
  
  public static boolean isNullOrEmptyOrSpace(String paramString)
  {
    return (paramString == null) || ("".equals(paramString.trim()));
  }
  
  public static boolean hasEmptyOrSpaceString(String[] paramArrayOfString)
  {
    if (paramArrayOfString == null) {
      return true;
    }
    for (String str : paramArrayOfString) {
      if (isNullOrEmptyOrSpace(str)) {
        return true;
      }
    }
    return false;
  }
  
  public static String formatListToParameters(List<String> paramList)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("(");
    for (int i = 0; i < paramList.size(); i++)
    {
      localStringBuilder.append("\"" + (String)paramList.get(i) + "\"");
      if (i == paramList.size() - 1) {
        break;
      }
      localStringBuilder.append(",");
    }
    localStringBuilder.append(")");
    return localStringBuilder.toString();
  }
  
  public static boolean containsString(String[] paramArrayOfString, String paramString, boolean paramBoolean)
  {
    for (int i = 0; i < paramArrayOfString.length; i++) {
      if (paramBoolean == true)
      {
        if (paramArrayOfString[i].trim().equals(paramString)) {
          return true;
        }
      }
      else if (paramArrayOfString[i].trim().equalsIgnoreCase(paramString)) {
        return true;
      }
    }
    return false;
  }
  
  public static String convertToAsterisks(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    if (paramString.length() > "************************************".length()) {
      return "************************************";
    }
    return "************************************".substring(0, paramString.length());
  }
  
  public static String leftZeroPad(String paramString, int paramInt)
  {
    if (paramString == null) {
      return null;
    }
    if (paramString.length() > "000000000000000".length()) {
      return paramString;
    }
    if (paramInt <= paramString.length()) {
      return paramString;
    }
    return "000000000000000".substring(0, paramInt - paramString.length()) + paramString;
  }
  
  public static String replaceNonPrintingCharsWithSpace(String paramString)
  {
    if (isNullOrEmpty(paramString)) {
      return "";
    }
    char[] arrayOfChar = paramString.toCharArray();
    StringBuffer localStringBuffer = new StringBuffer("");
    for (int i = 0; i < arrayOfChar.length; i++) {
      if (((arrayOfChar[i] >= 0) && (arrayOfChar[i] <= '\b')) || ((arrayOfChar[i] >= '\013') && (arrayOfChar[i] <= '\f')) || ((arrayOfChar[i] >= '\016') && (arrayOfChar[i] <= ' '))) {
        localStringBuffer.append(" ");
      } else {
        localStringBuffer.append(arrayOfChar[i]);
      }
    }
    return localStringBuffer.toString();
  }
  
  public static String join(List<String> paramList)
  {
    return join(paramList, null);
  }
  
  public static String join(List<String> paramList, String paramString)
  {
    if (paramList == null) {
      return null;
    }
    StringBuilder localStringBuilder = new StringBuilder();
    Iterator localIterator = paramList.iterator();
    while (localIterator.hasNext())
    {
      String str = (String)localIterator.next();
      if ((paramString != null) && (localStringBuilder.length() > 0)) {
        localStringBuilder.append(paramString);
      }
      localStringBuilder.append(str);
    }
    return localStringBuilder.toString();
  }
  
  public static String join(String[] paramArrayOfString)
  {
    return join(paramArrayOfString, null);
  }
  
  public static String join(String[] paramArrayOfString, String paramString)
  {
    if (paramArrayOfString == null) {
      return null;
    }
    StringBuilder localStringBuilder = new StringBuilder();
    for (String str : paramArrayOfString)
    {
      if ((paramString != null) && (localStringBuilder.length() > 0)) {
        localStringBuilder.append(paramString);
      }
      localStringBuilder.append(str);
    }
    return localStringBuilder.toString();
  }
}

/* Location:           C:\Users\ibrahim\git\astd_source\java\lib\rsi-utils.jar
 * Qualified Name:     com.reallysi.tools.StringUtil
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.7.1
 */