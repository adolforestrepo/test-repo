<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Figure Specializations
     
     Contains specializations of <section> that are used across
     ASTD-specific topic types.
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->



<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 
 <!ENTITY % example-table "example-table" >
 <!ENTITY % tool-table "tool-table" >
 <!ENTITY % worksheet-table "worksheet-table" >

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->


<!ENTITY % tool-table.content     "(%tbl.table.mdl;)">
<!ENTITY % tool-table.attributes
       "frame           (top|bottom|topbot|all|sides|none| 
                         -dita-use-conref-target)               #IMPLIED
        colsep          %yesorno;                               #IMPLIED
        rowsep          %yesorno;                               #IMPLIED
        %tbl.table.att;
        %bodyatt;
        %dita.table.attributes;">
<!ELEMENT tool-table    %tool-table.content;>
<!ATTLIST tool-table    %tool-table.attributes;>

<!ENTITY % worksheet-table.content     "(%tbl.table.mdl;)">
<!ENTITY % worksheet-table.attributes
       "frame           (top|bottom|topbot|all|sides|none| 
                         -dita-use-conref-target)               #IMPLIED
        colsep          %yesorno;                               #IMPLIED
        rowsep          %yesorno;                               #IMPLIED
        %tbl.table.att;
        %bodyatt;
        %dita.table.attributes;">
<!ELEMENT worksheet-table    %worksheet-table.content;>
<!ATTLIST worksheet-table    %worksheet-table.attributes;>

<!ENTITY % example-table.content     "(%tbl.table.mdl;)">
<!ENTITY % example-table.attributes
       "frame           (top|bottom|topbot|all|sides|none| 
                         -dita-use-conref-target)               #IMPLIED
        colsep          %yesorno;                               #IMPLIED
        rowsep          %yesorno;                               #IMPLIED
        %tbl.table.att;
        %bodyatt;
        %dita.table.attributes;">
<!ELEMENT example-table    %example-table.content;>
<!ATTLIST example-table    %example-table.attributes;>



<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST example-table   %global-atts;  class CDATA "- topic/table astd_table-d/example-table "        >
<!ATTLIST tool-table      %global-atts;  class CDATA "- topic/table astd_table-d/tool-table "        >
<!ATTLIST worksheet-table %global-atts;  class CDATA "- topic/table astd_table-d/worksheet-table "        >

 
<!-- ================== End Declaration Set  ======================== -->