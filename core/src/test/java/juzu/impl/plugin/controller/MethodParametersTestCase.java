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

package juzu.impl.plugin.controller;

import juzu.impl.inject.spi.InjectorProvider;
import juzu.test.AbstractInjectTestCase;
import juzu.test.protocol.mock.MockActionBridge;
import juzu.test.protocol.mock.MockApplication;
import juzu.test.protocol.mock.MockClient;
import juzu.test.protocol.mock.MockRenderBridge;
import org.junit.Test;

import java.util.Arrays;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class MethodParametersTestCase extends AbstractInjectTestCase {

  public MethodParametersTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void testStringArray() throws Exception {
    MockApplication<?> app = application("plugin.controller.method.parameters.string.array").init();

    //
    MockClient client = app.client();

    //
    MockRenderBridge render = client.render("none");
    MockRenderBridge mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals("", mv.assertStringResult());

    //
    render = client.render("0");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals("", mv.assertStringResult());

    //
    render = client.render("1");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals(Arrays.asList("bar").toString(), mv.assertStringResult());

    //
    render = client.render("2");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals(Arrays.asList("bar_1", "bar_2").toString(), mv.assertStringResult());
  }

  @Test
  public void testStringList() throws Exception {
    MockApplication<?> app = application("plugin.controller.method.parameters.string.list").init();

    //
    MockClient client = app.client();

    //
    MockRenderBridge render = client.render("none");
    MockRenderBridge mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals("", mv.assertStringResult());

    //
    render = client.render("0");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals("", mv.assertStringResult());

    //
    render = client.render("1");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals(Arrays.asList("bar").toString(), mv.assertStringResult());

    //
    render = client.render("2");
    mv = (MockRenderBridge)client.invoke(render.assertStringResult());
    assertEquals(Arrays.asList("bar_1", "bar_2").toString(), mv.assertStringResult());
  }

  @Test
  public void testBean() throws Exception {
    MockApplication<?> app = application("plugin.controller.method.parameters.bean").init();

    //
    MockClient client = app.client();

    //
    assertBean(client, "a", "v");
    assertBean(client, "b", Arrays.asList("v1", "v2").toString());
    assertBean(client, "c", Arrays.asList("v1", "v2").toString());
    assertBean(client, "d", "v");
    assertBean(client, "e", Arrays.asList("v1", "v2").toString());
    assertBean(client, "f", Arrays.asList("v1", "v2").toString());
    assertBean(client, "g", "s_valuev");
    assertBean(client, "g", "s_valuev");
    assertBean(client, "h", "s_valuev");

  }

  private void assertBean(MockClient client, String name, Object expected) throws Exception {
    MockRenderBridge render = client.render(name);
    String url = render.assertStringResult();
    MockRenderBridge m = (MockRenderBridge)client.invoke(url);
    assertEquals(expected, m.assertStringResult());

    render = client.render(name + "Action");
    url = render.assertStringResult();
    MockActionBridge action = (MockActionBridge)client.invoke(url);
    MockRenderBridge m2 = (MockRenderBridge)client.invoke(action.assertUpdate());
    assertEquals(expected, m2.assertStringResult());
  }

  public static boolean WAS_NULL;

  @Test
  public void testUnresolvedContext() throws Exception {
    MockApplication<?> app = application("plugin.controller.method.parameters.context.unresolved").init();

    //
    MockClient client = app.client();

    //
    MockRenderBridge render = client.render("index");

    assertTrue(WAS_NULL);

//    MockRenderBridge mv = (MockRenderBridge)client.invoke(render.assertStringResult());
  }

  @Test
  public void testContextualFactory() throws Exception {
    // We just check that compilation happens
    MockApplication<?> app = application("plugin.controller.method.parameters.context.factory").init();
  }
}
