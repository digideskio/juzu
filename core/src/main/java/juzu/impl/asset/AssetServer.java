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

package juzu.impl.asset;

import juzu.impl.plugin.application.ApplicationLifeCycle;
import juzu.impl.common.Tools;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class AssetServer {

  /** . */
  HashSet<ApplicationLifeCycle<?, ?>> runtimes = new HashSet<ApplicationLifeCycle<?, ?>>();

  public AssetServer() {
  }

  public void register(ApplicationLifeCycle<?, ?> assetManager) {
    runtimes.add(assetManager);
  }

  public void unregister(ApplicationLifeCycle<?, ?> assetManager) {
    runtimes.remove(assetManager);
  }

  public boolean doGet(String path, ServletContext ctx, HttpServletResponse resp) throws ServletException, IOException {
    if (path != null && path.length() > 0) {
      for (ApplicationLifeCycle<?, ?> runtime : runtimes) {
        String contentType;
        InputStream in;
        URL url = runtime.getScriptManager().resolveAsset(path);
        if (url != null) {
          contentType = "text/javascript";
          in = url.openStream();
        } else {
          contentType = null;
          in = null;
        }
        if (in == null) {
          url = runtime.getStylesheetManager().resolveAsset(path);
          if (url != null) {
            contentType = "text/css";
            in = runtime.getApplication().getClassLoader().getResourceAsStream(path.substring(1));
          }
        }

        // It could be a server resource like an image
        if (in == null) {
          in = ctx.getResourceAsStream(path);
          if (in != null) {
            int pos = path.lastIndexOf('/');
            String name = pos == -1 ? path : path.substring(pos + 1);
            contentType = ctx.getMimeType(name);
          } else {
            contentType = null;
            in = null;
          }
        }
        if (in != null) {
          if (contentType != null) {
            resp.setContentType(contentType);
          }
          Tools.copy(in, resp.getOutputStream());
          return true;
        }
      }
    }
    return false;
  }
}
