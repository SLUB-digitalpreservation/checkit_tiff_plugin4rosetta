/* 
2014-2018 by Andreas Romeyke (SLUB Dresden)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Hint: works only with checkit_tiff version 0.3.1 or higher
*/

package org.slub.rosetta.dps.repository.plugin;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.core.sdk.strings.StringUtils;
import com.exlibris.dps.sdk.techmd.MDExtractorPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;


/**
 * SLUBTechnicalMetadataExtractorCheckItTiffPlugin
 *
 * @author andreas.romeyke@slub-dresden.de (Andreas Romeyke)
 * @see com.exlibris.dps.sdk.techmd.MDExtractorPlugin 
 */
/*public class SLUBTechnicalMetadataExtractorCheckItTiffPlugin implements MDExtractorPlugin { */
public class SLUBTechnicalMetadataExtractorCheckItTiffPlugin implements MDExtractorPlugin {
    private static final ExLogger log = ExLogger.getExLogger(SLUBTechnicalMetadataExtractorCheckItTiffPlugin.class);
    private enum Checkit_tiff_versions {
        current, upcoming
    }
    private Map<Checkit_tiff_versions, String> checkit_tiff_binary_path;
    private Map<Checkit_tiff_versions, String> checkit_tiff_config_path;
    private Map<Checkit_tiff_versions, Boolean> is_checkit_tiff_valid;

    private String exiftool_binary_path;
    private List<String> extractionErrors = new ArrayList<String>();
    private List<String> validationLog = new ArrayList<String>();
    private Boolean isvalid = false;
    private Boolean iswellformed = false;
    //private boolean is_current_checkit_tiff_valid = false;
    //private boolean is_upcoming_checkit_tiff_valid = false;

    private Map<String,String> attributes = new HashMap<String, String>();
    //static final ExLogger log = ExLogger.getExLogger(SLUBTechnicalMetadataExtractorCheckItTiffPlugin.class, ExLogger.VALIDATIONSTACK);
    /** constructor */
    public SLUBTechnicalMetadataExtractorCheckItTiffPlugin() {
        log.info("SLUBTechnicalMetadataExtractorCheckItTiffPlugin instantiated");
        for (Checkit_tiff_versions v: Checkit_tiff_versions.values()) {
            is_checkit_tiff_valid.put(v, false);
        }
    }
    /** init params to configure the plugin via xml forms
     * @param initp parameter map
     */
    public void initParams(Map<String, String> initp) {
        this.checkit_tiff_binary_path.put(Checkit_tiff_versions.current, initp.get("current_checkit_tiff").trim());
        this.checkit_tiff_binary_path.put(Checkit_tiff_versions.upcoming, initp.get("upcoming_checkit_tiff").trim());
        this.checkit_tiff_config_path.put(Checkit_tiff_versions.current, initp.get("current_config_file").trim());
        this.checkit_tiff_config_path.put(Checkit_tiff_versions.upcoming, initp.get("upcoming_config_file").trim());



        this.exiftool_binary_path = initp.get("exiftool").trim();
        log.info("SLUBTechnicalMetadataExtractorCheckItTiffPlugin instantiated with "
                + "(current: "
                + " checkit_tiff_binary_path=" + checkit_tiff_binary_path.get(Checkit_tiff_versions.current)
                + " cfg=" + checkit_tiff_config_path.get(Checkit_tiff_versions.current)
                + ") | (upcoming: "
                + " checkit_tiff_binary_path=" + checkit_tiff_binary_path.get(Checkit_tiff_versions.upcoming)
                + " cfg=" + checkit_tiff_config_path.get(Checkit_tiff_versions.upcoming)
                + ")"
                + " and exiftool_binary_path=" + exiftool_binary_path);
    }

    private void parse_exiftool_output( String exiftoolxml ) {
        // see output of exiftool -X, alternatively check http://ns.exiftool.ca/ExifTool/1.0/
        Pattern p = Pattern.compile("^\\s*<([^>]+)>([^<]+)</\\1>");
        log.debug("Orig string is: '" + exiftoolxml);
        Matcher m = p.matcher(exiftoolxml);
        if (m.matches()) {
            String key = m.group(1);
            String value = m.group(2);
            log.debug("matcher: key=" + key + " value=" + value);
            attributes.put(key, value);
        }
    }

    private void check_path(String filePath, String msgPath, boolean is_executable) throws Exception {
        if (StringUtils.isEmptyString(filePath)) {
            throw new Exception(msgPath + " is empty");
        }
        Path path = Paths.get(filePath);
        if (! Files.exists( path ) || !Files.isRegularFile(path)) {
            throw new Exception(msgPath + " does not exist (" + filePath + ")");
        }
        if (is_executable && ! Files.isExecutable(path)) {
            throw new Exception(msgPath + " not executable (" + filePath + ")");
        }
    }

