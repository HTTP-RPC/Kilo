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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import static org.httprpc.WebServiceProxy.entry;
import static org.httprpc.WebServiceProxy.mapOf;

public class AddNoteActivity extends AppCompatActivity {
    private EditText messageEditText;

    private static String TAG = AddNoteActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_note);

        messageEditText = (EditText)findViewById(R.id.message_edit_text);

        Button cancelButton = (Button)findViewById(R.id.cancel_button);

        cancelButton.setOnClickListener(v -> {
            finish();
        });

        Button okButton = (Button) findViewById(R.id.ok_button);

        okButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();

            DemoApplication.getServiceProxy().invoke("POST", "/httprpc-server/notes", mapOf(entry("message", message)), (Void result, Exception exception) -> {
                if (exception == null) {
                    finish();
                } else {
                    Log.e(TAG, exception.getMessage());
                }
            });
        });
    }
}
