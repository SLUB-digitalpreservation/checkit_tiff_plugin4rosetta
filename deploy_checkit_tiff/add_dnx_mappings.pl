#!/usr/bin/env perl 
# adds all dnx properties
# by Andreas Romeyke (andreas.romeyke@slub-dresden.de)
#
# needs a geckodriver and the Selenium::Remote::Driver module
# see http://www.seleniumhq.org/
#
# tested against Rosetta 5.2 using chromedriver and chromium 60.0.3112.78 
# under Debian Stretch

# ensure, that the plugin is assigned to a classification, for example to
# "Image (Mix)"

use strict;
use warnings;
use utf8;
use Selenium::Remote::Driver;
use Selenium::Chrome;
use Data::Printer;
use v5.10;

my $host=shift;
my $user=shift;
my $passwd=shift;
my $institution=shift;
#my $ui_port=":1801";
#my $ui_port=":80"; # Produktion
my $ui_port=shift;


my %exiftool2dnx = (
    "ICC-header:ColorSpaceData" => "icc.colorspacedata",
    "ICC-header:DeviceModel" => "icc.devicemodel",
    "ICC-header:PrimaryPlatform" => "icc.primaryplatform",
    "ICC-header:ProfileCMMType" => "icc.profilecmmtype",
    "ICC-header:ProfileCreator" => "icc.profilecreator",
    "ICC-header:ProfileDateTime" => "icc.profiledatetime",
    "ICC-header:ProfileVersion" => "icc.profileversion",
    "ICC_Profile:CalibrationDateTime" => "icc.profilecalibrationdatetime",
    "ICC_Profile:MakeAndModel" => "icc.makeandmodel",
    "ICC_Profile:ProfileCopyright" => "icc.profilecopyright",
    "ICC_Profile:ProfileDescription" => "icc.profiledescription",
    "IFD0:BitsPerSample" => "tiff.bitspersample", # TIFF-Tag 258 BitsPerSample ()
    "IFD0:Compression" => "tiff.compression",
    "IFD0:Copyright" => "tiff.copyright", # TIFF-Tag 33432 Copyright ()
    "IFD0:DocumentName" => "tiff.documentname", # TIFF-Tag 269 DocumentName ()
    "ExifIFD:GrayResponseCurve" => "tiff.grayresponsecurve", # TIFF-Tag 291 GrayResponseCurve ()
    "IFD0:GrayResponseUnit" => "tiff.grayresponseunit", # TIFF-Tag 290 GrayResponseUnit ()
    "IFD0:ImageDescription" => "tiff.imagedescription", # TIFF-Tag 270 ImageDescription ()
    "IFD0:ImageHeight" => "image.height", # TIFF-Tag 257 ImageLength ()
    "IFD0:ImageWidth" => "image.width", # TIFF-Tag 256 ImageWidth (  )
    "IFD0:Make" => "tiff.make", # TIFF-Tag 271 Make ()
    "IFD0:MaxSampleValue" => "tiff.maxsamplevalue", # TIFF-Tag 281 MaxSampleValue ( )
    "IFD0:MinSampleValue" => "tiff.minsamplevalue", # TIFF-Tag 280 MinSampleValue ( )
    "IFD0:Model" => "tiff.model", # TIFF-Tag 272 Model ()
    "IFD0:ModifyDate" => "tiff.datetime", # TIFF-Tag 306 DateTime ()
    "IFD0:PageNumber" => "tiff.pagenumber", # TIFF-Tag 297 PageNumber ()
    "IFD0:PhotometricInterpretation" => "tiff.photometricinterpretation", # TIFF-Tag 262 PhotometricInterpretation ()
    "IFD0:PrimaryChromaticities" => "tiff.primarychromaticities", # TIFF-Tag 319 PrimaryChromaticities ()
    "IFD0:SamplesPerPixel" => "tiff.samplesperpixel", # TIFF-Tag 277 SamplesPerPixel ()
    "IFD0:Software" => "tiff.software", # TIFF-Tag 305 Software ()
    "IFD0:WhitePoint" => "tiff.whitepoint", # TIFF-Tag 318 WhitePoint ()
    "IFD0:XResolution" => "image.xresolution", # TIFF-Tag 282 XResolution ( Angabe in dpi)
    "IFD0:YResolution" => "image.yresolution", # TIFF-Tag 283 YResolution ( Angabe in dpi)
);

