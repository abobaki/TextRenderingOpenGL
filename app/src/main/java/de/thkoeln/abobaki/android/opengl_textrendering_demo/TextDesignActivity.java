package de.thkoeln.abobaki.android.opengl_textrendering_demo;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.thkoeln.abobaki.android.opengl_textrendering.SchriftzeichenUtility;
import de.thkoeln.cvogt.android.opengl_utilities.GLAnimatorFactoryCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLRendererCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLShapeCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLSurfaceViewCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLTouchEventCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLUtilsCV;

public class TextDesignActivity extends Activity {

    private GLSurfaceViewCV glSurfaceView;
    private GLRendererCV renderer;
    private GLShapeCV[] shapes;
    private String text;
    private String farbeAlsString;
    private float abstandZwWoerter;
    private float schriftzeichenGroesse;
    private int abstandSeekbarPosition;
    private int groesseSeekbarPosition = 50;

    private void textBearbeiten(GLSurfaceViewCV surfaceView) {
        surfaceView.clearShapes();
        text = "Bitte geben Sie einen Text ein" ;
        long start = System.nanoTime();
        shapes = SchriftzeichenUtility.textDarstellen(text, true);
        long duration = System.nanoTime() - start;
        int numberOfTriangle = 0;
        for (GLShapeCV shapeCV : shapes) {
            shapeCV.setLineWidth(8f);
            surfaceView.addShape(shapeCV);
            numberOfTriangle += shapeCV.getNumberOfTriangles();
        }
        String toast = "Anzahl von Dreiecke =" + numberOfTriangle  + "\nAnzahl von Vertices =" + SchriftzeichenUtility.anzahlVertices(text)
                + "\nZeitdauer ist "+duration/1000000 + "ms";
        SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, toast, Toast.LENGTH_LONG);

