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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class PostActivity extends AppCompatActivity {
    private ListView postListView;

    private List<Map<String, ?>> postList = null;

    private BaseAdapter postListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return postList.size();
        }

        @Override
        public Object getItem(int position) {
            return postList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return ((Number)postList.get(position).get("id")).longValue();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_post, null);
            }

            Map<String, ?> note = postList.get(position);

            String title = (String)note.get("title");

            TextView titleTextView = (TextView)convertView.findViewById(R.id.title_text_view);
            titleTextView.setText(title);

            String body = (String)note.get("body");

            TextView bodyTextView = (TextView)convertView.findViewById(R.id.body_text_view);
            bodyTextView.setText(body);

            return convertView;
        }
    };

    public static final String USER_ID_KEY = "userID";

    private static String TAG = PostActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post);

        postListView = (ListView)findViewById(R.id.post_list_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (postList == null) {
            long userID = getIntent().getLongExtra(USER_ID_KEY, 0);

            ExampleApplication.getServiceProxy().invoke("GET", "/posts", mapOf(entry("userId", userID)),
                (List<Map<String, ?>> result, Exception exception) -> {
                if (exception == null) {
                    postList = result;

                    postListView.setAdapter(postListAdapter);
                } else {
                    Log.e(TAG, exception.getMessage());
                }
            });
        }
    }
}

