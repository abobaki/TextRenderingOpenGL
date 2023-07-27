// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 31.1.2023

package de.thkoeln.cvogt.android.opengl_utilities;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Class to define renderers. A renderer is attached to a surface view, i.e. an object of class <I>GLSurfaceViewCV</I>,
 * and calls the <I>draw()</I> methods of the shapes of this view.
 * <P>
 * A renderer also specifies and updates the View Projection Matrix to be applied to all shapes,
 *  i.e. the matrix that specifies the properties of the eye/camera looking at the collection of shapes
 *  and the projection of the shapes onto the 2D display.
 * <BR>
 * @see de.thkoeln.cvogt.android.opengl_utilities.GLSurfaceViewCV
 * @see de.thkoeln.cvogt.android.opengl_utilities.GLShapeCV
 */

public class GLRendererCV implements GLSurfaceView.Renderer {

    /**
     * The associated surface view on which the renderer will draw the shapes. The renderer will get the shapes from this view to call their draw() methods.
     */

    private GLSurfaceViewCV surfaceView;

    /** Initial value for the position of the eye/camera (x/y/z coordinates). */
    public static final float[] eyePosInit = { 0f, 0f, 5f };

    /** Initial value for the focus of the eye/camera (x/y/z coordinates). */
    public static final float[] eyeFocusInit = { 0f, 0f, 0f };

    /** Current position of the eye/camera (x/y/z coordinates). */
    private float[] eyePos = eyePosInit.clone();

    /** Current focus of the eye/camera (x/y/z coordinates). */
    private float[] eyeFocus = eyeFocusInit.clone();

    /** Frustum: near value. */
    private static final float frustumNear = 1;

    /** Frustum: far value. */
    private static final float frustumFar = 1000;

    /**
     * View matrix calculated from the eye/camera position (eyePos) and eye/camera focus (eyeFocus).
     * It initialized in onSurfaceChanged() and updated each time the eye/camera position or focus changes.
     */

    private final float[] viewMatrix = new float[16];

    /**
     * Projection matrix to project the 3D coordinates to the 2D display. Is set in onSurfaceChanged() based on the display geometry.
     */

    private final float[] projectionMatrix = new float[16];

    /**
     * View projection matrix to be passed to the draw methods of the shapes.
     * <BR>
     * The matrix is the product of the projection matrix and the view matrix,
     * the view matrix being calculated from the eye/camera position (eyePos) and eye/camera focus (eyeFocus).
     * It initialized in onSurfaceChanged() and updated each time the eye/camera position or focus changes.
     */

    private final float[] viewProjectionMatrix = new float[16];

    /**
     * A shape whose current position shall be used as the origin of the point light.
     * This shape itself will not be lighted by the point light but only by the directional and ambient light if these are defined.
     * If no directional and ambient lighting is defined, no lighting at all will be applied to the shape, i.e. it will be displayed as it is.
     */

    private GLShapeCV pointLightSource = null;

    /**
     * Position of the point light source (x,y,z coordinates).
     * Will only be used if the pointLightSource attribute is null.
     * If this attribute and the pointLightSource and the directionalLightVector attribute are null no lighting is applied.
     */

    private float[] pointLightPos = null;

    /**
     * Vector of the directional light (x,y,z components).
     * If this attribute and the pointLightSource and the pointLightPos attributes are null no lighting is applied.
     */

    private float[] directionalLightVector = null;

    /**
     * The contribution of the point light to the total of point light and directional light,
     * as a percentage given by a value between 0 and 1.
     * Only valid if lighting is applied, i.e. pointLightSource, pointLightPos and directionalLightVector are not all null.
     */

    private float relativePointLightShare = 0;

    /**
     * The additional contribution of the ambient light,
     * as a small non-negative value.
     * Only valid if lighting is applied, i.e. pointLightSource, pointLightPos and directionalLightVector are not all null.
     */

    private float ambientLight = 0;

    /**
     * @param surfaceView The surface view to which this renderer shall be attached.
     */

    public void setSurfaceView(GLSurfaceViewCV surfaceView) {
        this.surfaceView = surfaceView;
    }

