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

module org.httprpc.kilo.client {
    requires java.sql;
    requires java.xml;

    exports org.httprpc.kilo;
    exports org.httprpc.kilo.beans;
    exports org.httprpc.kilo.io;
    exports org.httprpc.kilo.sql;
    exports org.httprpc.kilo.util;
    exports org.httprpc.kilo.util.concurrent;
    exports org.httprpc.kilo.xml;
}