    @Override
    public void extract(String filePath) throws Exception {
        for (Checkit_tiff_versions v: Checkit_tiff_versions.values()) {
            check_path( checkit_tiff_binary_path.get(v), "path for (" + v.name() + ") checkit_tiff_binary", true);
            check_path( checkit_tiff_config_path.get(v), "path for (" + v.name() + ") checkit_tiff_config", false);
        }

        check_path(exiftool_binary_path, "path for exiftool_binary", true);
        validate_tiff_by_upcoming_checkit_tiff(filePath);

        /* only check against current checkit_tiff if upcoming fails */
        if (is_checkit_tiff_valid.get(Checkit_tiff_versions.upcoming) == false) {
            validate_tiff_by_current_checkit_tiff(filePath);
        }

        /* If upcoming was true, only report a is valid, if current was true. report log of upcoming, if all fail, report log for all */
        if (true == is_checkit_tiff_valid.get(Checkit_tiff_versions.upcoming)) {
            isvalid = true;
            iswellformed = true;
            extractionErrors.clear();
        } else if (true == is_checkit_tiff_valid.get(Checkit_tiff_versions.current)) {
            isvalid = true;
            iswellformed=true;
            extractionErrors.clear();
        } else {
            isvalid = false;
            iswellformed = false;
            extractionErrors = validationLog;
        }

        // exiftool output of metadata
        try {
            String execstring = this.exiftool_binary_path + " -X " + filePath;
            log.info("executing: " + execstring);
            Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            String response="";
            while (line != null) {
                log.debug(line);
                parse_exiftool_output(line.trim());
                response+=line;
                line = reader.readLine();
            }
            attributes.put("exiftool-log", response.trim());

        } catch (IOException e) {
            log.error(e);
        } catch (InterruptedException e) {
            log.error(e);
            e.printStackTrace();

        }
        attributes.put("checkit-tiff-log", validationLog.toString());

    }
    private void validate_tiff_by_checkit_tiff_version(String filePath, Checkit_tiff_versions version) throws Exception {
        try {
            String execstring = this.checkit_tiff_binary_path.get(version) + " -q " + this.checkit_tiff_config_path.get(version) + " " + filePath ;
            log.debug("executing: " + execstring);
            Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();

            while (line != null) {
                System.out.println(line);
                validationLog.add(line + System.lineSeparator());
                line = reader.readLine();
            }
            if (p.exitValue() == 0) {
                is_checkit_tiff_valid.put(version,true);
            } else { // something wrong
                is_checkit_tiff_valid.put(version, false);
            }
        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);
            //log.error("ERROR: ("+version.name()+") checkit_tiff not available, path=" + this.checkit_tiff_binary_path.get(version) + ", " + e.getMessage());
            throw new Exception("ERROR: ("+version.name()+") checkit_tiff not available, path=" + this.checkit_tiff_binary_path.get(version) + ", " + e.getMessage());
        }
    }
    private void validate_tiff_by_current_checkit_tiff(String filePath) throws Exception {
        // checkit_tiff (current)
        validate_tiff_by_checkit_tiff_version(filePath, Checkit_tiff_versions.current);
    }

    private void validate_tiff_by_upcoming_checkit_tiff(String filePath) throws Exception {
        // checkit_tiff validation (upcoming)
        validate_tiff_by_checkit_tiff_version(filePath, Checkit_tiff_versions.upcoming);
    }

    public String getAgentName() {
        log.debug("getAgentName() called");
        return "checkit_tiff";
    }

    /** get agent version and signature version calling command VERSION
     *
     * @return string with version
     */
    public String getAgent() {
        log.debug("getAgent() called");
        String response="";
        for (Checkit_tiff_versions version : Checkit_tiff_versions.values()) {
            response += (version.name() + " checkit_tiff:\n");
            try {
                String execstring = this.checkit_tiff_binary_path.get(version) + " -v";
                Process p = Runtime.getRuntime().exec(execstring);
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    log.debug(line);
                    response += line;
                    line = reader.readLine();
                }
            } catch (IOException e) {
                log.error(e);
            } catch (InterruptedException e) {
                log.error(e);
                e.printStackTrace();
            }
        }
        return response.trim();
    }

    @Override
    public String getAttributeByName(String attribute) {
      if (attributes.containsKey(attribute)) {
          return attributes.get(attribute);
      }
        return "not found";
    }

    @Override
    public List<String> getExtractionErrors() {
      return this.extractionErrors;
    }
    /* processed using:
     * exiftool -lang de -listx -s > /tmp/exif.xml
     * then using 
     * - XPath "/taginfo/table[@name='Exif::Main']/tag[not(@g1)]/@name" to find IFD0-tags
     * - XPath "/taginfo/table[@name='Exif::Main']/tag[@g1='ExifIFD']/@name" to find EXIF-tags
     *
     * exiftool -lang de -listx -s | xpath -q -e "/taginfo/table[@name='Exif::Main']/tag[not(@g1)]/@name" | sed -e "s/name=\"\(.*\)\"/available.add(\"IFD0:\1\");/"
     * exiftool -lang de -listx -s | xpath -q -e "/taginfo/table[@name='Exif::Main']/tag[@g1='ExifIFD']/@name" | sed -e "s/name=\"\(.*\)\"/available.add(\"ExifIFD:\1\");/"
     * exiftool -lang de -listx -s | xpath -q -e "/taginfo/table[@name='ICC_Profile::Main']/tag/@name" | sed -e "s/name=\"\(.*\)\"/available.add(\"ICC_Profile:\1\");/"
     * exiftool -lang de -listx -s | xpath -q -e "/taginfo/table[@name='ICC_Profile::Header']/tag/@name" | sed -e "s/name=\"\(.*\)\"/available.add(\"ICC-Header:\1\");/"
     *
     * */
    @Override
    public List<String> getSupportedAttributeNames() {
      //return new ArrayList<String>(attributes.keySet());
        List<String> available = new ArrayList<String>();
        //available.add("checkit-tiff-conf");
        available.add("checkit-tiff-log");
        //available.add("checkit-tiff-path");

        available.add("Composite:Aperture");
        available.add("Composite:CircleOfConfusion");
        available.add("Composite:DateTimeCreated");
        available.add("Composite:DigitalCreationDateTime");
        available.add("Composite:Flash");
        available.add("Composite:FocalLength35efl");
        available.add("Composite:FOV");
        available.add("Composite:HyperfocalDistance");
        available.add("Composite:ImageSize");
        available.add("Composite:LensID");
        available.add("Composite:LightValue");
        available.add("Composite:ScaleFactor35efl");
        available.add("Composite:ShutterSpeed");
        available.add("Composite:SubSecCreateDate");
        available.add("Composite:SubSecDateTimeOriginal");
        available.add("ExifIFD:A100DataOffset");
        available.add("ExifIFD:Acceleration");
        available.add("ExifIFD:AdventRevision");
        available.add("ExifIFD:AdventScale");
        available.add("ExifIFD:AffineTransformMat");
        available.add("ExifIFD:AliasLayerMetadata");
        available.add("ExifIFD:AlphaByteCount");
        available.add("ExifIFD:AlphaDataDiscard");
        available.add("ExifIFD:AlphaOffset");
        available.add("ExifIFD:AmbientTemperature");
        available.add("ExifIFD:Annotations");
        available.add("ExifIFD:ApertureValue");
        available.add("ExifIFD:ApplicationNotes");
        available.add("ExifIFD:BackgroundColorIndicator");
        available.add("ExifIFD:BackgroundColorValue");
        available.add("ExifIFD:BadFaxLines");
        available.add("ExifIFD:BatteryLevel");
        available.add("ExifIFD:BitsPerExtendedRunLength");
        available.add("ExifIFD:BitsPerRunLength");
        available.add("ExifIFD:Brightness");
        available.add("ExifIFD:BrightnessValue");
        available.add("ExifIFD:CameraElevationAngle");
        available.add("ExifIFD:CFALayout");
        available.add("ExifIFD:CFAPattern");
        available.add("ExifIFD:CFAPlaneColor");
        available.add("ExifIFD:CIP3DataFile");
        available.add("ExifIFD:CIP3Sheet");
        available.add("ExifIFD:CIP3Side");
        available.add("ExifIFD:CleanFaxData");
        available.add("ExifIFD:ClipPath");
        available.add("ExifIFD:CMYKEquivalent");
        available.add("ExifIFD:CodingMethods");
        available.add("ExifIFD:ColorCharacterization");
        available.add("ExifIFD:ColorMap");
        available.add("ExifIFD:ColorResponseUnit");
        available.add("ExifIFD:ColorSequence");
        available.add("ExifIFD:ColorSpace");
        available.add("ExifIFD:ColorTable");
        available.add("ExifIFD:ComponentsConfiguration");
        available.add("ExifIFD:CompressedBitsPerPixel");
        available.add("ExifIFD:ConsecutiveBadFaxLines");
        available.add("ExifIFD:Contrast");
        available.add("ExifIFD:Converter");
        available.add("ExifIFD:CreateDate");
        available.add("ExifIFD:CustomRendered");
        available.add("ExifIFD:DataType");
        available.add("ExifIFD:DateTimeOriginal");
        available.add("ExifIFD:Decode");
        available.add("ExifIFD:DefaultImageColor");
        available.add("ExifIFD:DeviceSettingDescription");
        available.add("ExifIFD:DigitalZoomRatio");
        available.add("ExifIFD:DotRange");
        available.add("ExifIFD:ExifImageHeight");
        available.add("ExifIFD:ExifImageWidth");
        available.add("ExifIFD:ExifVersion");
        available.add("ExifIFD:ExpandFilm");
        available.add("ExifIFD:ExpandFilterLens");
        available.add("ExifIFD:ExpandFlashLamp");
        available.add("ExifIFD:ExpandLens");
        available.add("ExifIFD:ExpandScanner");
        available.add("ExifIFD:ExpandSoftware");
        available.add("ExifIFD:Exposure");
        available.add("ExifIFD:ExposureCompensation");
        available.add("ExifIFD:ExposureIndex");
        available.add("ExifIFD:ExposureMode");
        available.add("ExifIFD:ExposureProgram");
        available.add("ExifIFD:ExposureTime");
        available.add("ExifIFD:ExtraSamples");
        available.add("ExifIFD:FaxProfile");
        available.add("ExifIFD:FaxRecvParams");
        available.add("ExifIFD:FaxRecvTime");
        available.add("ExifIFD:FaxSubAddress");
        available.add("ExifIFD:FedexEDR");
        available.add("ExifIFD:FileSource");
        available.add("ExifIFD:Flash");
        available.add("ExifIFD:FlashEnergy");
        available.add("ExifIFD:FlashpixVersion");
        available.add("ExifIFD:FNumber");
        available.add("ExifIFD:FocalLength");
        available.add("ExifIFD:FocalLengthIn35mmFormat");
        available.add("ExifIFD:FocalPlaneResolutionUnit");
        available.add("ExifIFD:FocalPlaneXResolution");
        available.add("ExifIFD:FocalPlaneYResolution");
        available.add("ExifIFD:FovCot");
        available.add("ExifIFD:FreeByteCounts");
        available.add("ExifIFD:FreeOffsets");
        available.add("ExifIFD:GainControl");
        available.add("ExifIFD:Gamma");
        available.add("ExifIFD:GDALMetadata");
        available.add("ExifIFD:GDALNoData");
        available.add("ExifIFD:GooglePlusUploadCode");
        available.add("ExifIFD:GrayResponseCurve");
        available.add("ExifIFD:HCUsage");
        available.add("ExifIFD:HeightResolution");
        available.add("ExifIFD:Humidity");
        available.add("ExifIFD:ImageByteCount");
        available.add("ExifIFD:ImageColorIndicator");
        available.add("ExifIFD:ImageColorValue");
        available.add("ExifIFD:ImageDataDiscard");
        available.add("ExifIFD:ImageDepth");
        available.add("ExifIFD:ImageFullHeight");
        available.add("ExifIFD:ImageFullWidth");
        available.add("ExifIFD:ImageHeight");
        available.add("ExifIFD:ImageHistory");
        available.add("ExifIFD:ImageID");
        available.add("ExifIFD:ImageLayer");
        available.add("ExifIFD:ImageNumber");
        available.add("ExifIFD:ImageOffset");
        available.add("ExifIFD:ImageReferencePoints");
        available.add("ExifIFD:ImageType");
        available.add("ExifIFD:ImageUniqueID");
        available.add("ExifIFD:ImageWidth");
        available.add("ExifIFD:Indexed");
        available.add("ExifIFD:INGRReserved");
        available.add("ExifIFD:InkNames");
        available.add("ExifIFD:IntergraphFlagRegisters");
        available.add("ExifIFD:IntergraphMatrix");
        available.add("ExifIFD:IntergraphPacketData");
        available.add("ExifIFD:Interlace");
        available.add("ExifIFD:ISO");
        available.add("ExifIFD:ISOSpeed");
        available.add("ExifIFD:ISOSpeedLatitudeyyy");
        available.add("ExifIFD:ISOSpeedLatitudezzz");
        available.add("ExifIFD:IT8Header");
        available.add("ExifIFD:JBIGOptions");
        available.add("ExifIFD:JPEGACTables");
        available.add("ExifIFD:JPEGDCTables");
        available.add("ExifIFD:JPEGLosslessPredictors");
        available.add("ExifIFD:JPEGPointTransforms");
        available.add("ExifIFD:JPEGProc");
        available.add("ExifIFD:JPEGQTables");
        available.add("ExifIFD:JPEGRestartInterval");
        available.add("ExifIFD:JPEGTables");
        available.add("ExifIFD:JPLCartoIFD");
        available.add("ExifIFD:Lens");
        available.add("ExifIFD:LensInfo");
        available.add("ExifIFD:LensMake");
        available.add("ExifIFD:LensModel");
        available.add("ExifIFD:LensSerialNumber");
        available.add("ExifIFD:LightSource");
        available.add("ExifIFD:MakerNoteApple");
        available.add("ExifIFD:MakerNoteCanon");
        available.add("ExifIFD:MakerNoteCasio");
        available.add("ExifIFD:MakerNoteCasio2");
        available.add("ExifIFD:MakerNoteDJI");
        available.add("ExifIFD:MakerNoteFLIR");
        available.add("ExifIFD:MakerNoteFujiFilm");
        available.add("ExifIFD:MakerNoteGE");
        available.add("ExifIFD:MakerNoteGE2");
        available.add("ExifIFD:MakerNoteHasselblad");
        available.add("ExifIFD:MakerNoteHP");
        available.add("ExifIFD:MakerNoteHP2");
        available.add("ExifIFD:MakerNoteHP4");
        available.add("ExifIFD:MakerNoteHP6");
        available.add("ExifIFD:MakerNoteISL");
        available.add("ExifIFD:MakerNoteJVC");
        available.add("ExifIFD:MakerNoteJVCText");
        available.add("ExifIFD:MakerNoteKodak10");
        available.add("ExifIFD:MakerNoteKodak11");
        available.add("ExifIFD:MakerNoteKodak1a");
        available.add("ExifIFD:MakerNoteKodak1b");
        available.add("ExifIFD:MakerNoteKodak2");
        available.add("ExifIFD:MakerNoteKodak3");
        available.add("ExifIFD:MakerNoteKodak4");
        available.add("ExifIFD:MakerNoteKodak5");
        available.add("ExifIFD:MakerNoteKodak6a");
        available.add("ExifIFD:MakerNoteKodak6b");
        available.add("ExifIFD:MakerNoteKodak7");
        available.add("ExifIFD:MakerNoteKodak8a");
        available.add("ExifIFD:MakerNoteKodak8b");
        available.add("ExifIFD:MakerNoteKodak8c");
        available.add("ExifIFD:MakerNoteKodak9");
        available.add("ExifIFD:MakerNoteKodakUnknown");
        available.add("ExifIFD:MakerNoteKyocera");
        available.add("ExifIFD:MakerNoteLeica");
        available.add("ExifIFD:MakerNoteLeica2");
        available.add("ExifIFD:MakerNoteLeica3");
        available.add("ExifIFD:MakerNoteLeica4");
        available.add("ExifIFD:MakerNoteLeica5");
        available.add("ExifIFD:MakerNoteLeica6");
        available.add("ExifIFD:MakerNoteLeica7");
        available.add("ExifIFD:MakerNoteLeica8");
        available.add("ExifIFD:MakerNoteLeica9");
        available.add("ExifIFD:MakerNoteMinolta");
        available.add("ExifIFD:MakerNoteMinolta2");
        available.add("ExifIFD:MakerNoteMinolta3");
        available.add("ExifIFD:MakerNoteMotorola");
        available.add("ExifIFD:MakerNoteNikon");
        available.add("ExifIFD:MakerNoteNikon2");
        available.add("ExifIFD:MakerNoteNikon3");
        available.add("ExifIFD:MakerNoteNintendo");
        available.add("ExifIFD:MakerNoteOlympus");
        available.add("ExifIFD:MakerNoteOlympus2");
        available.add("ExifIFD:MakerNotePanasonic");
        available.add("ExifIFD:MakerNotePanasonic2");
        available.add("ExifIFD:MakerNotePentax");
        available.add("ExifIFD:MakerNotePentax2");
        available.add("ExifIFD:MakerNotePentax3");
        available.add("ExifIFD:MakerNotePentax4");
        available.add("ExifIFD:MakerNotePentax5");
        available.add("ExifIFD:MakerNotePentax6");
        available.add("ExifIFD:MakerNotePhaseOne");
        available.add("ExifIFD:MakerNoteReconyx");
        available.add("ExifIFD:MakerNoteRicoh");
        available.add("ExifIFD:MakerNoteRicoh2");
        available.add("ExifIFD:MakerNoteRicohText");
        available.add("ExifIFD:MakerNoteSamsung1a");
        available.add("ExifIFD:MakerNoteSamsung1b");
        available.add("ExifIFD:MakerNoteSamsung2");
        available.add("ExifIFD:MakerNoteSanyo");
        available.add("ExifIFD:MakerNoteSanyoC4");
        available.add("ExifIFD:MakerNoteSanyoPatch");
        available.add("ExifIFD:MakerNoteSigma");
        available.add("ExifIFD:MakerNoteSony");
        available.add("ExifIFD:MakerNoteSony2");
        available.add("ExifIFD:MakerNoteSony3");
        available.add("ExifIFD:MakerNoteSony4");
        available.add("ExifIFD:MakerNoteSony5");
        available.add("ExifIFD:MakerNoteSonyEricsson");
        available.add("ExifIFD:MakerNoteSonySRF");
        available.add("ExifIFD:MakerNoteUnknown");
        available.add("ExifIFD:MakerNoteUnknownBinary");
        available.add("ExifIFD:MakerNoteUnknownText");
        available.add("ExifIFD:MatrixWorldToCamera");
        available.add("ExifIFD:MatrixWorldToScreen");
        available.add("ExifIFD:Matteing");
        available.add("ExifIFD:MaxApertureValue");
        available.add("ExifIFD:MDColorTable");
        available.add("ExifIFD:MDFileTag");
        available.add("ExifIFD:MDFileUnits");
        available.add("ExifIFD:MDLabName");
        available.add("ExifIFD:MDPrepDate");
        available.add("ExifIFD:MDPrepTime");
        available.add("ExifIFD:MDSampleInfo");
        available.add("ExifIFD:MDScalePixel");
        available.add("ExifIFD:MeteringMode");
        available.add("ExifIFD:Model2");
        available.add("ExifIFD:ModelTiePoint");
        available.add("ExifIFD:ModelTransform");
        available.add("ExifIFD:ModeNumber");
        available.add("ExifIFD:MoireFilter");
        available.add("ExifIFD:MSDocumentText");
        available.add("ExifIFD:MSDocumentTextPosition");
        available.add("ExifIFD:MSPropertySetStorage");
        available.add("ExifIFD:MultiProfiles");
        available.add("ExifIFD:Noise");
        available.add("ExifIFD:NumberofInks");
        available.add("ExifIFD:OceApplicationSelector");
        available.add("ExifIFD:OceIDNumber");
        available.add("ExifIFD:OceImageLogic");
        available.add("ExifIFD:OceScanjobDesc");
        available.add("ExifIFD:OffsetSchema");
        available.add("ExifIFD:OffsetTime");
        available.add("ExifIFD:OffsetTimeDigitized");
        available.add("ExifIFD:OffsetTimeOriginal");
        available.add("ExifIFD:OPIProxy");
        available.add("ExifIFD:Opto-ElectricConvFactor");
        available.add("ExifIFD:OriginalFileName");
        available.add("ExifIFD:OtherImageLength");
        available.add("ExifIFD:OtherImageStart");
        available.add("ExifIFD:OwnerName");
        available.add("ExifIFD:Padding");
        available.add("ExifIFD:PixelFormat");
        available.add("ExifIFD:PixelIntensityRange");
        available.add("ExifIFD:PixelMagicJBIGOptions");
        available.add("ExifIFD:PixelScale");
        available.add("ExifIFD:Pressure");
        available.add("ExifIFD:ProfileType");
        available.add("ExifIFD:RasterPadding");
        available.add("ExifIFD:RawFile");
        available.add("ExifIFD:RawImageSegmentation");
        available.add("ExifIFD:RecommendedExposureIndex");
        available.add("ExifIFD:RegionXformTackPoint");
        available.add("ExifIFD:RelatedSoundFile");
        available.add("ExifIFD:RowInterleaveFactor");
        available.add("ExifIFD:SampleFormat");
        available.add("ExifIFD:SamsungRawByteOrder");
        available.add("ExifIFD:SamsungRawPointersLength");
        available.add("ExifIFD:SamsungRawPointersOffset");
        available.add("ExifIFD:SamsungRawUnknown");
        available.add("ExifIFD:Saturation");
        available.add("ExifIFD:SceneCaptureType");
        available.add("ExifIFD:SceneType");
        available.add("ExifIFD:SecurityClassification");
        available.add("ExifIFD:SelfTimerMode");
        available.add("ExifIFD:SensingMethod");
        available.add("ExifIFD:SensitivityType");
        available.add("ExifIFD:SerialNumber");
        available.add("ExifIFD:Shadows");
        available.add("ExifIFD:SharedData");
        available.add("ExifIFD:Sharpness");
        available.add("ExifIFD:ShutterSpeedValue");
        available.add("ExifIFD:Site");
        available.add("ExifIFD:SMaxSampleValue");
        available.add("ExifIFD:SMinSampleValue");
        available.add("ExifIFD:Smoothness");
        available.add("ExifIFD:SonyRawFileType");
        available.add("ExifIFD:SpatialFrequencyResponse");
        available.add("ExifIFD:SpectralSensitivity");
        available.add("ExifIFD:SRawType");
        available.add("ExifIFD:StandardOutputSensitivity");
        available.add("ExifIFD:StoNits");
        available.add("ExifIFD:StripByteCounts");
        available.add("ExifIFD:StripOffsets");
        available.add("ExifIFD:StripRowCounts");
        available.add("ExifIFD:SubjectArea");
        available.add("ExifIFD:SubjectDistance");
        available.add("ExifIFD:SubjectDistanceRange");
        available.add("ExifIFD:SubjectLocation");
        available.add("ExifIFD:SubSecTime");
        available.add("ExifIFD:SubSecTimeDigitized");
        available.add("ExifIFD:SubSecTimeOriginal");
        available.add("ExifIFD:SubTileBlockSize");
        available.add("ExifIFD:T4Options");
        available.add("ExifIFD:T6Options");
        available.add("ExifIFD:T82Options");
        available.add("ExifIFD:T88Options");
        available.add("ExifIFD:TextureFormat");
        available.add("ExifIFD:TIFF-EPStandardID");
        available.add("ExifIFD:TIFF_FXExtensions");
        available.add("ExifIFD:TileByteCounts");
        available.add("ExifIFD:TileDepth");
        available.add("ExifIFD:TileOffsets");
        available.add("ExifIFD:TimeZoneOffset");
        available.add("ExifIFD:TransferRange");
        available.add("ExifIFD:Transformation");
        available.add("ExifIFD:TransparencyIndicator");
        available.add("ExifIFD:TrapIndicator");
        available.add("ExifIFD:UIC1Tag");
        available.add("ExifIFD:UIC2Tag");
        available.add("ExifIFD:UIC3Tag");
        available.add("ExifIFD:UIC4Tag");
        available.add("ExifIFD:Uncompressed");
        available.add("ExifIFD:UserComment");
        available.add("ExifIFD:USPTOMiscellaneous");
        available.add("ExifIFD:USPTOOriginalContentType");
        available.add("ExifIFD:VersionYear");
        available.add("ExifIFD:WangAnnotation");
        available.add("ExifIFD:WangTag1");
        available.add("ExifIFD:WangTag3");
        available.add("ExifIFD:WangTag4");
        available.add("ExifIFD:WarpQuadrilateral");
        available.add("ExifIFD:WaterDepth");
        available.add("ExifIFD:WB_GRGBLevels");
        available.add("ExifIFD:WhiteBalance");
        available.add("ExifIFD:WidthResolution");
        available.add("ExifIFD:WrapModes");
        available.add("ExifIFD:XClipPathUnits");
        available.add("ExifIFD:XP_DIP_XML");
        available.add("ExifIFD:YClipPathUnits");
        available.add("ExifTool:ExifToolVersion");
        available.add("exiftool-log");
        available.add("File:CurrentIPTCDigest");
        available.add("File:ExifByteOrder");
        available.add("File:FileType");
        available.add("File:MIMEType");
        available.add("ICC-header:CMMFlags");
        available.add("ICC-header:ColorSpaceData");
        available.add("ICC-header:ConnectionSpaceIlluminant");
        available.add("ICC-header:DeviceAttributes");
        available.add("ICC-header:DeviceManufacturer");
        available.add("ICC-header:DeviceModel");
        available.add("ICC-header:PrimaryPlatform");
        available.add("ICC-header:ProfileClass");
        available.add("ICC-header:ProfileCMMType");
        available.add("ICC-header:ProfileConnectionSpace");
        available.add("ICC-header:ProfileCreator");
        available.add("ICC-header:ProfileDateTime");
        available.add("ICC-header:ProfileFileSignature");
        available.add("ICC-header:ProfileID");
        available.add("ICC-header:ProfileVersion");
        available.add("ICC-header:RenderingIntent");
        available.add("ICC_Profile:AToB0");
        available.add("ICC_Profile:AToB1");
        available.add("ICC_Profile:AToB2");
        available.add("ICC_Profile:BlueMatrixColumn");
        available.add("ICC_Profile:BlueTRC");
        available.add("ICC_Profile:BToA0");
        available.add("ICC_Profile:BToA1");
        available.add("ICC_Profile:BToA2");
        available.add("ICC_Profile:BToD0");
        available.add("ICC_Profile:BToD1");
        available.add("ICC_Profile:BToD2");
        available.add("ICC_Profile:BToD3");
        available.add("ICC_Profile:CalibrationDateTime");
        available.add("ICC_Profile:CharTarget");
        available.add("ICC_Profile:ChromaticAdaptation");
        available.add("ICC_Profile:ColorantOrder");
        available.add("ICC_Profile:ColorantTableOut");
        available.add("ICC_Profile:ColorimetricIntentImageState");
        available.add("ICC_Profile:CRDInfo");
        available.add("ICC_Profile:DeviceMfgDesc");
        available.add("ICC_Profile:DeviceModelDesc");
        available.add("ICC_Profile:DeviceSettings");
        available.add("ICC_Profile:DToB0");
        available.add("ICC_Profile:DToB1");
        available.add("ICC_Profile:DToB2");
        available.add("ICC_Profile:DToB3");
        available.add("ICC_Profile:FocalPlaneColorimetryEstimates");
        available.add("ICC_Profile:Gamut");
        available.add("ICC_Profile:GrayTRC");
        available.add("ICC_Profile:GreenMatrixColumn");
        available.add("ICC_Profile:GreenTRC");
        available.add("ICC_Profile:Luminance");
        available.add("ICC_Profile:MakeAndModel");
        available.add("ICC_Profile:MediaBlackPoint");
        available.add("ICC_Profile:MediaWhitePoint");
        available.add("ICC_Profile:NamedColor");
        available.add("ICC_Profile:NamedColor2");
        available.add("ICC_Profile:NativeDisplayInfo");
        available.add("ICC_Profile:OutputResponse");
        available.add("ICC_Profile:PerceptualRenderingIntentGamut");
        available.add("ICC_Profile:PostScript2CRD0");
        available.add("ICC_Profile:PostScript2CRD1");
        available.add("ICC_Profile:PostScript2CRD2");
        available.add("ICC_Profile:PostScript2CSA");
        available.add("ICC_Profile:Preview0");
        available.add("ICC_Profile:Preview1");
        available.add("ICC_Profile:Preview2");
        available.add("ICC_Profile:ProfileCopyright");
        available.add("ICC_Profile:ProfileDescription");
        available.add("ICC_Profile:ProfileDescriptionML");
        available.add("ICC_Profile:ProfileSequenceDesc");
        available.add("ICC_Profile:ProfileSequenceIdentifier");
        available.add("ICC_Profile:PS2CRDVMSize");
        available.add("ICC_Profile:PS2RenderingIntent");
        available.add("ICC_Profile:RedMatrixColumn");
        available.add("ICC_Profile:RedTRC");
        available.add("ICC_Profile:ReflectionHardcopyOrigColorimetry");
        available.add("ICC_Profile:ReflectionPrintOutputColorimetry");
        available.add("ICC_Profile:SaturationRenderingIntentGamut");
        available.add("ICC_Profile:SceneAppearanceEstimates");
        available.add("ICC_Profile:SceneColorimetryEstimates");
        available.add("ICC_Profile:Screening");
        available.add("ICC_Profile:ScreeningDesc");
        available.add("ICC_Profile:Technology");
        available.add("ICC_Profile:UCRBG");
        available.add("ICC_Profile:VideoCardGamma");
        available.add("ICC_Profile:ViewingCondDesc");
        available.add("ICC_Profile:WCSProfiles");
        available.add("IFD0:AnalogBalance");
        available.add("IFD0:Artist");
        available.add("IFD0:AsShotICCProfile");
        available.add("IFD0:AsShotNeutral");
        available.add("IFD0:AsShotPreProfileMatrix");
        available.add("IFD0:AsShotProfileName");
        available.add("IFD0:AsShotWhiteXY");
        available.add("IFD0:BaselineExposure");
        available.add("IFD0:BaselineExposureOffset");
        available.add("IFD0:BaselineNoise");
        available.add("IFD0:BaselineSharpness");
        available.add("IFD0:BitsPerSample");
        available.add("IFD0:CalibrationIlluminant1");
        available.add("IFD0:CalibrationIlluminant2");
        available.add("IFD0:CameraCalibration1");
        available.add("IFD0:CameraCalibration2");
        available.add("IFD0:CameraCalibrationSig");
        available.add("IFD0:CameraLabel");
        available.add("IFD0:CameraSerialNumber");
        available.add("IFD0:CellLength");
        available.add("IFD0:CellWidth");
        available.add("IFD0:ColorimetricReference");
        available.add("IFD0:ColorMatrix1");
        available.add("IFD0:ColorMatrix2");
        available.add("IFD0:Compression");
        available.add("IFD0:Copyright");
        available.add("IFD0:CurrentICCProfile");
        available.add("IFD0:CurrentPreProfileMatrix");
        available.add("IFD0:DefaultBlackRender");
        available.add("IFD0:DNGAdobeData");
        available.add("IFD0:DNGBackwardVersion");
        available.add("IFD0:DNGLensInfo");
        available.add("IFD0:DNGPrivateData");
        available.add("IFD0:DNGVersion");
        available.add("IFD0:DocumentName");
        available.add("IFD0:FillOrder");
        available.add("IFD0:ForwardMatrix1");
        available.add("IFD0:ForwardMatrix2");
        available.add("IFD0:FrameRate");
        available.add("IFD0:GeoTiffAsciiParams");
        available.add("IFD0:GeoTiffDirectory");
        available.add("IFD0:GeoTiffDoubleParams");
        available.add("IFD0:GrayResponseUnit");
        available.add("IFD0:HalftoneHints");
        available.add("IFD0:HostComputer");
        available.add("IFD0:ImageDescription");
        available.add("IFD0:ImageHeight");
        available.add("IFD0:ImageSourceData");
        available.add("IFD0:ImageWidth");
        available.add("IFD0:InkSet");
        available.add("IFD0:IPTC-NAA");
        available.add("IFD0:LinearResponseLimit");
        available.add("IFD0:LocalizedCameraModel");
        available.add("IFD0:Make");
        available.add("IFD0:MakerNoteSafety");
        available.add("IFD0:MaxSampleValue");
        available.add("IFD0:MinSampleValue");
        available.add("IFD0:Model");
        available.add("IFD0:ModifyDate");
        available.add("IFD0:NewRawImageDigest");
        available.add("IFD0:OldSubfileType");
        available.add("IFD0:Orientation");
        available.add("IFD0:OriginalBestQualitySize");
        available.add("IFD0:OriginalDefaultCropSize");
        available.add("IFD0:OriginalDefaultFinalSize");
        available.add("IFD0:OriginalRawFileData");
        available.add("IFD0:OriginalRawFileDigest");
        available.add("IFD0:OriginalRawFileName");
        available.add("IFD0:PageName");
        available.add("IFD0:PageNumber");
        available.add("IFD0:PanasonicTitle");
        available.add("IFD0:PanasonicTitle2");
        available.add("IFD0:PhotometricInterpretation");
        available.add("IFD0:PlanarConfiguration");
        available.add("IFD0:Predictor");
        available.add("IFD0:PreviewApplicationName");
        available.add("IFD0:PreviewApplicationVersion");
        available.add("IFD0:PreviewColorSpace");
        available.add("IFD0:PreviewDateTime");
        available.add("IFD0:PreviewImageLength");
        available.add("IFD0:PreviewImageStart");
        available.add("IFD0:PreviewSettingsDigest");
        available.add("IFD0:PreviewSettingsName");
        available.add("IFD0:PrimaryChromaticities");
        available.add("IFD0:PrintIM");
        available.add("IFD0:ProcessingSoftware");
        available.add("IFD0:ProfileCalibrationSig");
        available.add("IFD0:ProfileCopyright");
        available.add("IFD0:ProfileEmbedPolicy");
        available.add("IFD0:ProfileHueSatMapData1");
        available.add("IFD0:ProfileHueSatMapData2");
        available.add("IFD0:ProfileHueSatMapDims");
        available.add("IFD0:ProfileHueSatMapEncoding");
        available.add("IFD0:ProfileLookTableData");
        available.add("IFD0:ProfileLookTableDims");
        available.add("IFD0:ProfileLookTableEncoding");
        available.add("IFD0:ProfileName");
        available.add("IFD0:ProfileToneCurve");
        available.add("IFD0:Rating");
        available.add("IFD0:RatingPercent");
        available.add("IFD0:RawDataUniqueID");
        available.add("IFD0:RawImageDigest");
        available.add("IFD0:RawToPreviewGain");
        available.add("IFD0:ReductionMatrix1");
        available.add("IFD0:ReductionMatrix2");
        available.add("IFD0:ReelName");
        available.add("IFD0:ReferenceBlackWhite");
        available.add("IFD0:ResolutionUnit");
        available.add("IFD0:RowsPerStrip");
        available.add("IFD0:SamplesPerPixel");
        available.add("IFD0:SEMInfo");
        available.add("IFD0:ShadowScale");
        available.add("IFD0:Software");
        available.add("IFD0:StripByteCounts");
        available.add("IFD0:StripOffsets");
        available.add("IFD0:SubfileType");
        available.add("IFD0:T6Options");
        available.add("IFD0:TargetPrinter");
        available.add("IFD0:Thresholding");
        available.add("IFD0:ThumbnailLength");
        available.add("IFD0:ThumbnailOffset");
        available.add("IFD0:TileLength");
        available.add("IFD0:TileWidth");
        available.add("IFD0:TimeCodes");
        available.add("IFD0:TransferFunction");
        available.add("IFD0:TStop");
        available.add("IFD0:UniqueCameraModel");
        available.add("IFD0:WhitePoint");
        available.add("IFD0:XPAuthor");
        available.add("IFD0:XPComment");
        available.add("IFD0:XPKeywords");
        available.add("IFD0:XPosition");
        available.add("IFD0:XPSubject");
        available.add("IFD0:XPTitle");
        available.add("IFD0:XResolution");
        available.add("IFD0:YCbCrCoefficients");
        available.add("IFD0:YCbCrPositioning");
        available.add("IFD0:YCbCrSubSampling");
        available.add("IFD0:YPosition");
        available.add("IFD0:YResolution");
        available.add("IPTC2:ApplicationRecordVersion");
        available.add("IPTC2:CopyrightNotice");
        available.add("IPTC:ApplicationRecordVersion");
        available.add("IPTC:CodedCharacterSet");
        available.add("IPTC:CopyrightNotice");
        available.add("IPTC:DateCreated");
        available.add("IPTC:DigitalCreationDate");
        available.add("IPTC:DigitalCreationTime");
        available.add("IPTC:TimeCreated");
        available.add("Photoshop:AlphaChannelsNames");
        available.add("Photoshop:CopyrightFlag");
        available.add("Photoshop:DisplayedUnitsX");
        available.add("Photoshop:DisplayedUnitsY");
        available.add("Photoshop:GlobalAltitude");
        available.add("Photoshop:GlobalAngle");
        available.add("Photoshop:IPTCDigest");
        available.add("Photoshop:PhotoshopThumbnail");
        available.add("Photoshop:XResolution");
        available.add("Photoshop:YResolution");
        available.add("System:Directory");
        available.add("System:FileAccessDate");
        available.add("System:FileInodeChangeDate");
        available.add("System:FileModifyDate");
        available.add("System:FileName");
        available.add("System:FilePermissions");
        available.add("System:FileSize");
        available.add("XMP-aux:ApproximateFocusDistance");
        available.add("XMP-aux:ImageNumber");
        available.add("XMP-aux:Lens");
        available.add("XMP-aux:LensID");
        available.add("XMP-aux:LensInfo");
        available.add("XMP-aux:SerialNumber");
        available.add("XMP-crs:AlreadyApplied");
        available.add("XMP-crs:AutoLateralCA");
        available.add("XMP-crs:AutoWhiteVersion");
        available.add("XMP-crs:Blacks2012");
        available.add("XMP-crs:BlueHue");
        available.add("XMP-crs:BlueSaturation");
        available.add("XMP-crs:CameraProfile");
        available.add("XMP-crs:CameraProfileDigest");
        available.add("XMP-crs:Clarity2012");
        available.add("XMP-crs:ColorNoiseReduction");
        available.add("XMP-crs:ColorNoiseReductionDetail");
        available.add("XMP-crs:ColorNoiseReductionSmoothness");
        available.add("XMP-crs:ColorTemperature");
        available.add("XMP-crs:Contrast2012");
        available.add("XMP-crs:ConvertToGrayscale");
        available.add("XMP-crs:CropAngle");
        available.add("XMP-crs:CropBottom");
        available.add("XMP-crs:CropConstrainToWarp");
        available.add("XMP-crs:CropLeft");
        available.add("XMP-crs:CropRight");
        available.add("XMP-crs:CropTop");
        available.add("XMP-crs:DefringeGreenAmount");
        available.add("XMP-crs:DefringeGreenHueHi");
        available.add("XMP-crs:DefringeGreenHueLo");
        available.add("XMP-crs:DefringePurpleAmount");
        available.add("XMP-crs:DefringePurpleHueHi");
        available.add("XMP-crs:DefringePurpleHueLo");
        available.add("XMP-crs:Exposure2012");
        available.add("XMP-crs:GrainAmount");
        available.add("XMP-crs:GreenHue");
        available.add("XMP-crs:GreenSaturation");
        available.add("XMP-crs:HasCrop");
        available.add("XMP-crs:HasSettings");
        available.add("XMP-crs:Highlights2012");
        available.add("XMP-crs:HueAdjustmentAqua");
        available.add("XMP-crs:HueAdjustmentBlue");
        available.add("XMP-crs:HueAdjustmentGreen");
        available.add("XMP-crs:HueAdjustmentMagenta");
        available.add("XMP-crs:HueAdjustmentOrange");
        available.add("XMP-crs:HueAdjustmentPurple");
        available.add("XMP-crs:HueAdjustmentRed");
        available.add("XMP-crs:HueAdjustmentYellow");
        available.add("XMP-crs:LensManualDistortionAmount");
        available.add("XMP-crs:LensProfileChromaticAberrationScale");
        available.add("XMP-crs:LensProfileDigest");
        available.add("XMP-crs:LensProfileDistortionScale");
        available.add("XMP-crs:LensProfileEnable");
        available.add("XMP-crs:LensProfileFilename");
        available.add("XMP-crs:LensProfileName");
        available.add("XMP-crs:LensProfileSetup");
        available.add("XMP-crs:LensProfileVignettingScale");
        available.add("XMP-crs:LuminanceAdjustmentAqua");
        available.add("XMP-crs:LuminanceAdjustmentBlue");
        available.add("XMP-crs:LuminanceAdjustmentGreen");
        available.add("XMP-crs:LuminanceAdjustmentMagenta");
        available.add("XMP-crs:LuminanceAdjustmentOrange");
        available.add("XMP-crs:LuminanceAdjustmentPurple");
        available.add("XMP-crs:LuminanceAdjustmentRed");
        available.add("XMP-crs:LuminanceAdjustmentYellow");
        available.add("XMP-crs:LuminanceSmoothing");
        available.add("XMP-crs:ParametricDarks");
        available.add("XMP-crs:ParametricHighlights");
        available.add("XMP-crs:ParametricHighlightSplit");
        available.add("XMP-crs:ParametricLights");
        available.add("XMP-crs:ParametricMidtoneSplit");
        available.add("XMP-crs:ParametricShadows");
        available.add("XMP-crs:ParametricShadowSplit");
        available.add("XMP-crs:PerspectiveAspect");
        available.add("XMP-crs:PerspectiveHorizontal");
        available.add("XMP-crs:PerspectiveRotate");
        available.add("XMP-crs:PerspectiveScale");
        available.add("XMP-crs:PerspectiveUpright");
        available.add("XMP-crs:PerspectiveVertical");
        available.add("XMP-crs:PostCropVignetteAmount");
        available.add("XMP-crs:ProcessVersion");
        available.add("XMP-crs:RawFileName");
        available.add("XMP-crs:RedHue");
        available.add("XMP-crs:RedSaturation");
        available.add("XMP-crs:Saturation");
        available.add("XMP-crs:SaturationAdjustmentAqua");
        available.add("XMP-crs:SaturationAdjustmentBlue");
        available.add("XMP-crs:SaturationAdjustmentGreen");
        available.add("XMP-crs:SaturationAdjustmentMagenta");
        available.add("XMP-crs:SaturationAdjustmentOrange");
        available.add("XMP-crs:SaturationAdjustmentPurple");
        available.add("XMP-crs:SaturationAdjustmentRed");
        available.add("XMP-crs:SaturationAdjustmentYellow");
        available.add("XMP-crs:Shadows2012");
        available.add("XMP-crs:ShadowTint");
        available.add("XMP-crs:SharpenDetail");
        available.add("XMP-crs:SharpenEdgeMasking");
        available.add("XMP-crs:SharpenRadius");
        available.add("XMP-crs:Sharpness");
        available.add("XMP-crs:SplitToningBalance");
        available.add("XMP-crs:SplitToningHighlightHue");
        available.add("XMP-crs:SplitToningHighlightSaturation");
        available.add("XMP-crs:SplitToningShadowHue");
        available.add("XMP-crs:SplitToningShadowSaturation");
        available.add("XMP-crs:Tint");
        available.add("XMP-crs:Version");
        available.add("XMP-crs:Vibrance");
        available.add("XMP-crs:VignetteAmount");
        available.add("XMP-crs:WhiteBalance");
        available.add("XMP-crs:Whites2012");
        available.add("XMP-dc:Format");
        available.add("XMP-dc:Rights");
        available.add("XMP-exif:ApertureValue");
        available.add("XMP-exif:BrightnessValue");
        available.add("XMP-exif:ColorSpace");
        available.add("XMP-exif:CompressedBitsPerPixel");
        available.add("XMP-exif:Contrast");
        available.add("XMP-exif:CustomRendered");
        available.add("XMP-exif:DateTimeDigitized");
        available.add("XMP-exif:DateTimeOriginal");
        available.add("XMP-exif:ExifImageHeight");
        available.add("XMP-exif:ExifImageWidth");
        available.add("XMP-exif:ExifVersion");
        available.add("XMP-exif:ExposureCompensation");
        available.add("XMP-exif:ExposureMode");
        available.add("XMP-exif:ExposureProgram");
        available.add("XMP-exif:ExposureTime");
        available.add("XMP-exif:FileSource");
        available.add("XMP-exif:FlashFired");
        available.add("XMP-exif:FlashFunction");
        available.add("XMP-exif:FlashMode");
        available.add("XMP-exif:FlashpixVersion");
        available.add("XMP-exif:FlashRedEyeMode");
        available.add("XMP-exif:FlashReturn");
        available.add("XMP-exif:FNumber");
        available.add("XMP-exif:FocalLength");
        available.add("XMP-exif:FocalLengthIn35mmFormat");
        available.add("XMP-exif:FocalPlaneResolutionUnit");
        available.add("XMP-exif:FocalPlaneXResolution");
        available.add("XMP-exif:FocalPlaneYResolution");
        available.add("XMP-exif:ISO");
        available.add("XMP-exif:LightSource");
        available.add("XMP-exif:MeteringMode");
        available.add("XMP-exif:NativeDigest");
        available.add("XMP-exif:Saturation");
        available.add("XMP-exif:SceneCaptureType");
        available.add("XMP-exif:SceneType");
        available.add("XMP-exif:SensingMethod");
        available.add("XMP-exif:Sharpness");
        available.add("XMP-exif:ShutterSpeedValue");
        available.add("XMP-exif:SubjectDistanceRange");
        available.add("XMP-exif:WhiteBalance");
        available.add("XMP-photoshop:ColorMode");
        available.add("XMP-photoshop:DateCreated");
        available.add("XMP-photoshop:History");
        available.add("XMP-photoshop:ICCProfileName");
        available.add("XMP-tiff:BitsPerSample");
        available.add("XMP-tiff:Compression");
        available.add("XMP-tiff:ImageHeight");
        available.add("XMP-tiff:ImageWidth");
        available.add("XMP-tiff:Make");
        available.add("XMP-tiff:Model");
        available.add("XMP-tiff:NativeDigest");
        available.add("XMP-tiff:Orientation");
        available.add("XMP-tiff:PhotometricInterpretation");
        available.add("XMP-tiff:ResolutionUnit");
        available.add("XMP-tiff:SamplesPerPixel");
        available.add("XMP-tiff:XResolution");
        available.add("XMP-tiff:YResolution");
        available.add("XMP-xmp:CreateDate");
        available.add("XMP-xmp:CreatorTool");
        available.add("XMP-xmp:MetadataDate");
        available.add("XMP-xmpMM:DerivedFrom");
        available.add("XMP-xmpMM:DerivedFromDocumentID");
        available.add("XMP-xmpMM:DerivedFromInstanceID");
        available.add("XMP-xmpMM:DerivedFromOriginalDocumentID");
        available.add("XMP-xmpMM:DocumentID");
        available.add("XMP-xmpMM:HistoryParameters");
        available.add("XMP-xmpMM:InstanceID");
        available.add("XMP-xmpMM:OriginalDocumentID");
        available.add("XMP-xmp:ModifyDate");
        available.add("XMP-x:XMPToolkit");
        return available;
    }

    @Override
    public boolean isWellFormed() {
      return this.iswellformed;
    }

    @Override
    public boolean isValid() {
        log.debug("is valid=" + this.isvalid);
        return this.isvalid;
    }
    @Override
    public String getFormatName() {
        return "TIFF";
    }

    @Override
    public String getFormatVersion() {
      return "6 (baseline + SLUB extensions)";
    }

    @Override
    public Integer getImageCount() {
        return 1; //baseline tiff holds exact one
    }

    @Override
    public String getMimeType() {
      return "image/tiff";
    }

    /** stand alone check, main file to call local installed clamd
     * @param args list of files which should be scanned
     */
    public static void main(String[] args) {
        SLUBTechnicalMetadataExtractorCheckItTiffPlugin plugin = new SLUBTechnicalMetadataExtractorCheckItTiffPlugin();
        Map<String, String> initp = new HashMap<String, String>();
        // initp.put( "checkit_tiff", "/usr/bin/checkit_tiff");
        // initp.put( "config_file", "/etc/checkit_tiff/slub.cfg");
        initp.put( "current_checkit_tiff", "/home/romeyke/git/checkit_tiff/build/checkit_tiff");
        initp.put( "current_config_file", "/home/romeyke/git/checkit_tiff/example_configs/cit_tiff6_baseline_SLUBrelaxed.cfg");
        initp.put( "upcoming_checkit_tiff", "/home/romeyke/git/checkit_tiff/build/checkit_tiff");
        initp.put( "upcoming_config_file", "/home/romeyke/git/checkit_tiff/example_configs/cit_tiff6_baseline_SLUBrelaxed.cfg");
        initp.put( "exiftool", "/usr/bin/exiftool");
        plugin.initParams( initp );
        System.out.println("Agent: '" + plugin.getAgent() + "'");
        System.out.println();
        for (String file : args) {
            try {
                plugin.extract(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("RESULT: " + plugin.isValid());
            System.out.println("ERRORMESSAGE: " + plugin.getExtractionErrors());
        }
    }
}


