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

import juzu.impl.common.Tools;

import java.io.IOException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public interface Streamable {

  public static class CharSequence implements Streamable {

    private final java.lang.CharSequence s;

    public CharSequence(java.lang.CharSequence s) {
      this.s = s;
    }

    public void send(Stream stream) throws IOException {
      try {
        stream.append(s);
      }
      finally {
        Tools.safeClose(stream);
      }
    }
  }

  public static class InputStream implements Streamable {

    /** . */
    private final java.io.InputStream in;

    public InputStream(java.io.InputStream in) {
      this.in = in;
    }

    public void send(Stream stream) throws IOException {
      byte[] buffer = new byte[256];
      try {
        for (int l;(l = in.read(buffer)) != -1;) {
          stream.append(buffer, 0, l);
        }
      }
      finally {
        Tools.safeClose(stream);
      }
    }
  }

  public static class Bytes implements Streamable {

    private final byte[] s;

    public Bytes(byte[] s) {
      this.s = s;
    }

    public void send(Stream stream) throws IOException {
      try {
        stream.append(s);
      }
      finally {
        Tools.safeClose(stream);
      }
    }
  }

  void send(Stream stream) throws IOException;

}
