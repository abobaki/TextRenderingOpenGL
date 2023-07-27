package de.thkoeln.abobaki.android.opengl_textrendering_demo;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import de.thkoeln.abobaki.android.opengl_textrendering.SchriftzeichenUtility;


public class MainActivity extends ListActivity {

    private final String[] list = {"Textdesign bearbeiten", "Text-Index bearbeiten"};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SchriftzeichenUtility.initialisierung(this);
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        setListAdapter(adapter);
        setTitle(R.string.app_name);
    }

    @Override
    protected void onListItemClick(ListView liste, View datenElement, int position, long id) {
        super.onListItemClick(liste, datenElement, position, id);
        String text = ((TextView) datenElement).getText().toString();
        if (text.equals("Textdesign bearbeiten")) {
            startActivity(new Intent(this, TextDesignActivity.class));
        } else {
            startActivity(new Intent(this, TextIndexActivity.class));
        }
    }
}
