/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.xml;

import org.httprpc.kilo.beans.Key;

import java.util.List;
import java.util.Map;

public interface TestInterface {
    interface MapInterface {
        @Key("@b")
        String getB1();

        @Key("b")
        String getB2();

        ListInterface getList();
    }

    interface ListInterface {
        @Key("@c")
        String getC();

        @Key("item*")
        List<String> getStringItems();

        @Key("item*")
        List<Map<String, Object>> getMapItems();
    }

    @Key("@a")
    String getA();

    MapInterface getMap();
}
