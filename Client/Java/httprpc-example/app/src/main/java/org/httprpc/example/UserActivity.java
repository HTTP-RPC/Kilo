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

package org.httprpc.example;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {
    private ListView userListView;

    private List<Map<String, ?>> userList = null;

    private BaseAdapter userListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return userList.size();
        }

        @Override
        public Object getItem(int position) {
            return userList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return ((Number)userList.get(position).get("id")).longValue();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_user, null);
            }

            Map<String, ?> note = userList.get(position);

            String name = (String)note.get("name");

            TextView nameTextView = convertView.findViewById(R.id.name_text_view);
            nameTextView.setText(name);

            String email = (String)note.get("email");

            TextView emailTextView = convertView.findViewById(R.id.email_text_view);
            emailTextView.setText(email);

            return convertView;
        }
    };

    private static String TAG = UserActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user);

        userListView = findViewById(R.id.user_list_view);

        userListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(UserActivity.this, PostActivity.class);

            intent.putExtra(PostActivity.USER_ID_KEY, id);

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userList == null) {
            ExampleApplication.getServiceProxy().invoke("GET", "/users",
                (List<Map<String, ?>> result, Exception exception) -> {
                if (exception == null) {
                    userList = result;

                    userListView.setAdapter(userListAdapter);
                } else {
                    Log.e(TAG, exception.getMessage());
                }
            });
        }
    }
}
