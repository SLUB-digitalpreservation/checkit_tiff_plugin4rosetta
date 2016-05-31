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

/**
 * SLUBTechnicalMetadataExtractorCheckItTiffPlugin
 *
 * @author andreas.romeyke@slub-dresden.de (Andreas Romeyke)
 * @see com.exlibris.dps.sdk.techmd.MDExtractorPlugin 
 */
public class SLUBTechnicalMetadataExtractorCheckItTiffPlugin implements MDExtractorPlugin {
    private String checkit_tiff_binary_path;
    private String checkit_tiff_config_path;
    private List<String> extractionErrors = new ArrayList<String>();
    private boolean isvalid = false;
    private boolean iswellformed = false;
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
        this.checkit_tiff_binary_path = initp.get("checkit_tiff").trim();
        this.checkit_tiff_config_path = initp.get("config_file").trim();
        System.out.println("SLUBTechnicalMetadataExtractorCheckItTiffPlugin instantiated with checkit_tiff_binary_path=" + checkit_tiff_binary_path + " cfg=" + checkit_tiff_config_path);
    }

    @Override
    public void extract(String filePath) throws Exception {
      if(StringUtils.isEmptyString(checkit_tiff_binary_path)) {
        //log.error("No checkit_tiff_binary_path defined. Please set the plugin parameter to hold your checkit_tiff_binary_path.");
        throw new Exception("path not found");
      }
      if(StringUtils.isEmptyString(checkit_tiff_config_path)) {
        //log.error("No checkit_tiff_config_path defined. Please set the plugin parameter to hold your checkit_tiff_config_path.");
        throw new Exception("path not found");
      }

        try {
            String execstring = this.checkit_tiff_binary_path + " " + filePath + " " + this.checkit_tiff_config_path;
            Process p = Runtime.getRuntime().exec( execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();

            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
                extractionErrors.add(line);
            }
            if (p.exitValue() == 0) {
                isvalid=true;
                iswellformed=true;
            } else { // something wrong
                isvalid = false;
                iswellformed = false;
            }
        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);


            //return "ERROR: checkit_tiff not available";
        }
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

        try {
            String execstring = this.checkit_tiff_binary_path + " -v";
          Process p = Runtime.getRuntime().exec(execstring);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            String response="";
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
                response+=line;
            }
            return response.trim();
        } catch (IOException e) {
            //log.error("exception creation socket, clamd not available at host=" + host + "port=" + port, e);


        } catch (InterruptedException e) {
            e.printStackTrace();

        }
        return "ERROR: checkit_tiff not available";
    }

    @Override
    public String getAttributeByName(String attribute) {
      return (String) "attribute";
    }

    @Override
    public List<String> getExtractionErrors() {
      return extractionErrors;
    }

    @Override
    public List<String> getSupportedAttributeNames() {
      List<String> attributes = new ArrayList<String>();

      return attributes;

    }

    @Override
    public boolean isWellFormed() {
      return this.iswellformed;
    }

    @Override
    public boolean isValid() {
      return isvalid;
    }
    @Override
    public String getFormatName() {
        return "TIFF";
    }

    @Override
    public String getFormatVersion() {
      return "6";
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
        initp.put( "checkit_tiff", "/home/romeyke/git/checkit_tiff/build/checkit_tiff");
        initp.put( "config_file", "/home/romeyke/git/checkit_tiff/example_configs/cit_tiff6_baseline_SLUBrelaxed.cfg");

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
        }
    }
}


