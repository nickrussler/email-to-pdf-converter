##EML to PDF Converter

### Homepage
<a href="https://eml-to-pdf.com">eml-to-pdf.com</a>

### Screenshot
<img src="https://www.whitebyte.info/wp-content/uploads/2015/02/scr1.png" />

### Commandline Interface
```
Usage: EMLtoPDFConverter [options] 
  Options:
    -d, --debug
       Debug mode
       Default: false
    -dc, --disable-crashreports
       Do not send crash reports to the developer.
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

### How to build
 * `gradlew shadowJar` <br>
Creates a single self contained Jar in `build/libs`

 * `gradlew dist` <br>
Same as `gradlew shadowJar` but additionally creates windows exe launchers in `build/libs` for gui and console mode. This task needs the Launch4j binary in the path.

 * `gradlew innosetup` <br>
Creates a windows setup in `build/innosetup`. This task needs the Launch4j binary as well as the Inno Setup issc.exe in the path.

 * `gradlew check` <br>
Executes the unit tests and generates various reports (jacoco, checkstyle, findbugs, jdepend, unit test report).

### License
The code available under the terms of the GNU Affero General Public License (AGPL). This means you have to release the source code of applications that use this software. If you can't do so you can <a target="_blank" href="https://eml-to-pdf.com">buy a commercial license</a>.
