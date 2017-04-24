<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     RSuite RSI Classification Domain
     
     Defines specializations of data for classifying components
     of publications (topics, figures, tables, etc.).
     
     
     ============================================================= -->

<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->

<!ENTITY % targetaudience      "targetaudience" >
<!ENTITY % atomtype      "atomtype" >
<!ENTITY % texttype      "texttype" >
<!ENTITY % imagetype      "imagetype" >
<!ENTITY % learningresourcetype      "learningresourcetype" >
<!ENTITY % interaction_level      "interaction_level" >


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!ENTITY % targetaudience.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % targetaudience.attributes
"
  name
    NMTOKEN
    'targetaudience'
">
<!ELEMENT targetaudience %targetaudience.content; >
<!ATTLIST targetaudience %targetaudience.attributes; >

<!ENTITY % atomtype.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % atomtype.attributes
"
  name
    NMTOKEN
    'atomtype'
">
<!ELEMENT atomtype %atomtype.content; >
<!ATTLIST atomtype %atomtype.attributes; >

<!ENTITY % texttype.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % texttype.attributes
"
  name
    NMTOKEN
    'atomtype'
">
<!ELEMENT texttype %texttype.content; >
<!ATTLIST texttype %texttype.attributes; >

<!ENTITY % imagetype.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % imagetype.attributes
"
  name
    NMTOKEN
    'atomtype'
">
<!ELEMENT imagetype %imagetype.content; >
<!ATTLIST imagetype %imagetype.attributes; >

<!ENTITY % learningresourcetype.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % learningresourcetype.attributes
"
  name
    NMTOKEN
    'atomtype'
">
<!ELEMENT learningresourcetype %learningresourcetype.content; >
<!ATTLIST learningresourcetype %learningresourcetype.attributes; >

<!ENTITY % interaction_level.content
"
  (#PCDATA |
   %keyword;)*
">
<!ENTITY % interaction_level.attributes
"
  name
    NMTOKEN
    'atomtype'
">
<!ELEMENT interaction_level %interaction_level.content; >
<!ATTLIST interaction_level %interaction_level.attributes; >

<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST targetaudience   %global-atts;  class CDATA "+ topic/data  sdClassification-d/targetaudience ">
<!ATTLIST atomtype   %global-atts;  class CDATA "+ topic/data  sdClassification-d/atomtype ">
<!ATTLIST texttype   %global-atts;  class CDATA "+ topic/data  sdClassification-d/texttype ">
<!ATTLIST imagetype   %global-atts;  class CDATA "+ topic/data  sdClassification-d/imagetype ">
<!ATTLIST learningresourcetype   %global-atts;  class CDATA "+ topic/data  sdClassification-d/learningresourcetype ">
<!ATTLIST interaction_level   %global-atts;  class CDATA "+ topic/data  sdClassification-d/interaction_level ">


<!-- ================== End Classification Domain ==================== -->