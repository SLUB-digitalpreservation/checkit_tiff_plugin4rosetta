Plugin using checkit_tiff to validate TIFF files
================================================

you need an installed checkit_tiff and the exiftool

you could test it using 'test.sh'

== compile

* make clean
* make

== install
* copy jar-file to /operational_shared/plugins/custom/

== configuration
* check blog https://developers.exlibrisgroup.com/blog/Jpylyzer-Technical-Metadata-Extractor-Plugin
* check output of exiftool -X, alternatively check http://ns.exiftool.ca/ExifTool/1.0/
* add Mapping under "Preservation:Extractors", switch from "Global" to "Local", use
  "Custom"-Tab
* fill the fields 


