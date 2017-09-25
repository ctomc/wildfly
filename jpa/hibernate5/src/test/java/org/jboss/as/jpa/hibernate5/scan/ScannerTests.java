/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate5.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ScannerTests {


    /**
     * Directory where shrink-wrap built archives are written
     */
    private static File shrinkwrapArchiveDirectory;

    static {
        shrinkwrapArchiveDirectory = new File("target/packages");
        shrinkwrapArchiveDirectory.mkdirs();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        shrinkwrapArchiveDirectory.delete();
    }


    protected File buildLargeJar() {
        final String fileName = "large.jar";
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, fileName);

        // Build a large jar by adding a lorem ipsum file repeatedly.
        final File loremipsumTxtFile = new File("/org/hibernate/jpa/test/packaging/loremipsum.txt");
        for (int i = 0; i < 100; i++) {
            ArchivePath path = ArchivePaths.create("META-INF/file" + i);
            archive.addAsResource(loremipsumTxtFile, path);
        }

        File testPackage = new File(shrinkwrapArchiveDirectory, fileName);
        archive.as(ZipExporter.class).exportTo(testPackage, true);
        return testPackage;
    }


    @Test
    public void testGetBytesFromInputStream() throws Exception {
        File file = buildLargeJar();
        try {
            long start = System.currentTimeMillis();
            InputStream stream = new BufferedInputStream(
                    new FileInputStream(file));
            int oldLength = getBytesFromInputStream(stream).length;
            stream.close();
            long oldTime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            stream = new BufferedInputStream(new FileInputStream(file));
            int newLength = ArchiveHelper.getBytesFromInputStream(stream).length;
            stream.close();
            long newTime = System.currentTimeMillis() - start;

            assertEquals(oldLength, newLength);
            assertTrue(oldTime > newTime);
        } finally {
            file.delete();
        }
    }

    // This is the old getBytesFromInputStream from JarVisitorFactory before
    // it was changed by HHH-7835. Use it as a regression test.
    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        int size;

        byte[] entryBytes = new byte[0];
        for (; ; ) {
            byte[] tmpByte = new byte[4096];
            size = inputStream.read(tmpByte);
            if (size == -1) { break; }
            byte[] current = new byte[entryBytes.length + size];
            System.arraycopy(entryBytes, 0, current, 0, entryBytes.length);
            System.arraycopy(tmpByte, 0, current, entryBytes.length, size);
            entryBytes = current;
        }
        return entryBytes;
    }

    @Test
    public void testGetBytesFromZeroInputStream() throws Exception {
        // Ensure that JarVisitorFactory#getBytesFromInputStream
        // can handle 0 length streams gracefully.
        URL emptyTxtUrl = getClass().getResource("/org/hibernate/jpa/test/packaging/empty.txt");
        if (emptyTxtUrl == null) {
            throw new RuntimeException("Bah!");
        }
        InputStream emptyStream = new BufferedInputStream(emptyTxtUrl.openStream());
        int length = ArchiveHelper.getBytesFromInputStream(emptyStream).length;
        assertEquals(length, 0);
        emptyStream.close();
    }
}
