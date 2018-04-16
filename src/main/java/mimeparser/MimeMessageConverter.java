/*
 * Copyright 2016 Nick Russler
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mimeparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.tika.mime.MimeTypes;

import util.LogLevel;
import util.Logger;
import util.StringReplacer;
import util.StringReplacerCallback;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Converts eml files into pdf files.
 * @author Nick Russler
 */
public class MimeMessageConverter {
	/**
	 * Set System parameters to alleviate Java's built in Mime Parser strictness.
	 */
	static {
		System.setProperty("mail.mime.address.strict", "false");
		System.setProperty("mail.mime.decodetext.strict", "false");
		System.setProperty("mail.mime.decodefilename", "true");
		System.setProperty("mail.mime.decodeparameters", "true");
		System.setProperty("mail.mime.multipart.ignoremissingendboundary", "true");
		System.setProperty("mail.mime.multipart.ignoremissingboundaryparameter", "true");

		System.setProperty("mail.mime.parameters.strict", "false");
		System.setProperty("mail.mime.applefilenames", "true");
		System.setProperty("mail.mime.ignoreunknownencoding", "true");
		System.setProperty("mail.mime.uudecode.ignoremissingbeginend", "true");
		System.setProperty("mail.mime.multipart.allowempty", "true");
		System.setProperty("mail.mime.multipart.ignoreexistingboundaryparameter", "true");

		System.setProperty("mail.mime.base64.ignoreerrors", "true");

		// set own cleaner class to handle broken contentTypes
		System.setProperty("mail.mime.contenttypehandler", "mimeparser.ContentTypeCleaner");
	}

	// html wrapper template for text/plain messages
	private static final String HTML_WRAPPER_TEMPLATE = "<!DOCTYPE html><html><head><style>body{font-size: 0.5cm;}</style><meta charset=\"%s\"><title>title</title></head><body>%s</body></html>";
	private static final String ADD_HEADER_IFRAME_JS_TAG_TEMPLATE = "<script id=\"header-v6a8oxpf48xfzy0rhjra\" data-file=\"%s\" type=\"text/javascript\">%s</script>";
	private static final String HEADER_FIELD_TEMPLATE = "<tr><td class=\"header-name\">%s</td><td class=\"header-value\">%s</td></tr>";
	
	private static final Pattern IMG_CID_REGEX = Pattern.compile("cid:(.*?)\"", Pattern.DOTALL);
	private static final Pattern IMG_CID_PLAIN_REGEX = Pattern.compile("\\[cid:(.*?)\\]", Pattern.DOTALL);

	private static final String VIEWPORT_SIZE = "2480x3508";
	private static final int CONVERSION_DPI = 300;
	private static final int IMAGE_QUALITY = 100;

	/**
	 * Execute a command and redirect its output to the standard output.
	 * @param command list of the command and its parameters
	 */
	private static void execCommand(List<String> command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);

			if (Logger.level.compareTo(LogLevel.Info) >= 0) {
				pb.inheritIO();
			}

