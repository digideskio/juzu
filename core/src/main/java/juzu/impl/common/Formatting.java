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
package juzu.impl.common;

import juzu.bridge.portlet.JuzuPortlet;
import juzu.impl.compiler.CompilationError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Various utilities for formatting stuff.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class Formatting {

  /**
   * Append a script section that will add a <code>stylesheet</code> element in the head section of
   * the document.
   *
   * @param writer the writer
   * @throws IOException any io exception
   */
  public static void renderStyleSheet(Writer writer) throws IOException {
    // Get CSS
    URL cssURL = Formatting.class.getResource("juzu.css");
    String css = Tools.read(cssURL);
    css = css.replace("\"", "\\\"");
    css = css.replace("'", "\\'");
    css = css.replace("\n", "\\n");

    //
    writer.append("<script type='text/javascript'>\n");
    writer.append("var styleElement = document.createElement('style');\n");
    writer.append("var css = '");
    writer.append(css);
    writer.append("';\n");
    writer.append("styleElement.type = 'text/css';\n");
    writer.append("if (styleElement.styleSheet) {;\n");
    writer.append("styleElement.styleSheet.cssText = css;\n");
    writer.append("} else {\n");
    writer.append("styleElement.appendChild(document.createTextNode(css));\n");
    writer.append("}\n");
    writer.append("document.getElementsByTagName(\"head\")[0].appendChild(styleElement);\n");
    writer.append("</script>\n");
  }

  /**
   * Renders the throwable in the specified writer.
   *
   * @param writer the writer
   * @param t the throwable
   * @throws IOException any io exception
   */
  public static void renderThrowable(Class<?> stop, Writer writer, Throwable t) throws IOException {
    // Trim the stack trace to remove stuff we don't want to see
    int size = 0;
    StackTraceElement[] trace = t.getStackTrace();
    for (StackTraceElement element : trace) {
      if (stop != null && element.getClassName().equals(stop.getName())) {
        break;
      }
      else {
        size++;
      }
    }
    StackTraceElement[] ourTrace = new StackTraceElement[size];
    System.arraycopy(trace, 0, ourTrace, 0, ourTrace.length);
    t.setStackTrace(ourTrace);

    // We hack a bit
    final AtomicBoolean open = new AtomicBoolean(false);
    PrintWriter formatter = new PrintWriter(writer) {
      @Override
      public void println(Object x) {
        if (open.get()) {
          super.append("</ul></div>");
        }
        super.append("<p>");
        super.append(String.valueOf(x));
        super.append("</p>");
        open.set(false);
      }

      @Override
      public void println(String x) {
        if (!open.get()) {
          super.append("<div class=\"code\"><ul>");
          open.set(true);
        }
        super.append("<li><p>");
        super.append(x);
        super.append("</p></li>");
      }

      @Override
      public void println() {
        // Do nothing
      }
    };

    //
    writer.append("<section>");

    // We hack a bit with our formatter
    t.printStackTrace(formatter);

    //
    if (open.get()) {
      writer.append("</ul></div>");
    }

    //
    writer.append("</section>");
  }

  /**
   * Renders an iterable of {@link CompilationError} in the specified appendable.
   *
   * @param writer the writer
   * @param errors the errors
   * @throws IOException any io exception
   */
  public static void renderErrors(Writer writer, Iterable<CompilationError> errors) throws IOException {
    renderStyleSheet(writer);

    //
    for (CompilationError error : errors) {
      writer.append("<section>");
      writer.append("<p>").append(error.getMessage()).append("</p>");

      // Display the source code
      File source = error.getSourceFile();
      if (source != null) {
        int line = error.getLocation().getLine();
        int from = line - 2;
        int to = line + 3;
        BufferedReader reader = new BufferedReader(new FileReader(source));
        int count = 1;
        writer.append("<div class=\"code\"><ol start=\"").append(String.valueOf(from)).append("\">");
        for (String s = reader.readLine();s != null;s = reader.readLine()) {
          if (count >= from && count < to) {
            if (count == line) {
              writer.append("<li><p class=\"error\">").append(s).append("</p></li>");
            }
            else {
              writer.append("<li><p>").append(s).append("</p></li>");
            }
          }
          count++;
        }
        writer.append("</ol></div>");
      }
      writer.append("</section>");
    }
  }
}
