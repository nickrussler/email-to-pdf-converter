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

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.Test;

import util.LogLevel;
import util.Logger;

import com.google.common.base.Throwables;
import com.google.common.io.Files;

/**
 * TestClass.
 * @author Nick Russler
 */
public class MainTest {

	public static void convertFolderTest() throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("d:\\mylog.txt", "UTF-8");
		writer.println("Log start time: " + new Date());
		writer.println("-----------------------");
		writer.flush();

		Logger.level = LogLevel.Debug;

		String outPath = System.getProperty("user.home") + "/Desktop/test.pdf";

		File rootDir = new File("D:\\devel\\email_training\\");
		for (File f : Files.fileTreeTraverser().preOrderTraversal(rootDir)) {
			try {
				if (f.isDirectory()) {
					continue;
				}

				System.out.println(f.getAbsolutePath());
				writer.println("Process file: " + f.getAbsolutePath());

				Main.main(new String[] {
						"-d",
						"-o", outPath,
						f.getAbsolutePath()
				});


				// Desktop.getDesktop().open(new File(outPath));
			} catch (Exception e) {
				System.out.println(f.getAbsolutePath() + " - " + e.getMessage());

				writer.println(f.getAbsolutePath() + " - " + e.getMessage());
				writer.println(Throwables.getStackTraceAsString(e));
				writer.println("##########################");
				writer.flush();
			}
		}

		writer.println("-----------------------");
		writer.println("Log end time: " + new Date());
		writer.flush();

		writer.close();
	}

	@Test
	public void main_simplePlainMessage() throws MessagingException, IOException, URISyntaxException {
		File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
		String eml = new File(MainTest.class.getClassLoader().getResource("eml/testPlain.eml").toURI()).getAbsolutePath();

		String[] args = new String[]{
				"-o", tmpPdf.getAbsolutePath(),
				eml
		};

		LogLevel old = Logger.level;
		Logger.level = LogLevel.Error;

		Main.main(args);

		Logger.level = old;

		assertTrue(tmpPdf.exists());
		// assertTrue(tmpPdf.length() > 0);

		if (!tmpPdf.delete()) {
			tmpPdf.deleteOnExit();
		}
	}

	@Test
	public void main_htmlMessage() throws MessagingException, IOException, URISyntaxException {
		File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
		String eml = new File(MainTest.class.getClassLoader().getResource("eml/testHtml.eml").toURI()).getAbsolutePath();

		String[] args = new String[]{
				"-o", tmpPdf.getAbsolutePath(),
				eml
		};

		LogLevel old = Logger.level;
		Logger.level = LogLevel.Error;

		Main.main(args);

		Logger.level = old;

		assertTrue(tmpPdf.exists());
		// assertTrue(tmpPdf.length() > 0);

		if (!tmpPdf.delete()) {
			tmpPdf.deleteOnExit();
		}
	}

	@Test
	public void main_attachments() throws MessagingException, IOException, URISyntaxException {
		File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
		String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachments.eml").toURI()).getAbsolutePath();

		String[] args = new String[]{
				"-o", tmpPdf.getAbsolutePath(),
				"-a",
				eml
		};

		LogLevel old = Logger.level;
		Logger.level = LogLevel.Error;

		Main.main(args);

		Logger.level = old;

		File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) +  "-attachments");

		List<String> attachments = Arrays.asList(attachmentDir.list());
		assertThat(attachments, hasItems("IMAG0144.jpg", "IMAG0144.jpg"));

		if (!tmpPdf.delete()) {
			tmpPdf.deleteOnExit();
		}

		for (String fileName : attachments) {
			File f = new File(attachmentDir, fileName);
			if (!f.delete()) {
				f.deleteOnExit();
			}
		}

		if (!attachmentDir.delete()) {
			attachmentDir.deleteOnExit();
		}
	}

	@Test
	public void main_attachmentsSniffFileExtension() throws MessagingException, IOException, URISyntaxException {
		File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
		String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachmentsNoName.eml").toURI()).getAbsolutePath();

		String[] args = new String[]{
				"-o", tmpPdf.getAbsolutePath(),
				"-a",
				eml
		};

		LogLevel old = Logger.level;
		Logger.level = LogLevel.Error;

		Main.main(args);

		Logger.level = old;

		File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) +  "-attachments");

		List<String> attachments = Arrays.asList(attachmentDir.list());
		assertTrue(attachments.get(0).endsWith(".jpg"));

		if (!tmpPdf.delete()) {
			tmpPdf.deleteOnExit();
		}

		for (String fileName : attachments) {
			File f = new File(attachmentDir, fileName);
			if (!f.delete()) {
				f.deleteOnExit();
			}
		}

		if (!attachmentDir.delete()) {
			attachmentDir.deleteOnExit();
		}
	}
}
