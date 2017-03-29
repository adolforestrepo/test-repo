<?xml version="1.0" encoding="UTF-8"?>
<!-- =====================================
     RSuite-specific element types used for
     holding manageable object metadata 
     ===================================== -->
     
<!-- ===================================== 
     RSuite-Only Elements 
     =====================================
-->

<!ELEMENT RSUITE:METADATA
  ANY
 >
<!ATTLIST RSUITE:METADATA
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:SYSTEM
  ANY
 >
<!ATTLIST RSUITE:SYSTEM
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:LASTMODIFIED
  ANY
 >
<!ATTLIST RSUITE:LASTMODIFIED
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:CHECKOUT
  ANY
 >
<!ATTLIST RSUITE:CHECKOUT
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:COPYFROM
  ANY
 >
<!ATTLIST RSUITE:COPYFROM
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:CHECKOUTDATETIME
  ANY
 >
<!ATTLIST RSUITE:CHECKOUTDATETIME
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:ID
  ANY
 >
<!ATTLIST RSUITE:ID
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:USER
  ANY
 >
<!ATTLIST RSUITE:USER
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:FOLDER
  ANY
 >
<!ATTLIST RSUITE:FOLDER
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:DISPLAYNAME
  ANY
 >
<!ATTLIST RSUITE:DISPLAYNAME
  RSUITE:AUTOGENERATE
    CDATA
    #IMPLIED
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:LAYERED
  ANY
 >
<!ATTLIST RSUITE:LAYERED
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:CREATEDATE
  ANY
 >
<!ATTLIST RSUITE:CREATEDATE
  %rsuite_namespace_declarations;
 >

<!ELEMENT RSUITE:ALIAS
  ANY
 >
<!ATTLIST RSUITE:ALIAS
  %rsuite_namespace_declarations;
  RSUITE:TYPE CDATA #IMPLIED
 >

<!ELEMENT RSUITE:ALIASES
  ANY
 >
<!ATTLIST RSUITE:ALIASES
  %rsuite_namespace_declarations;
 >


<!ELEMENT RSUITE:DATA
  ANY
 >
<!ATTLIST RSUITE:DATA
  RSUITE:ID
    CDATA
    #IMPLIED
  RSUITE:CHILDID
    CDATA
    #IMPLIED
  RSUITE:NAME
    CDATA
    #IMPLIED
  %rsuite_namespace_declarations;
 >


 <!-- End of declaration set -->