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

package vellum.webrpc.test;

import java.security.Principal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vellum.webrpc.WebRPCService;
import vellum.webrpc.sql.ResultSetAdapter;

/**
 * Test service.
 */
public class TestService extends WebRPCService {
    /**
     * Adds two numbers.
     *
     * @param a
     * The first number.
     *
     * @param b
     * The second number.
     *
     * @return
     * The sum of the given numbers.
     */
    public double add(double a, double b) {
        return a + b;
    }

    /**
     * Adds a list of numbers.
     *
     * @param values
     * The numbers to add.
     *
     * @return
     * The sum of the given numbers.
     */
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

    /**
     * Splits a string into a list of characters.
     *
     * @param text
     * The string to split.
     *
     * @return
     * The list of characters.
     */
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

    /**
     * Returns a comma-delimited representation of the given selection.
     *
     * @param selection
     * The selected items.
     *
     * @return
     * A string representing the selected items.
     */
    public String getSelection(List<String> items) {
        return String.join(", ", items);
    }

    /**
     * Calculates statistics about a list of numbers.
     *
     * @param values
     * The numbers for which the statistics will be calculated.
     *
     * @return
     * Statistics about the given numbers.
     */
    public Statistics getStatistics(List<Double> values) {
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

        return statistics;
    }

    /**
     * Returns the contents of a test database table.
     *
     * @return
     * The contents of the test table.
     */
    public ResultSetAdapter getTestData() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        String url = String.format("jdbc:sqlite::resource:%s/test.db", getClass().getPackage().getName().replace('.', '/'));

        Connection connection = DriverManager.getConnection(url);

        Statement statement = connection.createStatement();

        return new ResultSetAdapter(statement.executeQuery("select * from test"));
    }

    /**
     * Returns nothing.
     */
    public void getVoid() {
    }

    /**
     * Returns the null value.
     *
     * @return
     * The <tt>null</tt> value.
     */
    public Object getNull() {
        return null;
    }

    /**
     * Returns the locale code associated with the current request as
     * <i>&lt;language&gt;_&lt;country&gt;</i>.
     *
     * @return
     * The locale code of the current request.
     */
    public String getLocaleCode() {
        Locale locale = getLocale();

        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     * Returns the user name associated with the current request.
     *
     * @return
     * The name of the current user.
     */
    public String getUserName() {
        Principal userPrincipal = getUserPrincipal();

        return (userPrincipal == null) ? null : userPrincipal.getName();
    }

    @Override
    public boolean isUserInRole(String role) {
        return super.isUserInRole(role);
    }
}
