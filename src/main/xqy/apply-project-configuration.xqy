(:
 : Configure RSuite's MarkLogic database for use by this project.
 : <p>
 : This script is to be implemented in a manner that it may (and should) be
 : executed when the rest of the project's customizations are deployed.  ML
 : will not re-index the database if there isn't a net change to the index
 : configuration.
 :)
xquery version "1.0-ml";
import module namespace admin = "http://marklogic.com/xdmp/admin" 
		  at "/MarkLogic/admin.xqy";

(: 
 : The following is a sequence of LMD names with string values that should
 : be added to a range index.
 :)
let $lmd-to-index-as-strings := (
    "ApplicationStatus",
    "TemporaryCptCode",
    "_md5Hash")

(:
 : List of LMD names that should be indexed as date.
 :)
let $lmd-to-index-as-date := ()

(:
 : List of contextual LMD names that should be indexed as date.
 :)
let $contextual-lmd-to-index-as-date := ()

(:
 : List of LMD names that should be indexed as dateTime.
 :)
let $lmd-to-index-as-date-time := ()
    
(:
 : List of system metadata names that should be indexed as dateTime.
 :)
let $smd-to-index-as-date-time := (
    "date-created",
    "last-modified",
    "checkout-date")

let $config as element(configuration) := admin:get-configuration()
let $database-id as xs:unsignedLong := xdmp:database()
let $rsuite-system-metadata-namespace as xs:string := "http://www.rsuitecms.com/rsuite/ns/materialized-view"
let $rsuite-layered-metadata-namespace as xs:string := "http://www.rsuitecms.com/rsuite/ns/materialized-view/layered-metadata"
let $rsuite-contextual-layered-metadata-namespace as xs:string := "http://www.rsuitecms.com/rsuite/ns/materialized-view/contextual-metadata"
let $index-collation as xs:string := "http://marklogic.com/collation/"
let $maintain-value-positions-in-index as xs:boolean := fn:false()
let $invalid-index-value-behavior as xs:string := "ignore" (: reject or ignore :)

(: Enable the URI lexicon, to help with diagnostic queries :)
let $config := admin:database-set-uri-lexicon(
    $config, 
    $database-id, 
    fn:true())
    
(: Set the database's directory creation setting to manual, to improve performance :)
let $config := admin:database-set-directory-creation(
    $config, 
    $database-id, 
    "manual")

(:
 : Configure element range indexes.
 :
 : First we clear all element range indexes.  Next we add the ones the 
 : application can benefit from.
 :)
let $config := 
    admin:database-delete-range-element-index(
        $config, 
        $database-id, 
        admin:database-get-range-element-indexes(
            $config, 
            $database-id
        )
    )

(: All string layered metadata indexes :)
let $lmd-string-index-config := 
    if (fn:exists($lmd-to-index-as-strings)) then
        admin:database-range-element-index(
            "string", 
            $rsuite-layered-metadata-namespace,
            fn:string-join($lmd-to-index-as-strings, " "), 
            $index-collation,
            $maintain-value-positions-in-index,
            $invalid-index-value-behavior)
    else ()
    
(: All date layered metadata indexes :)
let $lmd-date-index-config := 
    if (fn:exists($lmd-to-index-as-date)) then
        admin:database-range-element-index(
            "date", 
            $rsuite-layered-metadata-namespace,
            fn:string-join($lmd-to-index-as-date, " "), 
            $index-collation,
            $maintain-value-positions-in-index,
            $invalid-index-value-behavior)
    else ()

(: All date contextual layered metadata indexes :)
let $contextual-lmd-date-index-config := 
    if (fn:exists($contextual-lmd-to-index-as-date)) then
        admin:database-range-element-index(
            "date", 
            $rsuite-contextual-layered-metadata-namespace,
            fn:string-join($contextual-lmd-to-index-as-date, " "), 
            $index-collation,
            $maintain-value-positions-in-index,
            $invalid-index-value-behavior)
    else ()

(: All dateTime layered metadata indexes :)
let $lmd-date-time-index-config := 
    if (fn:exists($lmd-to-index-as-date-time)) then
        admin:database-range-element-index(
            "dateTime", 
            $rsuite-layered-metadata-namespace,
            fn:string-join($lmd-to-index-as-date-time, " "), 
            $index-collation,
            $maintain-value-positions-in-index,
            $invalid-index-value-behavior)
    else ()

(: All dateTime system metadata indexes :)
let $smd-date-time-index-config := 
    if (fn:exists($smd-to-index-as-date-time)) then
        admin:database-range-element-index(
            "dateTime", 
            $rsuite-system-metadata-namespace,
            fn:string-join($smd-to-index-as-date-time, " "), 
            $index-collation,
            $maintain-value-positions-in-index,
            $invalid-index-value-behavior)
    else ()

let $config := admin:database-add-range-element-index(
    $config,
    $database-id,
    (
        $lmd-string-index-config,
        $lmd-date-index-config,
        $contextual-lmd-date-index-config,
        $lmd-date-time-index-config,
        $smd-date-time-index-config
    )
)

(: Remove all element attribute range indexes.  This project does not benefit from them yet. :)
let $config := 
    admin:database-delete-range-element-attribute-index(
        $config, 
        $database-id, 
        admin:database-get-range-element-attribute-indexes(
            $config, 
            $database-id
        )
    )

return (
    admin:save-configuration($config), 
    "Configuration changes are complete. Please monitor the database status page to determine when the indexes changes will become effective."
)

