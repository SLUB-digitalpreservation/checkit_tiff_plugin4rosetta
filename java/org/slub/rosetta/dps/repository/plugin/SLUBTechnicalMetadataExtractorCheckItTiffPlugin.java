/* 
2014 by Andreas Romeyke (SLUB Dresden)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.slub.rosetta.dps.repository.plugin;


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

/**
 * SLUBTechnicalMetadataExtractorCheckItTiffPlugin
 *
 * @author andreas.romeyke@slub-dresden.de (Andreas Romeyke)
 * @see com.exlibris.dps.sdk.techmd.MDExtractorPlugin 
 */
/*public class SLUBTechnicalMetadataExtractorCheckItTiffPlugin implements MDExtractorPlugin { */
public class SLUBTechnicalMetadataExtractorCheckItTiffPlugin implements MDExtractorPlugin {

    private String actual_checkit_tiff_binary_path;
    private String actual_checkit_tiff_config_path;
    private String upcoming_checkit_tiff_binary_path;
    private String upcoming_checkit_tiff_config_path;

    private String exiftool_binary_path;
    private List<String> extractionErrors = new ArrayList<String>();
    private List<String> validationLog = new ArrayList<String>();
    private boolean isvalid = false;
    private boolean iswellformed = false;
    private boolean is_actual_checkit_tiff_valid = false;
    private boolean is_upcoming_checkit_tiff_valid = false;

    private Map<String,String> attributes = new HashMap<String, String>();
    //static final ExLogger log = ExLogger.getExLogger(SLUBTechnicalMetadataExtractorCheckItTiffPlugin.class, ExLogger.VALIDATIONSTACK);
    /** constructor */
    public SLUBTechnicalMetadataExtractorCheckItTiffPlugin() {
        //log.info("SLUBVirusCheckPlugin instantiated with host=" + host + " port=" + port + " timeout=" + timeout);
        System.out.println("SLUBTechnicalMetadataExtractorCheckItTiffPlugin instantiated");
    }
    /** init params to configure the plugin via xml forms
     * @param initp parameter map
     */
    public void initParams(Map<String, String> initp) {
        this.actual_checkit_tiff_binary_path = initp.get("actual_checkit_tiff").trim();
        this.actual_checkit_tiff_config_path = initp.get("actual_config_file").trim();
        this.upcoming_checkit_tiff_binary_path = initp.get("upcoming_checkit_tiff").trim();
        this.upcoming_checkit_tiff_config_path = initp.get("upcoming_config_file").trim();

        this.exiftool_binary_path = initp.get("exiftool").trim();
        System.out.println("SLUBTechnicalMetadataExtractorCheckItTiffPlugin instantiated with "
                + "(actual: "
                + " checkit_tiff_binary_path=" + actual_checkit_tiff_binary_path
                + " cfg=" + actual_checkit_tiff_config_path
                + ") | (upcoming: "
                + " checkit_tiff_binary_path=" + upcoming_checkit_tiff_binary_path
                + " cfg=" + upcoming_checkit_tiff_config_path
                + ")"
                + " and exiftool_binary_path=" + exiftool_binary_path);
    }

    private void parse_exiftool_output( String exiftoolxml ) {
        // see output of exiftool -X, alternatively check http://ns.exiftool.ca/ExifTool/1.0/
        Pattern p = Pattern.compile("<([^>]+)>([^<]+)</\1>");
        Matcher m = p.matcher(exiftoolxml);
        if (m.matches()) {
            String key = m.group(1);
            String value = m.group(2);
            System.out.println("matcher: key=" + key + " value=" + value);
            attributes.put(key, value);
        }
    }

