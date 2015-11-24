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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.sql.ResultSetAdapter;

/**
 * Test service.
 */
public class TestService extends WebService {
    public double add(double a, double b) {
        return a + b;
    }

    public double addValues(List<Double> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total;
    }

    public boolean invertValue(boolean value) {
        return !value;
    }

    public List<String> getCharacters(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        int n = text.length();

        ArrayList<String> characters = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            characters.add(Character.toString(text.charAt(i)));
        }

        return characters;
    }

    public String getSelection(List<String> items) {
        return String.join(", ", items);
    }

    public Map<String, Object> getStatistics(List<Double> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (double value : values) {
            statistics.setSum(statistics.getSum() + value);
        }

        statistics.setAverage(statistics.getSum() / n);

        return new BeanAdapter(statistics);
    }

    public List<Map<String, Object>> getTestData() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        String url = String.format("jdbc:sqlite::resource:%s/test.db", getClass().getPackage().getName().replace('.', '/'));

        Statement statement = DriverManager.getConnection(url).createStatement();

        return new ResultSetAdapter(statement.executeQuery("select * from test"));
    }

    public void getVoid() {
        // No-op
    }

    public Object getNull() {
        return null;
    }

    public String getLocaleCode() {
        Locale locale = getLocale();

        return locale.getLanguage() + "_" + locale.getCountry();
    }

    @Override
    public String getUserName() {
        return super.getUserName();
    }

    public boolean isUserInRole(String role) {
        return getUserRoles().contains(role);
    }
}
