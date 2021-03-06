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

package juzu.test.protocol.mock;

import juzu.Response;
import juzu.impl.bridge.spi.RenderBridge;
import juzu.impl.common.MethodHandle;
import juzu.impl.plugin.application.ApplicationLifeCycle;

import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class MockRenderBridge extends MockMimeBridge implements RenderBridge {

  public MockRenderBridge(ApplicationLifeCycle<?, ?> application, MockClient client, MethodHandle target, Map<String, String[]> parameters) {
    super(application, client, target, parameters);
  }

  public String getTitle() {
    if (response instanceof Response.Content) {
      Response.Content stream = (Response.Content)response;
      return stream.getTitle();
    } else {
      return null;
    }
  }
}
