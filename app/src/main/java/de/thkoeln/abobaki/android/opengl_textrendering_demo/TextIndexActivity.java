package de.thkoeln.abobaki.android.opengl_textrendering_demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import de.thkoeln.abobaki.android.opengl_textrendering.SchriftzeichenUtility;
import de.thkoeln.abobaki.android.opengl_textrendering_demo.R.id;
import de.thkoeln.cvogt.android.opengl_utilities.GLAnimatorFactoryCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLRendererCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLShapeCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLSurfaceViewCV;

public class TextIndexActivity extends Activity {

    private GLSurfaceViewCV glSurfaceView;
    private GLRendererCV renderer;
    private GLShapeCV[] shapes;
    private String text;

    private void textBearbeiten(GLSurfaceViewCV sV) {
        sV.clearShapes();
        text = "Hello World";
        long start = System.nanoTime();
        shapes = SchriftzeichenUtility.textDarstellen(text, true);
        long duration = System.nanoTime() - start;
        int numberOfTriangle = 0;
        for (GLShapeCV shapeCV : shapes) {
            sV.addShape(shapeCV);
            numberOfTriangle += shapeCV.getNumberOfTriangles();
        }
        String toast = "Anzahl von Dreiecke =" + numberOfTriangle + "\nAnzahl von Vertices =" + SchriftzeichenUtility.anzahlVertices(text) + "\nZeitdauer ist " + duration / 1000000 + "ms";
        SchriftzeichenUtility.toastAnzeigen(TextIndexActivity.this, toast, Toast.LENGTH_LONG);
        setContentView(sV);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        renderer = new GLRendererCV();
        glSurfaceView = new GLSurfaceViewCV(this, renderer, false);
        textBearbeiten(glSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = new MenuInflater(this);
        mi.inflate(R.menu.menu_advanced, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        renderer.resetEyePosAndFocus();

        if (item.getItemId() == R.id.itemControlViewmatrix) {
            final PopupWindow pw = renderer.getViewMatrixControlPopup(this);
            pw.showAtLocation(glSurfaceView, Gravity.TOP, 0, 0);
            int width = getWindowManager().getDefaultDisplay().getWidth() - 40;
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            pw.update(0, 0, width, height);
        }

        if (item.getItemId() == R.id.menuEditSentence) {
            final PopupWindow pw = new TextIndexControlPopup(this);
            pw.showAtLocation(glSurfaceView, Gravity.TOP, 0, 0);
            int width = getWindowManager().getDefaultDisplay().getWidth() - 40;
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            pw.update(0, 0, width, height);
        }
        return true;
    }

    private class TextIndexControlPopup extends PopupWindow implements View.OnClickListener {

        private final EditText editText;
        private final EditText addIndex;

        public TextIndexControlPopup(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.layout_popup_edit_text, null, false);
            setContentView(layout);
            setFocusable(true);

            editText = layout.findViewById(R.id.addLetter);
            addIndex = layout.findViewById(R.id.addIndex);
            addIndex.setText("1");
            Button btnAdd = layout.findViewById(R.id.addButton);
            btnAdd.setOnClickListener(this);
            Button btnRemove = layout.findViewById(R.id.removeButton);
            btnRemove.setOnClickListener(this);
            Button btnEdit = layout.findViewById(R.id.editButton);
            btnEdit.setOnClickListener(this);
            Button rotX = layout.findViewById(R.id.rotationButtonX);
            rotX.setOnClickListener(this);
            Button rotY = layout.findViewById(R.id.rotationButtonY);
            rotY.setOnClickListener(this);
            Button rotZ = layout.findViewById(R.id.rotationButtonZ);
            rotZ.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String inputText = addIndex.getText().toString();
            String editString =  editText.getText().toString();
            int index;
            int viewID = view.getId();

            if(viewID == id.addButton && editString.isEmpty()){
                SchriftzeichenUtility.toastAnzeigen(TextIndexActivity.this, "Bitte geben Sie einen Text zum Hinzufügen ein.", Toast.LENGTH_LONG);
                return;
            } else if(inputText.isEmpty()) {
                SchriftzeichenUtility.toastAnzeigen(TextIndexActivity.this, "Der Index fehlt. Bitte geben Sie einen ein.", Toast.LENGTH_LONG);
                return;
            } else {
                index = Integer.parseInt(inputText) - 1;
            }

            if(viewID == id.addButton && text.length()==0 ){
                index = 0;
            } else if (index <= -1) {
                SchriftzeichenUtility.toastAnzeigen(TextIndexActivity.this, "Bitte geben Sie einen positiven Indexwert ein.", Toast.LENGTH_LONG);
                return;
            } else if ((viewID != id.addButton && index >= text.length()) || (viewID == id.addButton && index > text.length())) {
                SchriftzeichenUtility.toastAnzeigen(TextIndexActivity.this,
                        "Bitte geben Sie einen Index ein, der innerhalb des gültigen Bereichs liegt :"+text.length(), Toast.LENGTH_LONG);
                return;
            }

            glSurfaceView.clearShapes();
            if (viewID == id.addButton) {
                if (text != null) {
                    text = text.substring(0, index) + editString + text.substring(index);
                } else {
                    text = editString;
                }
                shapes = SchriftzeichenUtility.textDarstellen(text, false);
            } else if (viewID == id.removeButton) {
                text = text.substring(0, index) + text.substring(index + 1);
                shapes = SchriftzeichenUtility.textDarstellen(text, false);
            } else if (viewID == id.editButton) {
                char c = editString.charAt(0);
                text = text.substring(0, index) + c + text.substring(index + 1);
                shapes = SchriftzeichenUtility.textDarstellen(text, false);
            } else if (viewID == id.rotationButtonX) {
                shapes[index].addAnimator(GLAnimatorFactoryCV.makeAnimRotX(360, 5000, 4, false));
            } else if (viewID == id.rotationButtonY) {
                shapes[index].addAnimator(GLAnimatorFactoryCV.makeAnimRotY(360, 5000, 4, false));
            } else if (viewID == id.rotationButtonZ) {
                shapes[index].addAnimator(GLAnimatorFactoryCV.makeAnimRotZ(360, 5000, 4, false));
            }
            for (GLShapeCV shapeCV : shapes) {
                glSurfaceView.addShape(shapeCV);
            }
        }
    }

}
