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

import javax.mail.internet.ContentType;
/**
 * Wrapper class that is used to bundle an object with it's contentType.
 * @author Nick Russler
 *
 * @param <T> generic type of the entry
 */
public class MimeObjectEntry<T> {
    private T entry;
    private ContentType contentType;

    public MimeObjectEntry(T entry, ContentType contentType) {
        this.entry = entry;
        this.contentType = contentType;
    }

    public T getEntry() {
        return entry;
    }

    public void setEntry(T entry) {
        this.entry = entry;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }
}
