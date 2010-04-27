/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 *
 * @since Apache Tika 0.8
 */
public class TikaInputStream extends ProxyInputStream {

    /**
     * Casts or wraps the given stream to a TikaInputStream instance.
     * This method can be used to access the functionality of this class
     * even when given just a normal input stream instance.
     *
     * @param stream normal input stream
     * @return a TikaInputStream instance
     */
    public static TikaInputStream get(InputStream stream) {
        if (stream instanceof TikaInputStream) {
            return (TikaInputStream) stream;
        } else {
            return new TikaInputStream(stream);
        }
    }

    /**
     * The file that contains the contents of this stream. This is either
     * the original file passed to the {@link #TikaInputStream(File)}
     * constructor or a temporary file created by a call to the
     * {@link #getFile()} method. If neither has been called, then
     * the value is <code>null</code>.
     */
    private File file;

    /**
     * Flag to indicate that {@link #file} is a temporary file that should
     * be removed when this stream is {@link #close() closed}.
     */
    private boolean temporary;

    private long length;

    /**
     * Current read position within this stream.
     */
    private long position = 0;

    private TikaInputStream(InputStream stream, File file, long length) {
        super(stream);
        this.file = file;
        this.temporary = (file == null);
        this.length = length;
    }

    public TikaInputStream(InputStream stream) {
        this(stream, null, -1);
    }

    public TikaInputStream(byte[] data) {
        this(new ByteArrayInputStream(data), null, data.length);
    }

    public TikaInputStream(File file) throws IOException {
        this(new FileInputStream(file), file, file.length());
    }

    public TikaInputStream(URL url) throws IOException {
        this(url.openStream(), null, -1);
    }

    public File getFile() throws IOException {
        if (file == null) {
            if (in == null) {
                throw new IOException("Stream has already been read");
            } else if (position > 0) {
                throw new IOException("Stream is already being read");
            } else {
                file = File.createTempFile("apache-tika-", ".tmp");
                OutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    out.close();
                }
                in.close();
                in = null;
            }
        }
        return file;
    }

    public long getLength() throws IOException {
        if (length == -1) {
            length = getFile().length();
        }
        return length;
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
        if (file != null) {
            if (temporary) {
                file.delete();
            }
            file = null;
        }
    }

    @Override
    protected void beforeRead(int n) throws IOException {
        if (in == null) {
            if (file != null) {
                in = new FileInputStream(file);
            } else {
                throw new IOException("End of the stream reached");
            }
        }
    }

    @Override
    protected void afterRead(int n) throws IOException {
        if (n != -1) {
            position += n;
        } else {
            close();
        }
    }

}
