/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.texai.network.netty.handler;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 *
 */
public final class CompactObjectInputStream extends ObjectInputStream {

  /** Constructs a new CompactObjectInputStream instance.
   *
   * @param inputStream the input stream
   * @throws IOException if an input/output error occurs
   */
  public CompactObjectInputStream(final InputStream inputStream) throws IOException {
    super(inputStream);
  }

  /** Reads and verifies the magic number and version number.
   *
   * @throws IOException if there are I/O errors while reading from the
   *   underlying <code>InputStream</code>
   */
  @Override
  protected void readStreamHeader() throws IOException {
    final int version = readByte() & 0xFF;
    if (version != STREAM_VERSION) {
      throw new StreamCorruptedException("Unsupported version: " + version);
    }
  }

  /** Reads a class descriptor from the serialization stream.  This method is
   * called when the ObjectInputStream expects a class descriptor as the next
   * item in the serialization stream.
   *
   * @return the class descriptor read
   * @throws IOException If an I/O error has occurred.
   * @throws ClassNotFoundException If the Class of a serialized object used
   *   in the class descriptor representation cannot be found
   */
  @Override
  protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
    final int type = read();
    if (type < 0) {
      throw new EOFException();
    }
    switch (type) {
      case CompactObjectOutputStream.TYPE_FAT_DESCRIPTOR:
        return super.readClassDescriptor();
      case CompactObjectOutputStream.TYPE_THIN_DESCRIPTOR:
        final String className = readUTF();
        final Class<?> clazz = loadClass(className);
        return ObjectStreamClass.lookup(clazz);
      default:
        throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
    }
  }

  /** Loads the local class equivalent of the specified stream class
   * description.
   *
   * @param   objectStreamClass an instance of class <code>ObjectStreamClass</code>
   * @return  a <code>Class</code> object corresponding to <code>desc</code>
   * @throws  IOException any of the usual Input/Output exceptions.
   * @throws  ClassNotFoundException if class of a serialized object cannot
   *          be found.
   */
  @Override
  protected Class<?> resolveClass(final ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
    final String className = objectStreamClass.getName();
    try {
      return loadClass(className);
    } catch (ClassNotFoundException ex) {
      return super.resolveClass(objectStreamClass);
    }
  }

  /** Loads the class.
   *
   * @param className the class name
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  protected Class<?> loadClass(final String className) throws ClassNotFoundException {
    Class<?> clazz;
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    if (classLoader == null) {
      clazz = Class.forName(className);
    } else {
      clazz = classLoader.loadClass(className);
    }
    return clazz;
  }
}
