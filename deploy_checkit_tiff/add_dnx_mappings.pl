#!/usr/bin/env perl 
# adds all dnx properties
# by Andreas Romeyke
#
# needs a geckodriver and the Selenium::Remote::Driver module
# see http://www.seleniumhq.org/
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

my %exiftool2dnx = (
    "IFD0:ImageWidth" => "image.width",
    "IFD0:BitsPerSample" => "tiff.bitspersample",
    "IFD0:Documentname" => "tiff.documentname",
    "IFD0:ModifyDate" => "tiff.datetime",
);

print "Try to mechanize adding DNX, using: 
  host=$host
  user=$user
  institution=$institution
  ";

my $driver = Selenium::Chrome->new();
$driver->debug_on;
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
    $driver->find_element("login1", "id")->click;
}

sub logout {
    print "logout:\n";
    $driver->get("http://$host:1801/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
    my $ele = $driver->find_element("user", "id")->click;
    $ele = $driver->find_element("Logout", "link")->click;
}

sub extractors_add_mapping {
    $driver->get("http://$host:1801/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
    $driver->find_element("Extractors", "link")->click;
    $driver->find_element("Custom", "link")->click;
    $driver->find_element("Edit", "link")->click;
    $driver->find_element("Add Mapping", "link")->click;
    $driver->find_element("pageBeancurrentMappingextractorProperty_button", "id")->click;
    $driver->find_element("ui-id-103", "id")->click;
    $driver->find_element("pageBeancurrentMappingclassificationProperty_button", "id")->click;
    $driver->find_element("ui-id-424", "id")->click;
    $driver->find_element("page.buttons.operation", "name")->click;
}


sub _change_to_xxx_format_library {
    $driver->get("http://$host:1801/mng/action/menus.do?first_time_key=com.exlibris.dps.wrk.general.menu");
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

}
sub change_to_global_format_library {
    _change_to_xxx_format_library();
    $driver->find_child_element( $td, "descendant::li[text()='true']", "xpath")->click;
    say "select true";
    $driver->find_element("//button[\@value='Update']", "xpath")->click;
    say "click update button";
}
sub change_to_local_format_library {
    _change_to_xxx_format_library();
    $driver->find_child_element( $td, "descendant::li[text()='false']", "xpath")->click;
    say "select false";
    $driver->find_element("//button[\@value='Update']", "xpath")->click;
    say "click update button";
}

login();

# change to global format library
change_to_global_format_library();
# add dnx property
# include dnx property to classification group (Image(MIX))
# add mapping
# change to local format library
change_to_local_format_library();

logout();


$driver->quit();
$driver->shutdown_binary();
