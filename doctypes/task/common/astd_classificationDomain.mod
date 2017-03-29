<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Classification Domain
     
     Defines specializations of data for classifying components
     of publications (topics, figures, tables, etc.).
     
     Copyright (c) 2009 American Society for Training and Development
     
     ============================================================= -->

<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->

<!ENTITY % classification      "classification" >
<!ENTITY % taxonomy-field      "taxonomy-field" >
<!ENTITY % taxonomy-value      "taxonomy-value" >


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->


<!ENTITY % classification.content 
  "((%taxonomy-field;)*
   )"
>
<!ENTITY % classification.attributes
  "name
    NMTOKEN
    'classification'
  "
> 
<!ELEMENT classification
  %classification.content;
>
<!ATTLIST classification
  %classification.attributes;
>

<!-- Taxonomy Item: Represents a single taxonomy subject. Content is one or more values -->
<!ENTITY % taxonomy-field.content
  "(%taxonomy-value;)+
  "
>
<!ENTITY % taxonomy-field.attributes
  "name
    CDATA
    #REQUIRED
  "
> 
<!ELEMENT taxonomy-field
  %taxonomy-field.content;
>
<!ATTLIST taxonomy-field
  %taxonomy-field.attributes;
>

<!-- Taxonomy value: Represents a single value for its containing taxonomy item. -->
<!ENTITY % taxonomy-value.content
  "(#PCDATA)*
  "
>
<!ENTITY % taxonomy-value.attributes
  "name
    CDATA
    'taxonomy-value'
  "
> 
<!ELEMENT taxonomy-value
  %taxonomy-value.content;
>
<!ATTLIST taxonomy-value
  %taxonomy-value.attributes;
>





<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST classification   %global-atts;  class CDATA "+ topic/data  astd-classification-d/classification ">
<!ATTLIST taxonomy-field   %global-atts;  class CDATA "+ topic/data  astd-classification-d/taxonomy-field ">
<!ATTLIST taxonomy-value   %global-atts;  class CDATA "+ topic/data  astd-classification-d/taxonomy-value ">


<!-- ================== End Classification Domain ==================== -->