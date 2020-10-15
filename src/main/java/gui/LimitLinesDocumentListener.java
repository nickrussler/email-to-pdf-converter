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

package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A class to control the maximum number of lines to be stored in a Document
 *
 * Excess lines can be removed from the start or end of the Document depending
 * on your requirement.
 *
 * a) if you append text to the Document, then you would want to remove lines
 * from the start. b) if you insert text at the beginning of the Document, then
 * you would want to remove lines from the end.
 */
public class LimitLinesDocumentListener implements DocumentListener {
    private int maximumLines;
    private boolean isRemoveFromStart;

    /*
     * Specify the number of lines to be stored in the Document. Extra lines
     * will be removed from the start of the Document.
     */
    public LimitLinesDocumentListener(int maximumLines) {
        this(maximumLines, true);
    }

    /*
     * Specify the number of lines to be stored in the Document. Extra lines
     * will be removed from the start or end of the Document, depending on the
     * boolean value specified.
     */
    public LimitLinesDocumentListener(int maximumLines, boolean isRemoveFromStart) {
        setLimitLines(maximumLines);
        this.isRemoveFromStart = isRemoveFromStart;
    }

    /*
     * Return the maximum number of lines to be stored in the Document
     */
    public int getLimitLines() {
        return maximumLines;
    }

    /*
     * Set the maximum number of lines to be stored in the Document
     */
    public void setLimitLines(int maximumLines) {
        if (maximumLines < 1) {
            String message = "Maximum lines must be greater than 0";
            throw new IllegalArgumentException(message);
        }

        this.maximumLines = maximumLines;
    }

    // Handle insertion of new text into the Document

    @Override
    public void insertUpdate(final DocumentEvent e) {
        // Changes to the Document can not be done within the listener
        // so we need to add the processing to the end of the EDT

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                removeLines(e);
            }
        });
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    /*
     * Remove lines from the Document when necessary
     */
    private void removeLines(DocumentEvent e) {
        // The root Element of the Document will tell us the total number
        // of line in the Document.

        Document document = e.getDocument();
        Element root = document.getDefaultRootElement();

        while (root.getElementCount() > maximumLines) {
            if (isRemoveFromStart) {
                removeFromStart(document, root);
            } else {
                removeFromEnd(document, root);
            }
        }
    }

    /*
     * Remove lines from the start of the Document
     */
    private void removeFromStart(Document document, Element root) {
        Element line = root.getElement(0);
        int end = line.getEndOffset();

        try {
            document.remove(0, end);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
    }

    /*
     * Remove lines from the end of the Document
     */
    private void removeFromEnd(Document document, Element root) {
        // We use start minus 1 to make sure we remove the newline
        // character of the previous line

        Element line = root.getElement(root.getElementCount() - 1);
        int start = line.getStartOffset();
        int end = line.getEndOffset();

        try {
            document.remove(start - 1, end - start);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
    }
}