			Process p = pb.start();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convert an EML file to PDF.
	 * @throws Exception
	 */
	public static void convertToPdf(String emlPath, String pdfOutputPath, boolean hideHeaders, boolean isHtml, boolean extractAttachments, String attachmentsdir, List<String> extParams) throws Exception {
		Logger.info("Start converting %s to %s", emlPath, pdfOutputPath);
		
		Logger.debug("Read eml file from %s", emlPath);
		final MimeMessage message = new MimeMessage(null, new FileInputStream(emlPath));

		/* ######### Parse Header Fields ######### */
		Logger.debug("Read and decode header fields");
		String subject = message.getSubject();

		String from = message.getHeader("From", null);
		if (from == null) {
			from = message.getHeader("Sender", null);
		}

		try {
			from = MimeUtility.decodeText(MimeUtility.unfold(from));
		} catch (Exception e) {
			// ignore this error
		}
		
		String[] recipients = new String[0];
		String recipientsRaw = message.getHeader("To", null);
		if (!Strings.isNullOrEmpty(recipientsRaw)) {
			try {
				recipientsRaw = MimeUtility.unfold(recipientsRaw);
				recipients = recipientsRaw.split(",");
				for (int i = 0; i < recipients.length; i++) {
					recipients[i] = MimeUtility.decodeText(recipients[i]);
				}
			} catch (Exception e) {
				// ignore this error
			}
		}
		
		String sentDateStr = message.getHeader("date", null);
		
		/* ######### Parse the mime structure ######### */
		Logger.info("Mime Structure of %s:\n%s", emlPath, MimeMessageParser.printStructure(message));

		Logger.debug("Find the main message body");
		MimeObjectEntry<String> bodyEntry = MimeMessageParser.findBodyPart(message);
		String charsetName = bodyEntry.getContentType().getParameter("charset");

		Logger.info("Extract the inline images");
		final HashMap<String, MimeObjectEntry<String>> inlineImageMap = MimeMessageParser.getInlineImageMap(message);
		
		/* ######### Embed images in the html ######### */
		String htmlBody = bodyEntry.getEntry();
		if (bodyEntry.getContentType().match("text/html")) {
			if (inlineImageMap.size() > 0) {
				Logger.debug("Embed the referenced images (cid) using <img src=\"data:image ...> syntax");

				// find embedded images and embed them in html using <img src="data:image ...> syntax
				htmlBody = StringReplacer.replace(htmlBody, IMG_CID_REGEX, new StringReplacerCallback() {
					@Override
					public String replace(Matcher m) throws Exception {
						MimeObjectEntry<String> base64Entry = inlineImageMap.get("<" + m.group(1) + ">");

						// found no image for this cid, just return the matches string as it is
						if (base64Entry == null) {
							return m.group();
						}

						return "data:" + base64Entry.getContentType().getBaseType() + ";base64," + base64Entry.getEntry() + "\"";
					}
				});
			}
		} else {
			Logger.debug("No html message body could be found, fall back to text/plain and embed it into a html document");

			// replace \n line breaks with <br>
			htmlBody = htmlBody.replace("\n", "<br>").replace("\r", "");

			// replace whitespace with &nbsp;
			htmlBody = htmlBody.replace(" ", "&nbsp;");

			htmlBody = String.format(HTML_WRAPPER_TEMPLATE, charsetName, htmlBody);
			if (inlineImageMap.size() > 0) {
				Logger.debug("Embed the referenced images (cid) using <img src=\"data:image ...> syntax");

				// find embedded images and embed them in html using <img src="data:image ...> syntax
				htmlBody = StringReplacer.replace(htmlBody, IMG_CID_PLAIN_REGEX, new StringReplacerCallback() {
					@Override
					public String replace(Matcher m) throws Exception {
						MimeObjectEntry<String> base64Entry = inlineImageMap.get("<" + m.group(1) + ">");

						// found no image for this cid, just return the matches string
						if (base64Entry == null) {
							return m.group();
						}

						return "<img src=\"data:" + base64Entry.getContentType().getBaseType() + ";base64," + base64Entry.getEntry() + "\" />";
					}
				});
			}
		}

		Logger.debug("Successfully parsed the .eml and converted it into html:");

		Logger.debug("---------------Result-------------");
		Logger.debug("Subject: %s", subject);
		Logger.debug("From: %s", from);
		if (recipients.length > 0) {
			Logger.debug("To: %s", Joiner.on(", ").join(recipients));
		}
		Logger.debug("Date: %s", sentDateStr);
		String bodyExcerpt = htmlBody.replace("\n", "").replace("\r", "");
		if (bodyExcerpt.length() >= 60) {
			bodyExcerpt = bodyExcerpt.substring(0, 40) + " [...] " + bodyExcerpt.substring(bodyExcerpt.length() - 20, bodyExcerpt.length());
		}
		Logger.debug("Body (excerpt): %s", bodyExcerpt);
		Logger.debug("----------------------------------");

		Logger.info("Start conversion to pdf");
		
		File tmpHtmlHeader = null;
		if (!hideHeaders) {
			tmpHtmlHeader = File.createTempFile("emailtopdf", ".html");
			String tmpHtmlHeaderStr = Resources.toString(Resources.getResource("header.html"), StandardCharsets.UTF_8);
			String headers = "";
			
			if (!Strings.isNullOrEmpty(from)) {
				headers += String.format(HEADER_FIELD_TEMPLATE, "From", HtmlEscapers.htmlEscaper().escape(from));
			}
			
			if (!Strings.isNullOrEmpty(subject)) {
				headers += String.format(HEADER_FIELD_TEMPLATE, "Subject", "<b>" + HtmlEscapers.htmlEscaper().escape(subject) + "<b>");
			}
			
			if (recipients.length > 0) {
				headers += String.format(HEADER_FIELD_TEMPLATE, "To", HtmlEscapers.htmlEscaper().escape(Joiner.on(", ").join(recipients)));
			}
			
			if (!Strings.isNullOrEmpty(sentDateStr)) {
				headers += String.format(HEADER_FIELD_TEMPLATE, "Date", HtmlEscapers.htmlEscaper().escape(sentDateStr));
			}
			
			Files.write(String.format(tmpHtmlHeaderStr, headers), tmpHtmlHeader, StandardCharsets.UTF_8);
			
			// Append this script tag dirty to the bottom
			htmlBody += String.format(ADD_HEADER_IFRAME_JS_TAG_TEMPLATE, tmpHtmlHeader.toURI(), Resources.toString(Resources.getResource("contentScript.js"), StandardCharsets.UTF_8));
		}
		
                File pdf_or_html = new File(pdfOutputPath);
                if (!isHtml) {
                    File tmpHtml = File.createTempFile("emailtopdf", ".html");
                    Logger.debug("Write html to temporary file %s", tmpHtml.getAbsolutePath());
                    Files.write(htmlBody, tmpHtml, Charset.forName(charsetName));
                    Logger.debug("Write pdf to %s", pdf_or_html.getAbsolutePath());

                    List<String> cmd = new ArrayList<String>(Arrays.asList("wkhtmltopdf",
                                    "--viewport-size", VIEWPORT_SIZE,
                                    // "--disable-smart-shrinking",
                                    "--dpi", String.valueOf(CONVERSION_DPI),
                                    "--image-quality", String.valueOf(IMAGE_QUALITY),
                                    "--encoding", charsetName));
                    cmd.addAll(extParams);
                    cmd.add(tmpHtml.getAbsolutePath());
                    cmd.add(pdf_or_html.getAbsolutePath());

                    Logger.debug("Execute: %s", Joiner.on(' ').join(cmd));
                    execCommand(cmd);
                    
                    if (!tmpHtml.delete()) {
			tmpHtml.deleteOnExit();
                    }

                    if (tmpHtmlHeader != null) {
                            if (!tmpHtmlHeader.delete()) {
                                    tmpHtmlHeader.deleteOnExit();
                            }
                    }
                } else {
                    Logger.debug("Write html to file %s", pdf_or_html.getAbsolutePath());
                    Files.write(htmlBody, pdf_or_html, Charset.forName(charsetName));
                }
                
		/* ######### Save attachments ######### */
		if (extractAttachments) {
			Logger.debug("Start extracting attachments");
			
			File attachmentDir = null;
			if (!Strings.isNullOrEmpty(attachmentsdir)) {
				attachmentDir = new File(attachmentsdir);
			} else {
				attachmentDir = new File(pdf_or_html.getParentFile(), Files.getNameWithoutExtension(pdfOutputPath) + "-attachments");
			}
                        
			attachmentDir.mkdirs();
			
			Logger.info("Extract attachments to %s", attachmentDir.getAbsolutePath());
			
			List<Part> attachmentParts = MimeMessageParser.getAttachments(message);
			Logger.debug("Found %s attachments", attachmentParts.size());
			for (int i = 0; i < attachmentParts.size(); i++) {
				Logger.debug("Process Attachment %s", i);
				
				Part part = attachmentParts.get(i);
				
				String attachmentFilename = null;
				try {
					attachmentFilename = part.getFileName();
				} catch (Exception e) {
					// ignore this error
				}
				
				File attachFile = null;
				if (!Strings.isNullOrEmpty(attachmentFilename)) {
					attachFile = new File(attachmentDir, attachmentFilename);
				} else {
					String extension = "";
					
					// try to find at least the file extension via the mime type
					try {
						extension = MimeTypes.getDefaultMimeTypes().forName(part.getContentType()).getExtension();
					} catch (Exception e) {
						// ignore this error
					}
					
					Logger.debug("Attachment %s did not hold any name, use random name", i);
					attachFile = File.createTempFile("nameless-", extension, attachmentDir);
				}
				
				Logger.debug("Save Attachment %s to %s", i, attachFile.getAbsolutePath());
				FileOutputStream fos = new FileOutputStream(attachFile);
				ByteStreams.copy(part.getInputStream(), fos);
				fos.close();
			}
		}
		
		Logger.info("Conversion finished");
	}
}