my $plugin_name="SLUBTechnicalMetadataExtractorCheckItTiffPlugin";
my $classification_group="Image (Mix)";


print "Try to mechanize adding DNX, using: 
  host=$host
  user=$user
  institution=$institution
  ";

my $driver = Selenium::Chrome->new();
$driver->debug_on;
$driver->set_implicit_wait_timeout(1000);
$driver->set_timeout('script', 1000);

sub login {
    print "login:\n";
    $driver->get("http://$host:8991/pds?func=load-login&lang=en&langOptions=en.English&institution=&institute=INS_SLUB&calling_system=dps");
    my $ele_institute = $driver->find_element("institute", "id");
    p( $ele_institute);
    print "inst select found\n";
    $driver->find_child_element(
        $ele_institute,
        "./*[text()='$institution']", "xpath"
    )->click;
    $driver->find_element("username", "id")->clear;
    $driver->find_element("username", "id")->send_keys("$user");
    $driver->find_element("password", "id")->clear;
    $driver->find_element("password", "id")->send_keys("$passwd");
    $driver->find_element("Login", "name")->click;
}

sub logout {
    print "logout:\n";
    $driver->get("http://$host${ui_port}/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
    my $ele = $driver->find_element("user", "id")->click;
    $driver->pause();
    $ele = $driver->find_element("Logout", "link")->click;
}



sub _change_to_xxx_format_library {
    $driver->get("http://$host${ui_port}/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
    $driver->find_element("Quick Launch", "link")->click;
    $driver->find_element("Administer the system", "link")->click;
    $driver->find_element("General", "link")->click;
    $driver->find_element("General Parameters", "link")->click;
    $driver->find_element("undefined_button", "id")->click;
    $driver->find_element("//li[\@title='format_library']", "xpath")->click;
    ## find 'tr[/td=format_library_is_global]', dann undefined button clicken,
    my $tr = $driver->find_element("//tr[starts-with(td,'format_library_is_global')]", "xpath");
    p( $tr );
    my $td = $driver->find_child_element($tr, "./td[\@class='form-inline']", "xpath");
    p ($td);
    $driver->find_child_element( $td, "descendant::button[\@id='undefined_button']", "xpath")->click;
    say "click combobox button";
    return $td;

}
sub change_to_global_format_library {
    my $td=_change_to_xxx_format_library();
    $driver->find_child_element( $td, "descendant::li[text()='true']", "xpath")->click;
    say "select true";
    $driver->find_element("//button[\@value='Update']", "xpath")->click;
    say "click update button";
}
sub change_to_local_format_library {
    my $td = _change_to_xxx_format_library();
    $driver->find_child_element( $td, "descendant::li[text()='false']", "xpath")->click;
    say "select false";
    $driver->find_element("//button[\@value='Update']", "xpath")->click;
    say "click update button";
    $driver->pause();
}

sub add_dnx_property ($$) {
    my $dnx_property = shift;
    my $dnx_description = shift;
    my $dow = localtime;
    #$driver->get("http://$host${ui_port}/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
    #$driver->find_element("Preservation", "link")->click;
    #$driver->find_element("(//a[contains(text(),'Significant Properties')])[2]", "xpath")->click;
    $driver->get("http://$host${ui_port}/mng/action/pageAction.page_xml.page_sig_prop_list.xml.do?pageViewMode=Edit&pageBean.currentUserMode=GLOBAL&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryGLOBAL.mngLibraryHeader.SigProps.InnerMenu&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryGLOBAL.mngLibraryHeader.SigProps.InnerMenu&backUrl=");
    $driver->pause();
    $driver->find_element("Add Significant Property", "link")->click;
    $driver->find_element("selectedSigPropname", "id")->clear;
    $driver->find_element("selectedSigPropname", "id")->send_keys("$dnx_property");
    $driver->find_element("selectedSigPropdescription", "id")->clear;
    $driver->find_element("selectedSigPropdescription", "id")->send_keys("$dnx_description (automatisiert durch $0 hinzugefügt, $dow)");
    $driver->find_element("SaveSigPropGenDetails", "name")->click;
}

sub join_dnx_property_to_classification_group ($) {
    my $dnx_property = shift;
    say "try to join dnx property";
    $driver->get("http://$host${ui_port}/mng/action/pageAction.page_xml.page_classification_list.xml.do?pageViewMode=Edit&pageBean.currentUserMode=GLOBAL&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryGLOBAL.mngLibraryHeader.Classifications.InnerMenu&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryGLOBAL.mngLibraryHeader.Classifications.InnerMenu&backUrl=");
    $driver->pause();
    $driver->find_element("find0.0", "id")->clear;
    $driver->find_element("find0.0", "id")->send_keys($classification_group);
    $driver->find_element("go", "name")->click;
    $driver->find_element("Edit", "link")->click;
    $driver->find_element("Related Properties", "link")->click;
    my $ele = $driver->find_element("//form[\@id='classificationDetailsForm']//div[\@class='available']", "xpath");
    $driver->find_child_element($ele, "(//input[\@type='text'])", "xpath")->clear;
    $driver->find_child_element($ele, "(//input[\@type='text'])[3]", "xpath")->send_keys($dnx_property);
    #$driver->find_element("//form[\@id='classificationDetailsForm']/div/div[4]/div/div/div[2]/ul/li[230]/a/span", "xpath")->click;
    #my $ele = $driver->find_element("li[\@title='$dnx_property']", "xpath");
    #$driver->find_child_element( $ele, "a", "link")->click;
    $driver->pause();
    $driver->find_element("Add all", "link")->click;
    $driver->find_element("SaveClassificationGenDetails", "name")->click;
    $driver->pause();
}

sub extractors_add_mapping ($$) {
    my $dnx_property = shift;
    my $exiftool_property = shift;
    say "try to add extractor mapping";
    $driver->get("http://$host${ui_port}//mng/action/pageAction.page_xml.page_extractors_list.xml.do?pageBean.deploymentMode=BUNDLED&pageViewMode=Edit&pageBean.currentUserMode=LOCAL&RenewBean=true&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryLOCAL.mngLibraryHeader.Extractors.InnerMenu&menuKey=com.exlibris.dps.wrk.general.menu.Preservation.AdvancedPreservationActivities.mngLibraryLOCAL.mngLibraryHeader.Extractors.InnerMenu&backUrl=");
    $driver->pause();
    $driver->find_element("Custom", "link")->click;
    $driver->find_element("find1.0", "id")->clear;
    $driver->find_element("find1.0", "id")->send_keys($plugin_name);
    $driver->find_element("go", "name")->click;
    $driver->find_element("Edit", "link")->click;
    $driver->find_element("Add Mapping", "link")->click;
    $driver->find_element("pageBeancurrentMappingextractorProperty_input", "id")->click;
    $driver->find_element("pageBeancurrentMappingextractorProperty_input", "id")->clear;
    $driver->find_element("pageBeancurrentMappingextractorProperty_input", "id")->send_keys($exiftool_property);
    $driver->find_element("//li[\@title='$exiftool_property']", "xpath")->click;
    $driver->pause();
    $driver->find_element("pageBeancurrentMappingclassificationProperty_input", "id")->click;
    $driver->find_element("pageBeancurrentMappingclassificationProperty_input", "id")->clear;
    $driver->find_element("pageBeancurrentMappingclassificationProperty_input", "id")->send_keys("$dnx_property");
    $driver->pause();
    $driver->find_element("//li[\@title='$dnx_property']", "xpath")->click;
    $driver->pause();
    $driver->find_element("pageBeancurrentMappingnormalizer_input", "id")->click;
    $driver->find_element("pageBeancurrentMappingnormalizer_input", "id")->click;
    $driver->find_element("pageBeancurrentMappingnormalizer_input", "id")->clear;
    $driver->pause();
    $driver->find_element("page.buttons.operation", "name")->click;
    $driver->pause();
}

login();

# change to global format library
change_to_global_format_library();

# add dnx property

foreach my $exiftool_property (sort keys %exiftool2dnx) {
    my $dnx_property = $exiftool2dnx{ $exiftool_property };
    add_dnx_property($dnx_property, "$dnx_property <- Exiftool '$exiftool_property'");
}

foreach my $exiftool_property (sort keys %exiftool2dnx) {
    my $dnx_property = $exiftool2dnx{ $exiftool_property };
    join_dnx_property_to_classification_group( $dnx_property);
}

foreach my $exiftool_property (sort keys %exiftool2dnx) {
    my $dnx_property = $exiftool2dnx{ $exiftool_property };
    extractors_add_mapping ($dnx_property, $exiftool_property);
}

# include dnx property to classification group (Image(MIX))
# add mapping
# change to local format library
change_to_local_format_library();

logout();


$driver->quit();
$driver->shutdown_binary();
