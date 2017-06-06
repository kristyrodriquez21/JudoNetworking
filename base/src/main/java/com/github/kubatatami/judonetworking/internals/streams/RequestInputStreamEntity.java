/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.github.kubatatami.judonetworking.internals.streams;

import com.github.kubatatami.judonetworking.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A streamed, non-repeatable entity that obtains its content from
 * an {@link InputStream}.
 */
public class RequestInputStreamEntity implements StreamEntity {

    private final InputStream content;

    private final long length;

    private final boolean binary;

    public RequestInputStreamEntity(final InputStream instream, long length) {
        this(instream, length, false);
    }

    public RequestInputStreamEntity(final InputStream instream, long length, boolean binary) {
        if (instream == null) {
            throw new IllegalArgumentException("Source input stream may not be null");
        }
        this.content = instream;
        this.length = length;
        this.binary = binary;

    }

    @Override
    public long getContentLength() {
        return this.length;
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        FileUtils.copyStreamOrCountBytes(outstream, content, length);
    }

    @Override
    public void close() throws IOException {
        this.content.close();
    }

    @Override
    public String getLog() throws IOException {
        if (binary) {
            if (length >= 0) {
                return "Binary body size: " + length;
            } else {
                return "Binary body";
            }
        } else {
            String result = FileUtils.convertStreamToString(content);
            content.reset();
            return result;
        }
    }
}