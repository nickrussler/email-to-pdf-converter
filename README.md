## EML to PDF Converter (Email to PDF Converter) <a href="https://travis-ci.org/nickrussler/eml-to-pdf-converter"><img align="right" src="https://travis-ci.org/nickrussler/eml-to-pdf-converter.svg?branch=master"></a>

This software can be used to convert eml files (emails as plain text in MIME format) to pdf files. It can be used as a library, command line tool or desktop application with its GUI.

The conversion is done by parsing (and cleaning) the mime/structure, converting it to html and then using [wkhtmltopdf](//github.com/wkhtmltopdf/wkhtmltopdf) to convert the generated html to a pdf file.

It also handles inline images, corrupt mime headers and can use a proxy.

### Download
You can download the lates binaries [**here**](//github.com/nickrussler/eml-to-pdf-converter/releases/latest).

There you can find a Windows setup.exe and a jar.<br>
If you want to use the jar (e.g. for a non Windows OS) you need the [wkhtmltopdf](http://wkhtmltopdf.org/) binary in the PATH.


### Screenshot
<img src="https://www.whitebyte.info/wp-content/uploads/2015/02/scr1.png" />

### Commandline Interface
```
Usage: EMLtoPDFConverter [options] [eml-filename]
  Options:
    -d, --debug
       Debug mode
       Default: false
    -e, --error
       Display only Error messages.
       Default: false
    -a, --extract-attachments
       Extract Attachments.
       Default: false
    -ad, --extract-attachments-directory
       Extract Attachments to this Directory, if this option is not present the
       directory is besides the pdf as "-attachments".
    -?, --help
       Print this help.
       Default: false
    -hh, --hide-headers
       Do not add email headers (subject, from, etc.) at the beginning of the
       PDF document.
       Default: false
    -o, --output-filepath
       Filepath of the produced PDF document. If this option is ommited the PDF
       will be placed alongside the EML File.
    -p, --proxy
       Proxy (e.g. "http://10.64.1.74:81"). If "auto" is supplied the default
       system proxy will be used.
    -q, --quiet
       Do not display any messages at all.
       Default: false
    -gui, --show-graphical-user-interface
       Show graphical user interface (other parameters are ignored when using
       this switch).
       Default: false
    -v, --version
       Print the version number.
       Default: false
  ```
E.g. ``java -jar emailconverter-2.0.0-all.jar example.eml`` (you need [wkhtmltopdf](http://wkhtmltopdf.org/) binary in the PATH)

### How to build
You need to git clone this repository. The build will fail if you remove the .git folder (e.g. download this as zip from github).

 * `gradlew shadowJar` <br>
Creates a single self contained Jar in `build/libs`

 * `gradlew dist` <br>
Same as `gradlew shadowJar` but additionally creates windows exe launchers in `build/libs` for gui and console mode. This task needs the [Launch4j](http://launch4j.sourceforge.net/) binary in the PATH.

 * `gradlew innosetup` <br>
Creates a windows setup in `build/innosetup`. This task needs the [Launch4j](http://launch4j.sourceforge.net/) binary as well as the [Inno Setup](http://www.jrsoftware.org/isinfo.php) issc.exe in the PATH.

 * `gradlew check` <br>
Executes the unit tests and generates various reports (jacoco, checkstyle, findbugs, jdepend, unit test report).

### License
The code is available under the terms of the Apache V2 License.
