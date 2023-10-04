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

package org.httprpc.kilo.test;

import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.beans.Key;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class UsersTest {
    public interface User {
        String getName();
        String getEmail();
        Address getAddress();
    }

    public interface Address {
        @Key("zipcode")
        String getZipCode();
        @Key("geo")
        Location getLocation();
    }

    public interface Location {
        @Key("lat")
        Double getLatitude();
        @Key("lng")
        Double getLongitude();
    }

    public interface UserServiceProxy {
        @RequestMethod("GET")
        @ResourcePath("users")
        List<User> getUsers();
    }

    public static void main(String[] args) throws IOException {
        testUsers1();
        testUsers2();
    }

    private static void testUsers1() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", new URL("https://jsonplaceholder.typicode.com/users"));

        var users = webServiceProxy.invoke(result -> BeanAdapter.coerceList((List<?>)result, User.class));

        listUsers(users);
    }

    private static void testUsers2() throws IOException {
        var userServiceProxy = WebServiceProxy.of(UserServiceProxy.class, new URL("https://jsonplaceholder.typicode.com/"));

        var users = userServiceProxy.getUsers();

        listUsers(users);
    }

    private static void listUsers(List<User> users) {
        var i = 0;

        for (var user : users) {
            if (i > 0) {
                System.out.println();
            }

            System.out.printf("Name = %s\n", user.getName());
            System.out.printf("Email = %s\n", user.getEmail());

            var address = user.getAddress();

            System.out.printf("Zip Code = %s\n", address.getZipCode());

            var location = address.getLocation();

            System.out.printf("Location = %f, %f\n", location.getLatitude(), location.getLongitude());

            i++;
        }
    }
}
