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

package org.httprpc.beans;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestBean {
    public static class NestedBean {
        public boolean getI() {
            return true;
        }
    }

    public long getA() {
        return 2L;
    }

    public double getB() {
        return 4.0;
    }

    public String getC() {
        return "abc";
    }

    public Date getD() {
        return new Date(0);
    }

    public NestedBean getE() {
        return new NestedBean();
    }

    public List<NestedBean> getF() {
        return Collections.singletonList(new NestedBean());
    }

    public Map<String, NestedBean> getG() {
        return Collections.singletonMap("h", new NestedBean());
    }
}
