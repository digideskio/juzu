/*
 * Copyright 2013 eXo Platform SAS
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

package juzu.io;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class BinaryOutputStream extends BinaryStream {

  /** . */
  private final OutputStream out;

  public BinaryOutputStream(Charset charset, OutputStream out) {
    super(charset);

    //
    this.out = out;
  }

  @Override
  public BinaryStream append(byte[] data, int off, int len) throws IOException {
    out.write(data, off, len);
    return this;
  }

  @Override
  public BinaryStream append(byte[] data) throws IOException {
    out.write(data);
    return this;
  }

  public void flush() throws IOException {
    out.flush();
  }

  public void close() throws IOException {
    out.close();
  }
}
