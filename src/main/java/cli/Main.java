/*
 * Copyright 2016 Nick Russler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cli;

import com.beust.jcommander.JCommander;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import gui.MainWindow;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import mimeparser.MimeMessageConverter;
import render.RenderOptions;
import util.HttpUtils;
import util.LogLevel;
import util.Logger;

/**
 * Commandline tool to convert an email (eml, msg) to html and pdf.
 *
 * @author Nick Russler
 */
public class Main {
    public static void main(String[] args) {
        CommandLineParameters cli = new CommandLineParameters();
        JCommander jCmd = new JCommander(cli);
        jCmd.setProgramName("EmailToPDFConverter");
        jCmd.parse(args);

        if (cli.isGui()) {
            MainWindow.main(new String[0]);
            return;
        }

        if (cli.isHelp()) {
            jCmd.usage();
            return;
        }

        if (cli.isVersion()) {
            System.out.println(Main.class.getPackage().getImplementationVersion());
            return;
        }

        if (cli.isDebug()) {
            Logger.level = LogLevel.Debug;
        }

        if (cli.isError()) {
            Logger.level = LogLevel.Error;
        }

        if (cli.isQuiet()) {
            Logger.level = LogLevel.Quiet;
        }

        if (cli.getFiles().isEmpty()) {
            Logger.error("Please provide the path of an email (eml, msg) file.");
            jCmd.usage();
            return;
        }

        String in = cli.getFiles().get(0);

        if (!(new File(in).exists())) {
            Logger.error("Input email file %s could not be found!", in);
            return;
        }

        String out = cli.getOutput();

        if (Strings.isNullOrEmpty(cli.getOutput())) {
            out = Files.getNameWithoutExtension(in) + ".pdf";

            File parent = new File(in).getParentFile();
            if (parent != null) {
                out = new File(parent, out).toString();
            }
        }

        String proxy = null;

        if ("auto".equalsIgnoreCase(cli.getProxy())) {
            Proxy defaultProxy = HttpUtils.getDefaultProxy();

            if (defaultProxy == null) {
                Logger.error("Default proxy could not be determined, please specify it manually");
                return;
            }

            InetSocketAddress defaultProxyAddress = (InetSocketAddress) defaultProxy.address();
            proxy = defaultProxy.type().toString() + "://" + defaultProxyAddress.toString();
            proxy = proxy.toLowerCase();
            Logger.debug("Use default proxy %s", proxy);
        } else if (!Strings.isNullOrEmpty(cli.getProxy())) {
            proxy = cli.getProxy();
            Logger.debug("Use proxy from parameters %s", proxy);
        }

        RenderOptions renderOptions = new RenderOptions(
                RenderOptions.Renderer.WKHTMLTOPDF, null, proxy, cli.getPageSize(), cli.getDPI(), false, null);

        try {
            MimeMessageConverter.convertToPdf(
                    in, out, cli.isHideHeaders(), cli.isAddAttachmentNames(), cli.isExtractAttachments(), cli.getExtractAttachmentsDir(), renderOptions);
        } catch (Exception e) {
            Logger.error("The email could not be converted. Error: %s", Throwables.getStackTraceAsString(e));
        }
    }
}