        surfaceView.addOnTouchListener(new OnTouchListenerForFling(this));
        setContentView(surfaceView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderer = new GLRendererCV();
        glSurfaceView = new GLSurfaceViewCV(this, renderer, false);
        schriftzeichenGroesse = 6.5f;
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
            final PopupWindow pw = new TextDesignControlPopup(this);
            pw.showAtLocation(glSurfaceView, Gravity.TOP, 0, 0);
            int width = getWindowManager().getDefaultDisplay().getWidth() - 40;
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            pw.update(0, 0, width, height);
        }
        return true;
    }

    private class TextDesignControlPopup extends PopupWindow implements AdapterView.OnItemSelectedListener, View.OnClickListener {

        private final SeekBar distanceSeekbar;
        private final SeekBar fontSeekbar;
        private final EditText textHinzufuegen;

        private TextDesignControlPopup(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.layout_popup_viewtext, null, false);
            setContentView(layout);
            setFocusable(true);
            textHinzufuegen = layout.findViewById(R.id.editText);
            Button btnText = layout.findViewById(R.id.btnForText);
            btnText.setOnClickListener(this);
            Spinner spinner = layout.findViewById(R.id.colorSpinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, de.thkoeln.abobaki.android.opengl_textrendering.R.array.colors, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            int spinnerPosition = adapter.getPosition(farbeAlsString);
            spinner.setSelection(spinnerPosition);
            spinner.setOnItemSelectedListener(this);
            distanceSeekbar = layout.findViewById(de.thkoeln.abobaki.android.opengl_textrendering.R.id.distanceSeekbar);
            distanceSeekbar.setOnSeekBarChangeListener(new TextDesignControlPopup.SeekbarsListener());
            distanceSeekbar.setProgress(abstandSeekbarPosition);
            fontSeekbar = layout.findViewById(de.thkoeln.abobaki.android.opengl_textrendering.R.id.fontSeekbar);
            fontSeekbar.setOnSeekBarChangeListener(new TextDesignControlPopup.SeekbarsListener());
            fontSeekbar.setProgress(groesseSeekbarPosition);
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            setFarbeAlsString(adapterView.getItemAtPosition(i).toString());
            SchriftzeichenUtility.farbeAendern(shapes, farbeAlsString);
            glSurfaceView.requestRender();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }

        @Override
        public void onClick(View v) {
            neueTextHinzufuegen(textHinzufuegen.getText().toString());
        }

        private void neueTextHinzufuegen(String eingabe) {
            int verticesNumbers = 0;
            glSurfaceView.clearShapes();
            setText(eingabe.trim());
            long start4 = System.nanoTime();
            shapes = SchriftzeichenUtility.textDarstellen(text,schriftzeichenGroesse,abstandZwWoerter,true);
            SchriftzeichenUtility.farbeAendern(shapes, farbeAlsString);
            long duration4 = System.nanoTime() - start4;

            for (GLShapeCV shapeCV : shapes) {
                glSurfaceView.addShape(shapeCV);
                verticesNumbers = verticesNumbers + shapeCV.getNumberOfTriangles();
            }

            String toast = "Anzahl von Dreiecke =" + verticesNumbers  + "\nAnzahl von Vertices =" +
                    SchriftzeichenUtility.anzahlVertices(text) + "\nZeitdauer ist "+duration4/1000000 + "ms";
            SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, toast, Toast.LENGTH_LONG);

        }

        private class SeekbarsListener implements SeekBar.OnSeekBarChangeListener {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean bool) {
                if (bool) {
                    if(schriftzeichenGroesse == 0)
                        schriftzeichenGroesse = 6.5f;
                    if (seekBar == distanceSeekbar) {
                         abstandZwWoerter = i / 100f;
                        abstandSeekbarPosition = i;

                    } else if (seekBar == fontSeekbar) {
                        schriftzeichenGroesse = i*0.09f + 2;
                        groesseSeekbarPosition = i;
                    }

                    SchriftzeichenUtility.textDarstellen(shapes, text, schriftzeichenGroesse, abstandZwWoerter,false);
                    glSurfaceView.requestRender();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        }
    }

    private class OnTouchListenerForFling implements GLSurfaceViewCV.GLOnTouchListenerCV {

        private final GestureDetector gestureDetector;
        private GLShapeCV touchedShape;

        public OnTouchListenerForFling(Context context) {
            gestureDetector = new GestureDetector(context, new FlingGestureListener(this));
        }

        public GLShapeCV getTouchedShape() {
            return touchedShape;
        }

        public void clearTouchedShape() {
            touchedShape = null;
        }

        @Override
        public boolean onTouch(GLSurfaceViewCV surfaceView, GLTouchEventCV event) {
            float[] rayStart = event.getRayStart();
            float[] rayVector = event.getRayVector();
            MotionEvent mEvent = event.getMotionEvent();
            List<GLShapeCV> shapeList = Arrays.asList(shapes);
            ArrayList<GLShapeCV> glshapes = new ArrayList<>(shapeList);

            if (mEvent.getAction() == MotionEvent.ACTION_DOWN)
                touchedShape = GLUtilsCV.closestShapeOnRay(rayStart, rayVector, glshapes);
            gestureDetector.onTouchEvent(mEvent);
            return true;
        }

        class FlingGestureListener extends GestureDetector.SimpleOnGestureListener {

            OnTouchListenerForFling listener;
            private FlingGestureListener(OnTouchListenerForFling listener) {
                this.listener = listener;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                 touchedShape = listener.getTouchedShape();
                 Random random = new Random();
                 int rand = random.nextInt(3);
                 if (touchedShape != null) {
                    SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, "Double Touch", Toast.LENGTH_SHORT);
                    if(rand == 0) {
                        touchedShape.addAnimator(GLAnimatorFactoryCV.makeAnimRotY(360, 6000, 1, false));
                    } else if (rand == 1) {
                        touchedShape.addAnimator(GLAnimatorFactoryCV.makeAnimRotX(360, 6000, 1, false));
                    } else {
                        touchedShape.addAnimator(GLAnimatorFactoryCV.makeAnimRotZ(360, 6000, 1, false));
                    }
                    touchedShape.startAnimators();
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                touchedShape = listener.getTouchedShape();
                float vectorX = e2.getX() - e1.getX();
                float vectorY = -(e2.getY() - e1.getY());
                String direction = SchriftzeichenUtility.getRichtung(e1.getX(),e1.getY(),e2.getX(),e2.getY());

                if (touchedShape != null) {
                    SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, direction, Toast.LENGTH_SHORT);
                    switch (direction) {
                        case "up":
                        case "down":
                            int ind = SchriftzeichenUtility.indexSchriftzeichen(shapes, touchedShape);
                            shapes = SchriftzeichenUtility.schriftzeichenEntfernen(shapes, touchedShape);
                            double sqrt = Math.sqrt(vectorX * vectorX + vectorY * vectorY);
                            ObjectAnimator anim = GLAnimatorFactoryCV.makeAnimTransLinearBy(vectorX, vectorY, touchedShape.getTransZ(), (int) (5000000 / (float) sqrt));
                            anim.addListener(new GLAnimatorFactoryCV.EndListenerRemove(touchedShape, glSurfaceView));
                            touchedShape.addAnimator(anim);
                            touchedShape.startAnimators();
                            listener.clearTouchedShape();
                            SchriftzeichenUtility.elementEntfernen(ind);
                            setText(SchriftzeichenUtility.zeichenEntfernenAnIndexOhneLeerzeichen(text, ind + 1));
                            new Thread(() -> {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                SchriftzeichenUtility.textDarstellen(shapes, text, schriftzeichenGroesse, abstandZwWoerter, false);
                            }).start();
                            break;
                        case "left":
                            SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, "left", Toast.LENGTH_SHORT);
                            setText(SchriftzeichenUtility.tauscheSchriftzeichen(shapes, text, touchedShape, "left"));
                            SchriftzeichenUtility.textDarstellen(shapes, text, schriftzeichenGroesse, abstandZwWoerter, false);
                            listener.clearTouchedShape();
                            break;
                        case "right":
                            SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, "right", Toast.LENGTH_SHORT);
                            setText(SchriftzeichenUtility.tauscheSchriftzeichen(shapes, text,  touchedShape, "right"));
                            SchriftzeichenUtility.textDarstellen(shapes, text, schriftzeichenGroesse, abstandZwWoerter, false);
                            listener.clearTouchedShape();
                            break;
                        default:
                            return false;
                    }
                    return true;
                } else SchriftzeichenUtility.toastAnzeigen(TextDesignActivity.this, "NOTHING TOUCHED", Toast.LENGTH_SHORT);
                return false;
            }
        }
    }

    private void setText(String text) {
        this.text = text;
    }

    private void setFarbeAlsString(String farbeAlsString) {
        this.farbeAlsString = farbeAlsString;
    }

}