    @Override
    public void extract(String filePath) throws Exception {
        if (StringUtils.isEmptyString(actual_checkit_tiff_binary_path)) {
            //log.error("No checkit_tiff_binary_path defined. Please set the plugin parameter to hold your checkit_tiff_binary_path.");
            throw new Exception("path for (actual) checkit_tiff_binary not found");
        }
        if (StringUtils.isEmptyString(actual_checkit_tiff_config_path)) {
            //log.error("No checkit_tiff_config_path defined. Please set the plugin parameter to hold your checkit_tiff_config_path.");
            throw new Exception("path for (actual) checkit_tiff_config not found");
        }
        if (StringUtils.isEmptyString(upcoming_checkit_tiff_binary_path)) {
            //log.error("No checkit_tiff_binary_path defined. Please set the plugin parameter to hold your checkit_tiff_binary_path.");
            throw new Exception("path for (upcoming) checkit_tiff_binary not found");
        }
        if (StringUtils.isEmptyString(upcoming_checkit_tiff_config_path)) {
            //log.error("No checkit_tiff_config_path defined. Please set the plugin parameter to hold your checkit_tiff_config_path.");
            throw new Exception("path for (upcoming) checkit_tiff_config not found");
        }
        if (StringUtils.isEmptyString(exiftool_binary_path)) {
            //log.error("No checkit_tiff_config_path defined. Please set the plugin parameter to hold your checkit_tiff_config_path.");
            throw new Exception("path for exiftool_binary not found");
        }

        // checkit_tiff validation (upcoming)
        try {
            String execstring = this.upcoming_checkit_tiff_binary_path + " " + filePath + " " + this.upcoming_checkit_tiff_config_path;
            System.out.println("executing: " + execstring);
            Process p = Runtime.getRuntime().exec( execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();

            while (line != null) {
                System.out.println(line);
                validationLog.add(line);
                line = reader.readLine();
            }
            if (p.exitValue() == 0) {
                is_upcoming_checkit_tiff_valid=true;
                extractionErrors.clear();
            } else { // something wrong
                is_upcoming_checkit_tiff_valid = false;
                extractionErrors=validationLog;
            }

        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);
            System.out.println( "ERROR: (upcoming) checkit_tiff not available, path=" + this.upcoming_checkit_tiff_binary_path + ", " + e.getMessage());
            throw new Exception("ERROR: (upcoming) checkit_tiff not available, path=" + this.upcoming_checkit_tiff_binary_path + ", " + e.getMessage());
        }
        /* only check against actual checkit_tiff if upcoming fails */
        if (is_upcoming_checkit_tiff_valid == false) {
            // checkit_tiff (actual)
            try {
                String execstring = this.actual_checkit_tiff_binary_path + " " + filePath + " " + this.actual_checkit_tiff_config_path;
                System.out.println("executing: " + execstring);
                Process p = Runtime.getRuntime().exec(execstring);
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();

                while (line != null) {
                    System.out.println(line);
                    validationLog.add(line);
                    line = reader.readLine();
                }
                if (p.exitValue() == 0) {
                    is_actual_checkit_tiff_valid = true;
                } else { // something wrong
                    is_actual_checkit_tiff_valid = false;
                }
            } catch (IOException e) {
                //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);
                System.out.println("ERROR: (actual) checkit_tiff not available, path=" + this.actual_checkit_tiff_binary_path + ", " + e.getMessage());
                throw new Exception("ERROR: (actual) checkit_tiff not available, path=" + this.actual_checkit_tiff_binary_path + ", " + e.getMessage());
            }
        }

        /* If upcoming was true, only report a is valid, if actual was true. report log of upcoming, if all fail, report log for all */
        if (true == is_upcoming_checkit_tiff_valid) {
            isvalid = true;
            iswellformed = true;
            extractionErrors.clear();
        } else if (true == is_actual_checkit_tiff_valid) {
            isvalid = true;
            iswellformed=true;
            extractionErrors = validationLog;
        } else {
            isvalid = false;
            iswellformed = false;
            extractionErrors = validationLog;
        }

        // exiftool output of metadata
        try {
            String execstring = this.exiftool_binary_path + " -X " + filePath;
            System.out.println("executing: " + execstring);
            Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            String response="";
            while (line != null) {
                System.out.println(line);
                parse_exiftool_output(line.trim());
                response+=line;
                line = reader.readLine();
            }
            attributes.put("exiftool-log", response.trim());

        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);


        } catch (InterruptedException e) {
            e.printStackTrace();

        }
        // attributes.put("checkit-tiff-version", "");
        // attributes.put("checkit-tiff-path", actual_checkit_tiff_binary_path.trim());
        // attributes.put("checkit-tiff-conf", actual_checkit_tiff_config_path.trim());
        attributes.put("checkit-tiff-log", validationLog.toString());

    }

    public String getAgentName()
    {
      return "checkit_tiff";
    }

    /** get clamd agent version and signature version calling clamd-command VERSION
     *
     * @return string with clamd version and signature version
     */
    public String getAgent() {
        String response="";
        response+="actual checkit_tiff:\n";
        try {
            String execstring = this.actual_checkit_tiff_binary_path + " -v";
            Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            while (line != null) {
                System.out.println(line);
                response+=line;
                line = reader.readLine();
            }
        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        response+="upcoming checkit_tiff:\n";
        try {
            String execstring = this.upcoming_checkit_tiff_binary_path + " -v";
            Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            while (line != null) {
                System.out.println(line);
                response+=line;
                line = reader.readLine();
            }
        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        available.add("ExifIFD:ApertureValue");
        available.add("ExifIFD:BrightnessValue");
        available.add("ExifIFD:CFAPattern");
        available.add("ExifIFD:ColorSpace");
        available.add("ExifIFD:ComponentsConfiguration");
        available.add("ExifIFD:CompressedBitsPerPixel");
        available.add("ExifIFD:Contrast");
        available.add("ExifIFD:CreateDate");
        available.add("ExifIFD:CustomRendered");
        available.add("ExifIFD:DateTimeOriginal");
        available.add("ExifIFD:DigitalZoomRatio");
        available.add("ExifIFD:ExifImageHeight");
        available.add("ExifIFD:ExifImageWidth");
        available.add("ExifIFD:ExifVersion");
        available.add("ExifIFD:ExposureCompensation");
        available.add("ExifIFD:ExposureMode");
        available.add("ExifIFD:ExposureProgram");
        available.add("ExifIFD:ExposureTime");
        available.add("ExifIFD:FileSource");
        available.add("ExifIFD:Flash");
        available.add("ExifIFD:FlashpixVersion");
        available.add("ExifIFD:FNumber");
        available.add("ExifIFD:FocalLength");
        available.add("ExifIFD:FocalLengthIn35mmFormat");
        available.add("ExifIFD:FocalPlaneResolutionUnit");
        available.add("ExifIFD:FocalPlaneXResolution");
        available.add("ExifIFD:FocalPlaneYResolution");
        available.add("ExifIFD:GainControl");
        available.add("ExifIFD:ISO");
        available.add("ExifIFD:LensInfo");
        available.add("ExifIFD:LensModel");
        available.add("ExifIFD:LightSource");
        available.add("ExifIFD:MaxApertureValue");
        available.add("ExifIFD:MeteringMode");
        available.add("ExifIFD:Saturation");
        available.add("ExifIFD:SceneCaptureType");
        available.add("ExifIFD:SceneType");
        available.add("ExifIFD:SensingMethod");
        available.add("ExifIFD:SensitivityType");
        available.add("ExifIFD:SerialNumber");
        available.add("ExifIFD:Sharpness");
        available.add("ExifIFD:ShutterSpeedValue");
        available.add("ExifIFD:SubjectDistanceRange");
        available.add("ExifIFD:SubSecTimeDigitized");
        available.add("ExifIFD:SubSecTimeOriginal");
        available.add("ExifIFD:WhiteBalance");
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
        available.add("ICC_Profile:GrayTRC");
        available.add("ICC_Profile:GreenMatrixColumn");
        available.add("ICC_Profile:GreenTRC");
        available.add("ICC_Profile:MediaBlackPoint");
        available.add("ICC_Profile:MediaWhitePoint");
        available.add("ICC_Profile:ProfileCopyright");
        available.add("ICC_Profile:ProfileDescription");
        available.add("ICC_Profile:RedMatrixColumn");
        available.add("ICC_Profile:RedTRC");
        available.add("IFD0:Artist");
        available.add("IFD0:BitsPerSample");
        available.add("IFD0:Compression");
        available.add("IFD0:Copyright");
        available.add("IFD0:FillOrder");
        available.add("IFD0:ImageHeight");
        available.add("IFD0:ImageWidth");
        available.add("IFD0:Make");
        available.add("IFD0:Model");
        available.add("IFD0:ModifyDate");
        available.add("IFD0:Orientation");
        available.add("IFD0:PhotometricInterpretation");
        available.add("IFD0:PlanarConfiguration");
        available.add("IFD0:ResolutionUnit");
        available.add("IFD0:RowsPerStrip");
        available.add("IFD0:SamplesPerPixel");
        available.add("IFD0:Software");
        available.add("IFD0:StripByteCounts");
        available.add("IFD0:StripOffsets");
        available.add("IFD0:SubfileType");
        available.add("IFD0:T6Options");
        available.add("IFD0:XResolution");
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
        System.out.println("DEBUG: is valid=" + this.isvalid);
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
        initp.put( "actual_checkit_tiff", "/home/romeyke/git/checkit_tiff/build/checkit_tiff");
        initp.put( "actual_config_file", "/home/romeyke/git/checkit_tiff/example_configs/cit_tiff6_baseline_SLUBrelaxed.cfg");
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


