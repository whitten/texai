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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/** A compact object output stream.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * reformatted and commented by Stephen L. Reed, Texai.org, stephenreed@yahoo.com
 */
class CompactObjectOutputStream extends ObjectOutputStream {

  /** the fat type descriptor */
  static final int TYPE_FAT_DESCRIPTOR = 0;
  /** the thin type descriptor */
  static final int TYPE_THIN_DESCRIPTOR = 1;

  /** Constructs a new CompactObjectOutputStream instance.
   *
   * @param outputStream the output stream
   * @throws IOException if an input/output error occurs
   */
  public CompactObjectOutputStream(final OutputStream outputStream) throws IOException {
    super(outputStream);
  }

  /** Writes the magic number and version to the stream.
   *
   * @throws IOException if I/O errors occur while writing to the underlying
   *   stream
   */
  @Override
  protected void writeStreamHeader() throws IOException {
    writeByte(STREAM_VERSION);
  }

  /** Write the specified class descriptor to the ObjectOutputStream.  Class
   * descriptors are used to identify the classes of objects written to the
   * stream.
   *
   * @param objectStreamClass class descriptor to write to the stream
   * @throws IOException If an I/O error has occurred.
   */
  @Override
  protected void writeClassDescriptor(final ObjectStreamClass objectStreamClass) throws IOException {
    //Preconditions
    assert objectStreamClass != null : "objectStreamClass must not be null";

    final Class<?> clazz = objectStreamClass.forClass();
    if (clazz.isPrimitive() || clazz.isArray()) {
      write(TYPE_FAT_DESCRIPTOR);
      super.writeClassDescriptor(objectStreamClass);
    } else {
      write(TYPE_THIN_DESCRIPTOR);
      writeUTF(objectStreamClass.getName());
    }
  }
}
