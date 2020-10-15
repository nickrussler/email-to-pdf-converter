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

package cli;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
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
        for (File f : Files.fileTraverser().depthFirstPreOrder(rootDir)) {
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
    public void main_simplePlainMessage() throws IOException, URISyntaxException {
        File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testPlain.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{
                "-o", tmpPdf.getAbsolutePath(),
                "-s", "Letter",
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
    public void main_htmlMessage() throws IOException, URISyntaxException {
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
    public void main_msg_htmlMessage() throws IOException, URISyntaxException {
        File tmpPdf = File.createTempFile("emailtopdf", ".pdf");
        String msgFilePath = new File(MainTest.class.getClassLoader().getResource("msg/testHtml.msg").toURI()).getAbsolutePath();

        String[] args = new String[] {
                "-o", tmpPdf.getAbsolutePath(),
                msgFilePath
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
    public void main_attachments() throws IOException, URISyntaxException {
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
    public void main_attachmentsSniffFileExtension() throws IOException, URISyntaxException {
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
