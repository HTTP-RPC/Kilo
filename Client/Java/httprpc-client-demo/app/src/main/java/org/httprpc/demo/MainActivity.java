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

package org.httprpc.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class MainActivity extends AppCompatActivity {
    private ListView noteListView;
    private Button deleteButton;

    private List<Map<String, ?>> noteList = Collections.emptyList();

    private BaseAdapter noteListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return noteList.size();
        }

        @Override
        public Object getItem(int position) {
            return noteList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return ((Number)noteList.get(position).get("id")).longValue();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_note, null);
            }

            Map<String, ?> note = noteList.get(position);

            String message = (String)note.get("message");

            TextView messageTextView = (TextView)convertView.findViewById(R.id.message_text_view);
            messageTextView.setText(message);

            Date date = new Date(((Number)note.get("date")).longValue());

            TextView dateTextView = (TextView)convertView.findViewById(R.id.date_text_view);
            dateTextView.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date));

            return convertView;
        }
    };

    private static String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        noteListView = (ListView)findViewById(R.id.note_list_view);

        noteListView.setOnItemClickListener((parent, view, position, id) -> {
            deleteButton.setEnabled(true);
        });

        noteListView.setAdapter(noteListAdapter);

        deleteButton = (Button)findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(v -> {
            final int position = noteListView.getCheckedItemPosition();

            int id = ((Number)noteList.get(position).get("id")).intValue();

            DemoApplication.getServiceProxy().invoke("DELETE", "/httprpc-server/notes", mapOf(entry("id", id)), (Void result, Exception exception) -> {
                if (exception == null) {
                    noteList.remove(position);
                    noteListAdapter.notifyDataSetChanged();

                    deleteButton.setEnabled(false);
                } else {
                    Log.e(TAG, exception.getMessage());
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        deleteButton.setEnabled(false);

        DemoApplication.getServiceProxy().invoke("GET", "/httprpc-server/notes", (List<Map<String, ?>> result, Exception exception) -> {
            if (exception == null) {
                noteList = result;

                noteListAdapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, exception.getMessage());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        boolean result;
        switch (id) {
            case R.id.add_note_item: {
                startActivity(new Intent(this, AddNoteActivity.class));

                result = true;

                break;
            }

            default: {
                result = false;

                break;
            }
        }

        return result;
    }
}
