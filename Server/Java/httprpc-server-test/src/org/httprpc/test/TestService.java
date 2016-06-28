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

package org.httprpc.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.httprpc.RPC;
import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.sql.Parameters;
import org.httprpc.sql.ResultSetAdapter;

public class TestService extends WebService {
    @RPC(method="GET", path="sum")
    public double getSum(double a, double b) {
        return a + b;
    }

    @RPC(method="GET", path="sum")
    public double getSum(List<Double> values) {
        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total;
    }

    @RPC(method="GET", path="inverse")
    public boolean getInverse(boolean value) {
        return !value;
    }

    @RPC(method="GET", path="characters")
    public static List<String> getCharacters(String text) {
        List<String> characters = null;

        if (text != null) {
            int n = text.length();

            characters = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                characters.add(Character.toString(text.charAt(i)));
            }
        }

        return characters;
    }

    @RPC(method="POST", path="selection")
    public String getSelection(List<String> items) {
        return String.join(", ", items);
    }

    @RPC(method="GET", path="tree")
    public Map<String, ?> getTree() {
        TreeNode root = new TreeNode("Seasons", false);

        TreeNode winter = new TreeNode("Winter", false);
        winter.getChildren().addAll(Arrays.asList(new TreeNode("January"), new TreeNode("February"), new TreeNode("March")));

        root.getChildren().add(winter);

        TreeNode spring = new TreeNode("Spring", false);
        spring.getChildren().addAll(Arrays.asList(new TreeNode("April"), new TreeNode("May"), new TreeNode("June")));

        root.getChildren().add(spring);

        TreeNode summer = new TreeNode("Summer", false);
        summer.getChildren().addAll(Arrays.asList(new TreeNode("July"), new TreeNode("August"), new TreeNode("September")));

        root.getChildren().add(summer);

        TreeNode fall = new TreeNode("Fall", false);
        fall.getChildren().addAll(Arrays.asList(new TreeNode("October"), new TreeNode("November"), new TreeNode("December")));

        root.getChildren().add(fall);

        return new BeanAdapter(root);
    }

    @RPC(method="PUT")
    public String put(String value) {
        return value;
    }

    @RPC(method="DELETE", path="/")
    public int delete(int value) {
        return value;
    }

    @RPC(method="POST", path="statistics")
    public Map<String, ?> getStatistics(List<Double> values) {
        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (int i = 0; i < n; i++) {
            statistics.setSum(statistics.getSum() + values.get(i));
        }

        statistics.setAverage(statistics.getSum() / n);

        return new BeanAdapter(statistics);
    }

    @RPC(method="GET", path="testData")
    public ResultSetAdapter getTestData() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.sqlite.JDBC");

        String url = String.format("jdbc:sqlite::resource:%s/test.db", getClass().getPackage().getName().replace('.', '/'));

        String sql = "select * from test where a=:a or b=:b or c=coalesce(:c, 4.0)";

        Parameters parameters = Parameters.parse(new StringReader(sql));
        PreparedStatement statement = DriverManager.getConnection(url).prepareStatement(parameters.getSQL());

        parameters.apply(statement, mapOf(entry("a", "hello"), entry("b", 3)));

        return new ResultSetAdapter(statement.executeQuery());
    }

    @RPC(method="GET", path="void")
    public void getVoid() {
        // No-op
    }

    @RPC(method="GET", path="null")
    public String getNull() {
        return null;
    }

    @RPC(method="GET", path="localeCode")
    public String getLocaleCode() {
        Locale locale = getLocale();

        return locale.getLanguage() + "_" + locale.getCountry();
    }

    @Override
    @RPC(method="GET", path="user/name")
    public String getUserName() {
        return super.getUserName();
    }

    @RPC(method="GET", path="user/roleStatus")
    public boolean getUserRoleStatus(String role) {
        return getUserRoles().contains(role);
    }

    @RPC(method="POST", path="/attachmentInfo")
    public Map<String, ?> getAttachmentInfo(String text, List<URL> attachments) throws IOException {
        LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

        for (URL url : attachments) {
            long bytes = 0;
            long checksum = 0;

            try (InputStream inputStream = url.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    bytes++;
                    checksum += b;
                }
            }

            attachmentInfo.add(mapOf(
                entry("bytes", bytes),
                entry("checksum", checksum))
            );
        }

        return mapOf(
            entry("text", text),
            entry("attachmentInfo", attachmentInfo)
        );
    }

    @RPC(method="GET", path="/longList")
    public List<Integer> getLongList() {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return index;
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
    }

    @RPC(method="GET", path="/delayedResult")
    public String getDelayedResult(String result, int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}
