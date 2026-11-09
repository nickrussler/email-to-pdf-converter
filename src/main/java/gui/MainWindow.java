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

package gui;

import cli.Main;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Main Window GUI Class.
 *
 * @author Nick Russler
 */
public class MainWindow {

    private JFrame frmEmailToPdf;
    private JTextField textField;
    private JLabel lblConvertingEmail;
    private JProgressBar progressBar;
    private JButton btnStartConversion;
    private JComboBox<String> rendererComboBox;
    private JComboBox<String> orientationComboBox;
    private JTextField executableField;
    private JComboBox<String> browserDownloadComboBox;
    private JTextField browserCacheField;
    private JButton browserCacheBrowseButton;
    private JTextField customStylesheetField;
    private JButton customStylesheetBrowseButton;
    private JLabel executableHint;
    private JLabel browserOptionsHint;
    private JCheckBox chckbxUseProxy;
    private JRadioButton rdbtnAutomaticProxySelection;
    private JRadioButton rdbtnManual;
    private String currentRenderer = "Chrome";

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ignore error
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    MainWindow window = new MainWindow();
                    window.frmEmailToPdf.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public MainWindow() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmEmailToPdf = new JFrame();
        frmEmailToPdf.setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class.getResource("/icons/logo_emlconverter-482_16x16.png")));
        frmEmailToPdf.setResizable(false);
        frmEmailToPdf.setTitle("Email to PDF Converter");
        frmEmailToPdf.setBackground(Color.WHITE);
        frmEmailToPdf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(null);
        contentPanel.setPreferredSize(new Dimension(700, 870));

        JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frmEmailToPdf.setContentPane(contentScrollPane);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 11, 507, 210);
        contentPanel.add(scrollPane);
        scrollPane.setBorder(new TitledBorder(null, "Email Files", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        scrollPane.setBackground(Color.WHITE);

        final JList<String> list = new JList<String>();
        scrollPane.setViewportView(list);
        final DefaultListModel<String> listModel = new DefaultListModel<String>();
        list.setModel(listModel);
        list.setBackground(new Color(255, 255, 255));
        list.setBorder(null);
        listModel.addListDataListener(new ListDataListener() {
            public void contentsChanged() {
                lblConvertingEmail.setText("Email 0 of " + listModel.getSize());
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged();
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                contentsChanged();
            }
        });

        final JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        final JButton btnAddFolder = new JButton("Add Folder");
        btnAddFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dirChooser.showOpenDialog(frmEmailToPdf) == JFileChooser.APPROVE_OPTION) {
                    btnAddFolder.setEnabled(false);
                    frmEmailToPdf.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (File f : Files.fileTraverser().depthFirstPreOrder(dirChooser.getSelectedFile())) {
                                if (f.getName().endsWith(".eml") || f.getName().endsWith(".msg")) {
                                    listModel.addElement(f.getAbsolutePath());
                                }
                            }

                            btnAddFolder.setEnabled(true);
                            frmEmailToPdf.setCursor(Cursor.getDefaultCursor());
                        }
                    });
                }
            }
        });
        btnAddFolder.setIcon(new ImageIcon(MainWindow.class.getResource("/icons/folder_add.png")));
        btnAddFolder.setBounds(527, 72, 140, 34);
        contentPanel.add(btnAddFolder);

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Message files", "eml", "msg"));

        JButton btnAddFile = new JButton("Add File(s)");
        btnAddFile.setIcon(new ImageIcon(MainWindow.class.getResource("/icons/email_add.png")));
        btnAddFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser.showOpenDialog(frmEmailToPdf) == JFileChooser.APPROVE_OPTION) {
                    for (File f : fileChooser.getSelectedFiles()) {
                        listModel.addElement(f.getAbsolutePath());
                    }
                }
            }
        });
        btnAddFile.setBounds(527, 36, 140, 34);
        contentPanel.add(btnAddFile);

        JButton btnClearList = new JButton("Clear List");
        btnClearList.setIcon(new ImageIcon(MainWindow.class.getResource("/icons/cross.png")));
        btnClearList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.clear();
            }
        });
        btnClearList.setBounds(527, 154, 140, 34);
        contentPanel.add(btnClearList);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBackground(Color.WHITE);
        panel.setBounds(10, 232, 664, 296);
        contentPanel.add(panel);
        panel.setLayout(null);

        final JCheckBox chckbxAddEmailHeaders = new JCheckBox("Add Email headers to the PDF document");
        chckbxAddEmailHeaders.setSelected(true);
        chckbxAddEmailHeaders.setBackground(Color.WHITE);
        chckbxAddEmailHeaders.setBounds(10, 17, 632, 23);
        panel.add(chckbxAddEmailHeaders);

        JLabel lblRenderer = new JLabel("Renderer:");
        lblRenderer.setBounds(10, 47, 70, 20);
        panel.add(lblRenderer);

        rendererComboBox = new JComboBox<String>(new String[]{"Chrome", "wkhtmltopdf"});
        rendererComboBox.setName("rendererComboBox");
        rendererComboBox.setSelectedItem("Chrome");
        rendererComboBox.setBackground(Color.WHITE);
        rendererComboBox.setBounds(80, 44, 120, 26);
        rendererComboBox.setToolTipText("Choose the HTML-to-PDF renderer. Chrome is the default.");
        panel.add(rendererComboBox);

        JLabel lblOrientation = new JLabel("Orientation:");
        lblOrientation.setBounds(220, 47, 80, 20);
        panel.add(lblOrientation);

        orientationComboBox = new JComboBox<String>(new String[]{"Portrait", "Landscape"});
        orientationComboBox.setName("orientationComboBox");
        orientationComboBox.setSelectedItem("Portrait");
        orientationComboBox.setBackground(Color.WHITE);
        orientationComboBox.setBounds(300, 44, 120, 26);
        orientationComboBox.setToolTipText("Page orientation. Landscape is best supported by the Chrome renderer.");
        panel.add(orientationComboBox);

        JLabel lblExecutable = new JLabel("Executable:");
        lblExecutable.setBounds(10, 75, 75, 20);
        panel.add(lblExecutable);

        executableField = new JTextField();
        executableField.setName("executableField");
        executableField.setBounds(90, 72, 475, 26);
        executableField.setToolTipText("Optional renderer executable path. Leave blank for automatic selection.");
        panel.add(executableField);

        JButton btnBrowseExecutable = new JButton("Browse");
        btnBrowseExecutable.setBounds(572, 70, 82, 30);
        btnBrowseExecutable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser executableChooser = new JFileChooser();
                executableChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                executableChooser.setDialogTitle("Select renderer executable");
                if (executableChooser.showOpenDialog(frmEmailToPdf) == JFileChooser.APPROVE_OPTION) {
                    executableField.setText(executableChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        panel.add(btnBrowseExecutable);

        executableHint = new JLabel("Automatic when blank");
        executableHint.setFont(new Font("Tahoma", Font.PLAIN, 11));
        executableHint.setForeground(Color.GRAY);
        executableHint.setBounds(90, 99, 475, 18);
        panel.add(executableHint);

        JPanel browserPanel = new JPanel();
        browserPanel.setBorder(new TitledBorder(null, "Chrome Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        browserPanel.setBackground(Color.WHITE);
        browserPanel.setBounds(10, 120, 644, 108);
        browserPanel.setLayout(null);
        panel.add(browserPanel);

        JLabel lblBrowserDownload = new JLabel("Download mode:");
        lblBrowserDownload.setBounds(10, 18, 100, 20);
        browserPanel.add(lblBrowserDownload);

        browserDownloadComboBox = new JComboBox<String>(new String[]{"Auto", "Never", "Once", "Latest", "Force"});
        browserDownloadComboBox.setName("browserDownloadComboBox");
        browserDownloadComboBox.setSelectedItem("Auto");
        browserDownloadComboBox.setBackground(Color.WHITE);
        browserDownloadComboBox.setBounds(112, 15, 120, 26);
        browserPanel.add(browserDownloadComboBox);

        JLabel lblBrowserCache = new JLabel("Cache directory:");
        lblBrowserCache.setBounds(240, 18, 100, 20);
        browserPanel.add(lblBrowserCache);

        browserCacheField = new JTextField();
        browserCacheField.setName("browserCacheField");
        browserCacheField.setBounds(344, 15, 205, 26);
        browserCacheField.setToolTipText("Browser cache directory. Leave blank for the backend default.");
        browserPanel.add(browserCacheField);

        browserCacheBrowseButton = new JButton("Browse");
        browserCacheBrowseButton.setBounds(556, 13, 80, 30);
        browserCacheBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser cacheChooser = new JFileChooser();
                cacheChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                cacheChooser.setDialogTitle("Select browser cache directory");
                if (cacheChooser.showOpenDialog(frmEmailToPdf) == JFileChooser.APPROVE_OPTION) {
                    browserCacheField.setText(cacheChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        browserPanel.add(browserCacheBrowseButton);

        JLabel lblCustomStylesheet = new JLabel("Custom stylesheet:");
        lblCustomStylesheet.setBounds(10, 51, 120, 20);
        browserPanel.add(lblCustomStylesheet);

        customStylesheetField = new JTextField();
        customStylesheetField.setName("customStylesheetField");
        customStylesheetField.setBounds(136, 48, 412, 26);
        customStylesheetField.setToolTipText("Optional Chrome CSS file. Relative URLs are not resolved against the stylesheet directory.");
        browserPanel.add(customStylesheetField);

        customStylesheetBrowseButton = new JButton("Browse");
        customStylesheetBrowseButton.setBounds(556, 46, 80, 30);
        customStylesheetBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser stylesheetChooser = new JFileChooser();
                stylesheetChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                stylesheetChooser.setFileFilter(new FileNameExtensionFilter("CSS files", "css"));
                stylesheetChooser.setDialogTitle("Select custom print stylesheet");
                if (stylesheetChooser.showOpenDialog(frmEmailToPdf) == JFileChooser.APPROVE_OPTION) {
                    customStylesheetField.setText(stylesheetChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        browserPanel.add(customStylesheetBrowseButton);

        browserOptionsHint = new JLabel("Browser acquisition and caching happen automatically during conversion.");
        browserOptionsHint.setFont(new Font("Tahoma", Font.PLAIN, 11));
        browserOptionsHint.setForeground(Color.GRAY);
        browserOptionsHint.setBounds(10, 78, 624, 18);
        browserPanel.add(browserOptionsHint);

        chckbxUseProxy = new JCheckBox("Use Proxy");
        chckbxUseProxy.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                rdbtnAutomaticProxySelection.setEnabled(chckbxUseProxy.isSelected());
                rdbtnManual.setEnabled(chckbxUseProxy.isSelected());

                if (!chckbxUseProxy.isSelected()) {
                    textField.setEnabled(false);
                }
            }
        });
        chckbxUseProxy.setBackground(Color.WHITE);
        chckbxUseProxy.setBounds(10, 239, 86, 23);
        panel.add(chckbxUseProxy);

        rdbtnAutomaticProxySelection = new JRadioButton("Automatic Proxy Selection");
        rdbtnAutomaticProxySelection.setSelected(true);
        rdbtnAutomaticProxySelection.setEnabled(false);
        rdbtnAutomaticProxySelection.setBackground(Color.WHITE);
        rdbtnAutomaticProxySelection.setBounds(98, 239, 180, 23);
        panel.add(rdbtnAutomaticProxySelection);

        textField = new JTextField();
        textField.setEnabled(false);
        textField.setBounds(352, 240, 290, 22);
        panel.add(textField);
        textField.setColumns(10);

        rdbtnManual = new JRadioButton("Manual");
        rdbtnManual.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                textField.setEnabled(rdbtnManual.isSelected());
            }
        });
        rdbtnManual.setEnabled(false);
        rdbtnManual.setBackground(Color.WHITE);
        rdbtnManual.setBounds(280, 239, 66, 23);
        panel.add(rdbtnManual);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rdbtnManual);
        bg.add(rdbtnAutomaticProxySelection);

        final JCheckBox chckbxExtractAttachments = new JCheckBox("Extract Attachments");
        chckbxExtractAttachments.setBackground(Color.WHITE);
        chckbxExtractAttachments.setBounds(10, 266, 150, 23);
        panel.add(chckbxExtractAttachments);

        final JCheckBox chckbxAddAttachmentNames = new JCheckBox("Add Attachment Names");
        chckbxAddAttachmentNames.setBackground(Color.WHITE);
        chckbxAddAttachmentNames.setBounds(160, 266, 180, 23);
        panel.add(chckbxAddAttachmentNames);

        rendererComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedRenderer = String.valueOf(rendererComboBox.getSelectedItem());
                if (!selectedRenderer.equals(currentRenderer)) {
                    currentRenderer = selectedRenderer;
                    executableField.setText("");
                }
                updateRendererOptions();
            }
        });
        executableField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRendererOptions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRendererOptions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRendererOptions();
            }
        });
        browserDownloadComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRendererOptions();
            }
        });
        updateRendererOptions();

        JPanel panelProgress = new JPanel();
        panelProgress.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelProgress.setBackground(Color.WHITE);
        panelProgress.setBounds(10, 536, 664, 127);
        contentPanel.add(panelProgress);
        panelProgress.setLayout(null);

        btnStartConversion = new JButton("Start Conversion");
        btnStartConversion.setIcon(new ImageIcon(MainWindow.class.getResource("/icons/arrow_refresh.png")));
        btnStartConversion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnStartConversion.setEnabled(false);
                frmEmailToPdf.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                String proxyTmp = null;

                if (chckbxUseProxy.isSelected()) {
                    if (rdbtnAutomaticProxySelection.isSelected()) {
                        proxyTmp = "auto";
                    } else {
                        proxyTmp = textField.getText();
                    }
                }

                final String proxy = proxyTmp;
                final String renderer = selectedRenderer();
                final String orientation = selectedOrientation();
                final String executable = executableField.getText();
                final String browserDownload = selectedBrowserDownload();
                final String browserCacheDir = browserCacheField.getText();
                final String customStylesheetPath = customStylesheetField.getText();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startConversion(
                                Collections.list(listModel.elements()), renderer, orientation, executable, browserDownload, browserCacheDir, customStylesheetPath, chckbxAddEmailHeaders.isSelected(), proxy, chckbxAddAttachmentNames.isSelected(), chckbxExtractAttachments.isSelected());
                    }
                }).start();
            }
        });
        btnStartConversion.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnStartConversion.setBounds(117, 75, 425, 41);
        panelProgress.add(btnStartConversion);

        progressBar = new JProgressBar();
        progressBar.setBounds(10, 32, 644, 32);
        panelProgress.add(progressBar);

        lblConvertingEmail = new JLabel("Email 0 of 0");
        lblConvertingEmail.setBounds(12, 8, 400, 14);
        panelProgress.add(lblConvertingEmail);

        JButton btnRemoveSelectedFrom = new JButton("Remove selected");
        btnRemoveSelectedFrom.setIcon(new ImageIcon(MainWindow.class.getResource("/icons/email_delete.png")));
        btnRemoveSelectedFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (String s : list.getSelectedValuesList()) {
                    listModel.removeElement(s);
                }
            }
        });
        btnRemoveSelectedFrom.setBounds(527, 117, 140, 34);
        contentPanel.add(btnRemoveSelectedFrom);

        JScrollPane scrollPaneLog = new JScrollPane();
        scrollPaneLog.setBounds(10, 674, 664, 183);
        scrollPaneLog.setBorder(new TitledBorder(null, "Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        scrollPaneLog.setBackground(Color.WHITE);
        contentPanel.add(scrollPaneLog);

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        textArea.setForeground(Color.GREEN);
        textArea.setBackground(Color.BLACK);
        textArea.setEditable(false);
        scrollPaneLog.setViewportView(textArea);

        MessageConsole mc = new MessageConsole(textArea);
        try {
            mc.redirectOut(Color.GREEN, null);
            mc.redirectErr(Color.RED, null);
        } catch (UnsupportedEncodingException e1) {
        }

        mc.setContainer(scrollPaneLog);
        mc.setMessageLines(1000);

        frmEmailToPdf.pack();
        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension packedSize = frmEmailToPdf.getSize();
        int scrollBarWidth = contentScrollPane.getVerticalScrollBar().getPreferredSize().width;
        frmEmailToPdf.setSize(Math.min(packedSize.width + scrollBarWidth, maxBounds.width), Math.min(packedSize.height, maxBounds.height));
        frmEmailToPdf.setLocationRelativeTo(null);
    }

    private void updateRendererOptions() {
        boolean chrome = isChromeSelected();
        boolean executableOverride = !isBlank(executableField.getText());
        boolean downloadOptionsEnabled = chrome && !executableOverride;

        browserDownloadComboBox.setEnabled(downloadOptionsEnabled);
        browserCacheField.setEnabled(downloadOptionsEnabled);
        browserCacheBrowseButton.setEnabled(downloadOptionsEnabled);
        customStylesheetField.setEnabled(chrome);
        customStylesheetBrowseButton.setEnabled(chrome);
        customStylesheetField.setToolTipText(chrome ? "Optional Chrome CSS file. Relative URLs are not resolved against the stylesheet directory." : "Chrome only; not used by wkhtmltopdf.");
        executableHint.setText(chrome ? "Automatic when blank" : "Installed wkhtmltopdf when blank");

        if (!chrome) {
            browserOptionsHint.setText("Chrome-only settings; not used by wkhtmltopdf.");
        } else if (executableOverride) {
            browserOptionsHint.setText("Download mode and cache are ignored while an executable override is set.");
        } else if ("Force".equals(browserDownloadComboBox.getSelectedItem())) {
            browserOptionsHint.setText("Force downloads a fresh browser for each email.");
        } else {
            browserOptionsHint.setText("Browser acquisition and caching happen automatically during conversion.");
        }

        browserDownloadComboBox.setToolTipText(browserDownloadTooltip(selectedBrowserDownload()));
        updateProxyHelp();
    }

    private void updateProxyHelp() {
        String help;
        if (isChromeSelected()) {
            help = "Chrome uses this proxy for browser downloads and external email resources. Proxy authentication is unsupported.";
        } else {
            help = "wkhtmltopdf uses this proxy for email resource requests.";
        }
        chckbxUseProxy.setToolTipText(help);
        rdbtnAutomaticProxySelection.setToolTipText(help);
        rdbtnManual.setToolTipText(help);
        textField.setToolTipText(help);
    }

    private boolean isChromeSelected() {
        return rendererComboBox == null || "Chrome".equals(rendererComboBox.getSelectedItem());
    }

    private String selectedRenderer() {
        return isChromeSelected() ? "chrome" : "wkhtmltopdf";
    }

    private String selectedOrientation() {
        Object selected = orientationComboBox == null ? "Portrait" : orientationComboBox.getSelectedItem();
        return String.valueOf(selected).toLowerCase(Locale.ROOT);
    }

    private String selectedBrowserDownload() {
        Object selected = browserDownloadComboBox == null ? "Auto" : browserDownloadComboBox.getSelectedItem();
        return String.valueOf(selected).toLowerCase(Locale.ROOT);
    }

    private static String browserDownloadTooltip(String mode) {
        if ("never".equals(mode)) {
            return "Use an installed browser or cached shell; never download.";
        }
        if ("once".equals(mode)) {
            return "Use the managed cached shell or download it; skip installed browsers.";
        }
        if ("latest".equals(mode)) {
            return "Check stable browser metadata; download only when that version is not cached.";
        }
        if ("force".equals(mode)) {
            return "Download a fresh browser for each email.";
        }
        return "Use an installed browser, a usable cached shell, or download the stable shell.";
    }

    private static boolean isBlank(String value) {
        return Strings.isNullOrEmpty(value) || value.trim().isEmpty();
    }

    static List<String> buildConversionArguments(
                                                 String renderer, String orientation, String executable, String browserDownload, String browserCacheDir, String customStylesheetPath, boolean showHeaders, String proxy, boolean addAttachmentNames, boolean extractAttachments) {
        List<String> args = new ArrayList<String>();

        if (!showHeaders) {
            args.add("--hide-headers");
        }

        if (!Strings.isNullOrEmpty(proxy)) {
            args.add("--proxy");
            args.add(proxy);
        }

        if (addAttachmentNames) {
            args.add("--add-attachment-names");
        }

        if (extractAttachments) {
            args.add("--extract-attachments");
        }

        String rendererName = "wkhtmltopdf".equalsIgnoreCase(renderer) ? "wkhtmltopdf" : "chrome";
        args.add("--renderer");
        args.add(rendererName);
        args.add("--orientation");
        args.add("landscape".equalsIgnoreCase(orientation) ? "landscape" : "portrait");

        if ("chrome".equals(rendererName) && !isBlank(customStylesheetPath)) {
            args.add("--custom-stylesheet-path");
            args.add(customStylesheetPath);
        }

        if (!isBlank(executable)) {
            args.add("--renderer-executable");
            args.add(executable);
        } else if ("chrome".equals(rendererName)) {
            if (!isBlank(browserDownload)) {
                args.add("--browser-download");
                args.add(browserDownload);
            }
            if (!isBlank(browserCacheDir)) {
                args.add("--browser-cache-dir");
                args.add(browserCacheDir);
            }
        }

        return args;
    }

    /**
     * Start converting the email files.
     */
    private void startConversion(
                                 List<String> l, String renderer, String orientation, String executable, String browserDownload, String browserCacheDir, String customStylesheetPath, boolean showHeaders, String proxy, boolean addAttachmentNames, boolean extractAttachments) {
        try {
            ArrayList<String> argsOptions = new ArrayList<String>(
                    buildConversionArguments(renderer, orientation, executable, browserDownload, browserCacheDir, customStylesheetPath, showHeaders, proxy, addAttachmentNames, extractAttachments));

            int listSize = l.size();
            for (int i = 0; i < listSize; i++) {
                String[] args = new String[argsOptions.size() + 1];
                argsOptions.toArray(args);
                args[args.length - 1] = l.get(i);

                Main.main(args);

                final String text = "Email " + (i + 1) + " of " + listSize;
                final int percent = (int) Math.ceil(((i + 1d) * 100d) / listSize);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        lblConvertingEmail.setText(text);
                        progressBar.setValue(percent);
                    }
                });
            }
        } finally {
            btnStartConversion.setEnabled(true);
            frmEmailToPdf.setCursor(Cursor.getDefaultCursor());
        }
    }
}