    /**
     * Method called by the runtime system when the associated surface view has been initialized.
     * Colors the background black and compiles the OpenGL programs of the shapes to be displayed (i.e. the shapes attached to the associated surface view).
     * Prepares the textures for textured shapes.
     */

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        ArrayList<GLShapeCV> shapesToRender = surfaceView.getShapesToRender();
        for (GLShapeCV shape : shapesToRender) {
            shape.initOpenGLPrograms();
            shape.prepareTextures();
        }
    }

    /**
     * Method called by the runtime system when the geometry of the display changes.
     * Sets the projection matrix.
     */

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, +ratio, -1, 1, frustumNear, frustumFar);
        updateViewProjectionMatrix();
    }

    /**
     * Method called by the runtime system when the surface view shall been drawn, i.e. its shapes shall be rendered.
     */

    @Override
    synchronized public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);  // clear the buffers before drawing the shapes
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background color: black
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);  // such that fragments in the front ...
        GLES20.glDepthFunc(GLES20.GL_LESS);     // ... hide fragments in the back
        GLES20.glDepthMask( true );
        // draw the shapes based on the current view projection matrix
        ArrayList<GLShapeCV> shapesToRender = surfaceView.getShapesToRender();
        // start = (new Date()).getTime();
        //long start = System.nanoTime();
        for (GLShapeCV shape : shapesToRender) {
            if (!shape.isCompiled()) {
                shape.initOpenGLPrograms();
                shape.prepareTextures();
            }
            if (pointLightSource==null)
                shape.draw(viewProjectionMatrix,viewMatrix,pointLightPos,directionalLightVector,relativePointLightShare,ambientLight);
            else
            if (shape==pointLightSource)
                if (directionalLightVector!=null||ambientLight>0)
                    shape.draw(viewProjectionMatrix,viewMatrix,null,directionalLightVector,0,ambientLight);
                else
                    shape.draw(viewProjectionMatrix,viewMatrix,null,null,0,0);
            else
                shape.draw(viewProjectionMatrix,viewMatrix,pointLightSource.getTrans(),directionalLightVector,relativePointLightShare,ambientLight);

        }
        // duration = (new Date()).getTime() - start;
         //long duration = System.nanoTime() - start;
         //Log.v("GLDEMO",">>> Draw "+duration/1000000+" ms");
    }

    synchronized public void onDrawFrameBAK(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);  // clear the buffers before drawing the shapes
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background color: black
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);  // such that fragments in the front ...
        GLES20.glDepthFunc(GLES20.GL_LESS);     // ... hide fragments in the back
        GLES20.glDepthMask( true );
        // draw the shapes based on the current view projection matrix
        ArrayList<GLShapeCV> shapesToRender = surfaceView.getShapesToRender();
        // start = (new Date()).getTime();
        // long start = System.nanoTime();
        for (GLShapeCV shape : shapesToRender) {
            if (!shape.isCompiled()) {
                shape.initOpenGLPrograms();
                shape.prepareTextures();
            }
            shape.draw(viewProjectionMatrix,viewMatrix,pointLightPos,directionalLightVector,relativePointLightShare,ambientLight);
        }
        // duration = (new Date()).getTime() - start;
        // long duration = System.nanoTime() - start;
        // Log.v("GLDEMO",">>> Draw "+duration+" ns");
    }

    /**
     * Get a copy of the current projection matrix.
     */

    synchronized public float[] getProjectionMatrix() {
        return projectionMatrix.clone();
    }

    /**
     * Get a copy of the current projection matrix.
     */

    synchronized public float[] getViewMatrix() {
        return viewMatrix.clone();
    }

    /**
     * Get a copy of the current view projection matrix.
     */

    synchronized public float[] getViewProjectionMatrix() {
        return viewProjectionMatrix.clone();
    }

    /**
     * Calculates the current view matrix from the eye/camera position (eyePos) and eye/camera focus (eyeFocus).
     * Updates the view projection matrix as the product of the projection matrix and the view matrix.
     * Is called initially from onSurfaceChanged() and updated each time the eye/camera position or focus changes.
     */

    private void updateViewProjectionMatrix() {
        Matrix.setLookAtM(viewMatrix, 0,
                eyePos[0], eyePos[1], eyePos[2],
                eyeFocus[0], eyeFocus[1], eyeFocus[2],
                // vector specifying the "up direction" as seen from the eye/camera
                0f, 1f, 0f);
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        surfaceView.requestRender();
    }

    /**
     * Gets a copy of the current eye/camera position (eyePos attribute).
     */

    synchronized public float[] getEyePos() {
        return eyePos.clone();
    }

    /**
     * Gets the current eye/camera position value (eyePos attribute) in one of the three dimensions.
     * @param index 0 = x dimension, 1 = y dimension, 2 = z dimension, any other value = x dimension.
     * @return The value.
     */

    synchronized public float getEyePos(int index) {
        if (index<0||index>2) return eyePos[0];
        return eyePos[index];
    }

    /**
     * Gets a copy of the current eye/camera focus (eyeFocus attribute).
     */

    synchronized public float[] getEyeFocus() {
        return eyeFocus.clone();
    }

    /**
     * Gets the current eye/camera focus value (eyeFocus attribute) in one of the three dimensions.
     * @param index 0 = x dimension, 1 = y dimension, 2 = z dimension, any other value = x dimension.
     * @return The value.
     */

    synchronized public float getEyeFocus(int index) {
        if (index<0||index>2) return eyeFocus[0];
        return eyeFocus[index];
    }

    /**
     * Sets the eye/camera position value (eyePos attribute) in one of the three dimensions. Updates the view matrix accordingly.
     * @param index 0 = x dimension, 1 = y dimension, 2 = z dimension, any other value = x dimension.
     * @param value The value to be set.
     */

    synchronized public void setEyePos(int index, float value) {
        if (index<0||index>2) { eyePos[0] = value; return; }
        eyePos[index] = value;
        updateViewProjectionMatrix();
    }

    /**
     * Sets the eye/camera focus value (eyeFocus attribute) in one of the three dimensions. Updates the view matrix accordingly.
     * @param index 0 = x dimension, 1 = y dimension, 2 = z dimension, any other value = x dimension.
     * @param value The value to be set.
     */

    synchronized public void setEyeFocus(int index, float value) {
        if (index<0||index>2) { eyeFocus[0] = value; return; }
        eyeFocus[index] = value;
        updateViewProjectionMatrix();
    }

    /**
     * Sets the values of eye/camera position and focus (attributes eyePos and eyeFocus). Updates the view matrix accordingly.
     * @param eyePosX The new value for eyePos[0]
     * @param eyePosY The new value for eyePos[1]
     * @param eyePosZ The new value for eyePos[2]
     * @param eyeFocusX The new value for eyeFocus[0]
     * @param eyeFocusY The new value for eyeFocus[1]
     * @param eyeFocusZ The new value for eyeFocus[2]
     */

    synchronized public void setEyePosAndFocus(float eyePosX, float eyePosY, float eyePosZ, float eyeFocusX, float eyeFocusY, float eyeFocusZ) {
        setEyePosAndFocus(new float[] { eyePosX, eyePosY, eyePosZ }, new float[] { eyeFocusX, eyeFocusY, eyeFocusZ });
    }

    /**
     * Sets the values of eye/camera position and focus (attributes eyePos and eyeFocus). Updates the view matrix accordingly.
     * If one of the parameters is not valid, the attributes will remain unchanged.
     */

    synchronized public void setEyePosAndFocus(float[] eyePos, float[] eyeFocus) {
        if (eyePos==null||eyePos.length!=3||eyeFocus==null||eyeFocus.length!=3) return;
        this.eyePos = eyePos.clone();
        this.eyeFocus = eyeFocus.clone();
        updateViewProjectionMatrix();
    }

    /**
     * Resets the eye/camera position and focus (attributes eyePos and eyeFocus) to their initial values. Updates the view matrix accordingly.
     */

    synchronized public void resetEyePosAndFocus() {
        setEyePosAndFocus(eyePosInit, eyeFocusInit);
    }

    /**
     * Gets a popup window
     * through which the view matrix of this renderer (i.e. the eye/camera position (eyePos) and eye/camera focus (eyeFocus) can be controlled.
     */

    public PopupWindow getViewMatrixControlPopup(Context context) {
        return new ViewMatrixControlPopup(context);
    }

    /**
     * Class for popoup windows
     * through which the view matrix of this renderer (i.e. the eye/camera position (eyePos) and eye/camera focus (eyeFocus) can be controlled.
     */

    private class ViewMatrixControlPopup extends PopupWindow {

        LinearLayout layout;

        SeekBar seekBarEyeX, seekBarEyeY, seekBarEyeZ, seekBarCenterX, seekBarCenterY, seekBarCenterZ;

        ViewMatrixControlPopup(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = (LinearLayout) inflater.inflate(R.layout.layout_popup_viewmatrix, null, false);
            setContentView(layout);
            setFocusable(true);
            seekBarEyeX = layout.findViewById(R.id.seekBarEyeX);
            seekBarEyeX.setOnSeekBarChangeListener(new SeekbarsListener());
            seekBarEyeY = layout.findViewById(R.id.seekBarEyeY);
            seekBarEyeY.setOnSeekBarChangeListener(new SeekbarsListener());
            seekBarEyeZ = layout.findViewById(R.id.seekBarEyeZ);
            seekBarEyeZ.setOnSeekBarChangeListener(new SeekbarsListener());
            seekBarCenterX = layout.findViewById(R.id.seekBarCenterX);
            seekBarCenterX.setOnSeekBarChangeListener(new SeekbarsListener());
            seekBarCenterY = layout.findViewById(R.id.seekBarCenterY);
            seekBarCenterY.setOnSeekBarChangeListener(new SeekbarsListener());
            seekBarCenterZ = layout.findViewById(R.id.seekBarCenterZ);
            seekBarCenterZ.setOnSeekBarChangeListener(new SeekbarsListener());
        }

        // Listener für den Bestätigungsbutton im PopupWindow:
        // schließt das PopupWindow

        private class ButtonListener implements View.OnClickListener {
            public void onClick(View v) {
                dismiss();
            }
        }

        private class SeekbarsListener implements SeekBar.OnSeekBarChangeListener {
            private final float faktor = 0.5f;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (seekBar==seekBarEyeX)
                    eyePos[0] = eyePosInit[0] + ((i - 50)*faktor);
                if (seekBar==seekBarEyeY)
                    eyePos[1] = eyePosInit[1] + ((i - 50)*faktor);
                if (seekBar==seekBarEyeZ)
                    eyePos[2] = eyePosInit[2] + ((i - 50)*faktor);
                if (seekBar==seekBarCenterX)
                    eyeFocus[0] = eyeFocusInit[0] + ((i - 50)*faktor);
                if (seekBar==seekBarCenterY)
                    eyeFocus[1] = eyeFocusInit[1] + ((i - 50)*faktor);
                if (seekBar==seekBarCenterZ)
                    eyeFocus[2] = eyeFocusInit[2] + ((i - 50)*faktor);
                updateViewProjectionMatrix();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        }

    }

    /**
     * Sets the pointLightSource attribute
     * @param pointLightSource
     */

    synchronized public void setPointLightSource(GLShapeCV pointLightSource) {
        this.pointLightSource = pointLightSource;
    }

    /**
     * Sets the lighting specification (NB: does not work for shapes with lines and/or textures yet)
     * @param pointLightPos see pointLightPos attribute
     * @param directionalLightVector see directionalLightVector attribute
     * @param relativePointLightShare see relativePointLightShare attribute
     * @param ambientLight see ambientLight attribute
     */

    synchronized public void setLighting(float[] pointLightPos, float[] directionalLightVector, float relativePointLightShare, float ambientLight) {
        if (pointLightPos!=null)
            this.pointLightPos = pointLightPos.clone();
        else
            this.pointLightPos = null;
        if (directionalLightVector!=null)
            this.directionalLightVector = directionalLightVector.clone();
        else
            this.directionalLightVector = null;
        this.relativePointLightShare = relativePointLightShare;
        this.ambientLight = ambientLight;
    }

    /**
     * Switches off lighting by setting pointLightPos and directionalLightVector to null
     * and relativePointLightShare and ambientLight to zero.
     */

    synchronized public void switchOffLighting() {
        pointLightSource = null;
        pointLightPos = null;
        directionalLightVector = null;
        relativePointLightShare = 0;
        ambientLight = 0;
    }

}
