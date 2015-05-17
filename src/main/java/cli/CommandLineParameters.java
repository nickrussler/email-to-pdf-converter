/*
 * EML to PDF Converter
 * Copyright (C) 2015 Nick Russler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cli;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * JavaBean which holds the CLI params.
 * @author Nick Russler
 */
public class CommandLineParameters {
	@Parameter(description = "<EML-File>", arity = 1)
	private List<String> files = new ArrayList<String>();

	@Parameter(names = {"-o", "--output-filepath"}, description = "Filepath of the produced PDF document. If this option is ommited the PDF will be placed alongside the EML File.")
	private String output;

	@Parameter(names = {"-p", "--proxy"}, description = "Proxy (e.g. \"http://10.64.1.74:81\"). If \"auto\" is supplied the default system proxy will be used.")
	private String proxy;

	@Parameter(names = {"-d", "--debug"}, description = "Debug mode")
	private boolean debug = false;

	@Parameter(names = {"-e", "--error"}, description = "Display only Error messages.")
	private boolean error = false;

	@Parameter(names = {"-q", "--quiet"}, description = "Do not display any messages at all.")
	private boolean quiet = false;

	@Parameter(names = {"-?", "--help"}, description = "Print this help.", help = true)
	private boolean help;
	
	@Parameter(names = {"-v", "--version"}, description = "Print the version number.")
	private boolean version;
	
	@Parameter(names = {"-hh", "--hide-headers"}, description = "Do not add email headers (subject, from, etc.) at the beginning of the PDF document.")
	private boolean hideHeaders = false;
	
	@Parameter(names = {"-a", "--extract-attachments"}, description = "Extract Attachments.")
	private boolean extractAttachments = false;
	
	@Parameter(names = {"-dc", "--disable-crashreports"}, description = "Do not send crash reports to the developer.")
	private boolean disableCrashreports = false;
	
	@Parameter(names = {"-ad", "--extract-attachments-directory"}, description = "Extract Attachments to this Directory, if this option is not present the directory is besides the pdf as \"<pdf-name>-attachments\".")
	private String extractAttachmentsDir;
	
	@Parameter(names = {"-gui", "--show-graphical-user-interface"}, description = "Show graphical user interface (other parameters are ignored when using this switch).")
	private boolean gui;

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isQuiet() {
		return quiet;
	}

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getProxy() {
		return proxy;
	}

	public void setProxy(String proxy) {
		this.proxy = proxy;
	}

	public boolean isHideHeaders() {
		return hideHeaders;
	}

	public void setHideHeaders(boolean hideHeaders) {
		this.hideHeaders = hideHeaders;
	}

	public boolean isVersion() {
		return version;
	}

	public void setVersion(boolean version) {
		this.version = version;
	}

	public boolean isExtractAttachments() {
		return extractAttachments;
	}

	public void setExtractAttachments(boolean extractAttachments) {
		this.extractAttachments = extractAttachments;
	}

	public String getExtractAttachmentsDir() {
		return extractAttachmentsDir;
	}

	public void setExtractAttachmentsDir(String extractAttachmentsDir) {
		this.extractAttachmentsDir = extractAttachmentsDir;
	}

	public boolean isDisableCrashreports() {
		return disableCrashreports;
	}

	public void setDisableCrashreports(boolean disableCrashreports) {
		this.disableCrashreports = disableCrashreports;
	}

	public boolean isGui() {
		return gui;
	}

	public void setGui(boolean gui) {
		this.gui = gui;
	}
}