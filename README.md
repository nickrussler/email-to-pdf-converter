## Email to PDF Converter (formely EML to PDF Converter) [![Actions Status](https://github.com/nickrussler/email-to-pdf-converter/workflows/Check,%20Build/badge.svg)](https://github.com/nickrussler/email-to-pdf-converter/actions)
[![Packaging status](https://repology.org/badge/vertical-allrepos/email-to-pdf-converter.svg)](https://repology.org/project/email-to-pdf-converter/versions)

This software can be used to convert email files (eml or msg) to pdf files. It can be used as a library, command line tool or desktop application with its GUI.

The conversion is done by parsing (and cleaning) the mime/structure, converting it to html and then using a Chrome/Chromium/Edge browser or [wkhtmltopdf](//github.com/wkhtmltopdf/wkhtmltopdf) to convert the generated html to a pdf file. Chrome is the default renderer; if no browser is installed, a Chrome headless shell can be downloaded automatically.

It also handles inline images, corrupt mime headers and can use a proxy.

JavaScript in email HTML is not executed during conversion; script tags are rendered inert. Use `--dangerous-enable-javascript` only if you explicitly need to execute scripts.


### Prerequisites
You need :

* a complete jre, not a headless one (on Debian, for example, you need to have `openjdk-11-jre` installed);
* a browser for the default renderer: an installed Chrome/Chromium/Edge browser, or allow the converter to download a Chrome headless shell; or [wkhtmltopdf](//github.com/wkhtmltopdf/wkhtmltopdf) in your $PATH for `--renderer wkhtmltopdf`

### Download
You can download the lates binaries [**here**](//github.com/nickrussler/email-to-pdf-converter/releases/latest).

There you can find a Windows setup.exe and a jar.<br>
If you want to use the jar (e.g. for a non Windows OS), install Chrome/Chromium/Edge or allow the converter to download a Chrome headless shell. To use the legacy renderer, put the [wkhtmltopdf](http://wkhtmltopdf.org/) binary in the PATH and pass `--renderer wkhtmltopdf`.

### Screenshot
<img src="https://www.whitebyte.info/wp-content/uploads/2015/02/scr1.png" />

### Commandline Interface
```
Usage: EmailToPDFConverter [options] <email-file>
  Options:
    -d, --debug
      Debug mode
      Default: false
    -e, --error
      Display only Error messages.
      Default: false
    -aa, --add-attachment-names
      Add the list of attachment names under the body text
      Default: false
    -a, --extract-attachments
      Extract Attachments.
      Default: false
    -ad, --extract-attachments-directory
      Extract Attachments to this Directory, if this option is not present the
      directory is besides the pdf as "<pdf-name>-attachments".
    -?, --help
      Print this help.
    -hh, --hide-headers
      Do not add email headers (subject, from, etc.) at the beginning of the
      PDF document.
      Default: false
    -o, --output-filepath
      Filepath of the produced PDF document. If this option is ommited the PDF
      will be placed alongside the email File.
    -s, --page-size
      Set paper size to: A0-A9, B0-B10, C5E, Comm10E, DLE, Executive,
      Folio, Ledger, Legal, Letter, Tabloid (default A4)
      Default: A4
    -r, --dpi
      Set wkhtmltopdf DPI. Ignored by the Chrome renderer. (default 300)
      Default: 300
    -O, --orientation
      Set page orientation: portrait, landscape (default portrait)
      Default: portrait
    --renderer
      Renderer to use: wkhtmltopdf, chrome (default chrome). Chrome blocks
      local file access.
      Default: chrome
    --renderer-executable
      Path to the wkhtmltopdf or Chrome/Chromium executable.
    --browser-download
      Chrome browser source: auto, never, once, latest, force (default auto).
      Default: auto
    --browser-cache-dir
      Directory for an automatically downloaded Chrome headless shell.
    --custom-stylesheet-path
      Path to a custom print stylesheet for the Chrome renderer. Relative URLs
      in the CSS are not resolved against the stylesheet directory.
    --dangerous-enable-javascript
      Execute JavaScript contained in the email HTML. This is unsafe and
      disabled by default.
      Default: false
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
E.g. ``java -jar emailconverter-2.1.1-all.jar example.eml`` (uses Chrome by default; install Chrome/Chromium/Edge or allow the automatic headless-shell download. For wkhtmltopdf, pass `--renderer wkhtmltopdf`.)

With the Chrome renderer (the default), browser selection works as follows:

* `auto` uses an installed Chrome/Chromium/Edge browser first, then a cached downloaded shell, and finally downloads a Chrome headless shell.
* `never` uses an installed browser or cached shell and never downloads.
* `once` uses the managed headless shell, downloading it only when no cached copy exists.
* `latest` uses the managed headless shell and checks for a newer Stable version, downloading it when necessary. It checks the metadata endpoint on each conversion.
* `force` downloads the current Stable headless shell even when that version is already cached and replaces the cached copy after successful extraction.

Downloaded shells are cached per version and platform. Use `--browser-cache-dir` to change the cache location. `auto`, `never`, and `once` can reuse a cached shell without a metadata request.

Use `--orientation landscape` for landscape output. Chrome honors orientation declarations in the email's print CSS; if the final PDF is portrait, conversion fails with a clear error instead of writing a mismatched document. The legacy wkhtmltopdf renderer may still overlap complex table text in landscape.

### Custom print stylesheet

Chrome accepts an optional custom print stylesheet:

```text
--custom-stylesheet-path PATH
```

The file is read as UTF-8 and its CSS text is injected into the loaded document through Chrome DevTools. Relative asset URLs are not resolved against the stylesheet file's directory. The option is independent of `--orientation`.

Chrome uses its inspector stylesheet cascade, so a more specific `!important` rule in the email can still win. Use `--orientation landscape` together with an example override for issue #77:

```css
@page { size: auto !important; }
```

```bash
java -jar emailconverter.jar --orientation landscape --custom-stylesheet-path landscape.css "Test email conversion.msg"
```

The stylesheet is supplied by the caller; orientation alone never injects it. If a surviving page rule prevents landscape output, conversion fails with the landscape validation error.

The Swing GUI exposes the same option as a **Custom stylesheet** field with a CSS file picker.

### Local-file access

Chrome intentionally does not load `file://` resources referenced by email HTML. There is no Chrome opt-in.

The legacy wkhtmltopdf renderer retains its existing `extParams` escape hatch (`--enable-local-file-access`) for backwards compatibility through the public API only. It is not exposed as a CLI option and should be treated as a legacy behavior.

Automatic download supports glibc-based Linux x86-64/ARM64, macOS Intel/Apple Silicon, and Windows x86/x64. It is not available on musl-based Linux (for example Alpine) or Windows ARM64; install Chromium/Chrome or use `--renderer-executable` there.

### How to Build
You need to git clone this repository. The build will fail if you remove the .git folder (e.g. download this as zip from github).

 * `gradlew shadowJar` <br>
Creates a single self contained Jar in `build/libs`

 * `gradlew dist` <br>
Same as `gradlew shadowJar` but additionally creates windows exe launchers in `build/libs` for gui and console mode. This task needs the [Launch4j](http://launch4j.sourceforge.net/) binary in the PATH.

 * `gradlew innosetup` <br>
Creates a windows setup in `build/innosetup`. This task needs the [Launch4j](http://launch4j.sourceforge.net/) binary as well as the [Inno Setup](http://www.jrsoftware.org/isinfo.php) issc.exe in the PATH.

 * `gradlew check` <br>
Executes the unit tests and generates various reports (jacoco, checkstyle, findbugs, jdepend, unit test report).

### Releases
Pushing a version tag such as `4.0.0` runs the Windows release workflow. It builds `setup.exe` and the versioned jar, uploads them as workflow artifacts, and creates a draft GitHub release for review before publishing.

### Date Formatting
Dates are formatted with the default locale. You can change it, e.g. by passing the VM argument `-Duser.language=en-US` similar to setting the timezone e.g. `-Duser.timezone="Asia/Kolkata"`.

### License
The code is available under the terms of the Apache V2 License.
