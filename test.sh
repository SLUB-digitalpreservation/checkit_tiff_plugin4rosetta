#!/bin/bash
EXL=/exlibris/dps/d4_1/system.dir/dps-sdk-4.1.0/
echo java -cp \
"${EXL}/lib/dps-sdk-4.1.0.jar:${EXL}/dps-sdk-projects/dps-sdk-deposit/lib/log4j-1.2.14.jar:\
${EXL}/dps-sdk-projects/dps-sdk-deposit/lib/commons-codec-1.3.jar:\
${EXL}/dps-sdk-projects/dps-sdk-deposit/lib/xmlbeans-2.3.0.jar:\
/usr/share/java/commons-lang.jar:\
./SLUBTechnicalMetadataExtractorCheckItTiffPlugin.jar" \
org.slub.rosetta.dps.repository.plugin.SLUBTechnicalMetadataExtractorCheckItTiffPlugin \
$1
