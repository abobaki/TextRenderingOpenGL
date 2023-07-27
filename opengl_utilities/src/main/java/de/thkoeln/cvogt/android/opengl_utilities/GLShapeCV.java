// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 5.12.2022

package de.thkoeln.cvogt.android.opengl_utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Class to define shapes, i.e. 2D or 3D objects, that can be rendered by a renderer of class <I>GLRendererCV</I> on a view of class <I>GLSurfaceViewCV</I>.
 * Such shapes are defined by a collection of triangles of class <I>GLTriangleCV</I> and/or a set of lines of class <I>GLLineCV</I>.
 * The shapes can be animated.
 * <P>
 * An object of this class typically includes these items:
 * <P>
 * <UL>
 * <LI>A collection of colored or textured triangles and/or a collection of colored lines between two endpoints each.
 * It is assumed that all triangles have the same coloring / texture type
 * (i.e. have all a uniform color [but possibly different colors for the different triangles], a gradient color or a texture from a bitmap file).
 * Note: The current version of this class displays lines only if the triangles are colored, i.e. not textured.
 * <P>
 * The vertex coordinates of these triangles and lines are specified with respect to the "model coordinate system" ("local coordinate system") of the shape they belong to.
 * The center (0,0,0) of the model coordinate system is typically the geometric center of the 2D or 3D object defined by the shape,
 * e.g. the center of a circle or a cube.
 * <P><LI>A model matrix specifying the translation, rotation, and scaling operations that map the model coordinates to world coordinates,
 * i.e. that place the shape into the "real world".
 * <P><LI>An OpenGL program with the vertex shaders and fragment shaders to be executed by the graphics hardware.
 * <P><LI>A <I>draw()</I> method to be called by a renderer to display the shape.
 * <P><LI>Optionally animators and an additional control thread to define how the object shall be animated.
 * </UL>
 *  @see GLSurfaceViewCV
 *  @see GLRendererCV
 *  @see GLTriangleCV
 *  @see GLLineCV
 *  @see GLShapeFactoryCV
 *  @see GLAnimatorFactoryCV
 *  @see GLSceneFactoryCV
 *  @see TextureBitmapsCV
 */

public class GLShapeCV {

    /** The ID of the shape. */

    private String id;

    /** The surface view to which the shape is currently attached. */

    private GLSurfaceViewCV surfaceView;

    /**
     * Coloring / texture type of the shape (values according to GLPlatformCV, constants COLORING_XXX).
     * This value will be derived from the triangles. It is currently assumed that all triangles have the same coloring / texture type.
     */

    private int coloringType;

    /** The triangles building this shape. The vertex coordinates of these triangles are specified with respect to the "model coordinate system" ("local coordinate system") of this shape.
     * <BR>
     * This attribute can be null. If so, the 'lines' attribute must not be null and the shape will consist of lines only. */

    private GLTriangleCV[] triangles;

    /**
     * Bitmaps specifying the triangle textures if the triangles are textured.
     * The array is initialized by the constructor from the texture bitmaps of the triangles.
     * Only valid if the triangles are textured, i.e. not colored.
     */

    private Bitmap[] textureBitmaps;

    /** IDs of the textures. Only valid if the triangles are textured, i.e. not colored. */

    private int[] textureNames;

    /**
     * Bitmaps specifying the uv coordinates for the triangle textures if the triangles a textured.
     * The array is initialized by the constructor from the uv coordinates of the triangles.
     * Only valid if the triangles are textured, i.e. not colored.
     */

    private float[] uvCoordinates;

    /** The lines belonging to this shape. The vertex coordinates of these lines are specified with respect to the "model coordinate system" ("local coordinate system") of this shape.
     * <BR>
     * This attribute can be null. If so, the 'triangles' attribute must not be null and the shape will consist of triangles only.
     * <BR>
     * Note: The current version of this class displays lines only if the triangles are colored, i.e. not textured.
     */

    private GLLineCV[] lines;

    /**
     * The width of the lines.
     * Only valid if the shape has lines, i.e. the 'lines' attribute is not null.
     * Initialized by the constructor.
     */

    private float lineWidth;

    /**
     * The intrinsic size of the shape in the three dimensions (0 = x, 1 = y, 2 = z),
     * i.e the maximum difference between the coordinates of any two triangle or line vertices in the x, y, or z dimension.
     * i.e. the extension of the enclosing cube in the selected dimension.
     * The value refers to the local coordinate system (model coordinate system),
     * i.e. disregards the transformations specified by the model matrix.
     */

    private float[] intrinsicSize;

    /**
     * The model matrix specifying the translation, rotation, and scaling operations that map the model coordinates of the triangle vertices to world coordinates.
     * It thus "places the shape into the real world".
     * The model matrix is automatically calculated from the scaling matrix (attribute scalingMatrix),
     * the rotation matrix (attribute rotationMatrix), and the translation matrix (attribute translationMatrix),
     */

    private final float[] modelMatrix;

    /**
     * The scaling matrix (a float array of length 16, as required by OpenGL).
     * The scaling factor for the x dimension is stored at position 0,
     * for the y dimension at position 5 and for the z dimension at position 10.
     * The factors can be written and read through the setScale and getScale methods.
     * Its initial value is the identity matrix.
     */

    private final float[] scalingMatrix;

    /**
     * The rotation matrix (a float array of length 16, as required by OpenGL).
     * Its initial value is the identity matrix.
     */

    private float[] rotationMatrix;

    /**
     * The translation matrix (a float array of length 16, as required by OpenGL).
     * The translation value for the x dimension is stored at position 12,
     * for the y dimension at position 13 and for the z dimension at position 14.
     * The values can be written and read through the setTrans and getTrans methods.
     * Its initial value is the identity matrix.
     */

    private final float[] translationMatrix;

    /** A temporary list of animators that shall be started in a later call of the startAnimators() method.
     *  After starting these animators, startAnimators() will clear this 'animators' attribute.
     *  <BR>
     *  The animators are based on the Android "property animation technique" and will therefore call setXXX() methods of the shape.
     *  The general idea is that these animators affect the model matrix of the shape, i.e. the placement of the complete shape in the world by scaling, rotating, and translating it.
     *  Animation of selected coordinate values in the shape's model coordinate space (i.e. morphing operations on the shape) will be done by a control thread attached to the shape.
     *  @see GLAnimatorFactoryCV
     *  @see ControlThread
     */

    private ArrayList<Animator> animators;

    /** The control thread of the shape.
     *  The general idea is that this thread modifies selected vertex coordinates and color values and thus "morphs" the shape in its model coordinate space.
     *  Animation operations that affect the model matrix of the shape, i.e. the placement of the complete shape in the world, will be controlled by animators as specified by the 'animators' attribute.
     */

    private ControlThread controlThread;

    /** The IDs of the OpenGL ES program to draw this shape (without lighting). */

    private int openGLprogramWithoutLighting;

    /** The IDs of the OpenGL ES program to draw this shape (with lighting). */

    private int openGLprogramWithLighting;

    /**
     * Information whether the OpenGL programs have been compiled.
     * If not, the renderer will compile the program in its onDrawFrame() method,
     * i.e. call the initOpenGLPrograms() method of this shape.
     */

    private boolean isCompiled;

    /** The Open GL ES vertex shader code (without lighting). */

    private String vertexShaderCodeWithoutLighting;

    /** The Open GL ES vertex shader code (with lighting). */

    private String vertexShaderCodeLighting;

    /** The Open GL ES fragment shader codes. */

    private String fragmentShaderCode;

    /**
     * Buffer to pass the vertex coordinates of the triangles to the graphics hardware.
     * Only valid if the shape has triangles, i.e. the 'triangles' attribute is not null.
     */

    private FloatBuffer triangleVerticesBuffer;

    /**
     * Buffer to pass the surface normals of the triangles to the graphics hardware
     * that are needed to calculate the light reflected by the shape's surfaces.
     * Only valid if the shape has triangles, i.e. the 'triangles' attribute is not null.
     */

    private FloatBuffer triangleNormalsBuffer;

    /**
     * Buffer to pass the end vertex coordinates of the lines to the graphics hardware.
     * Only valid if the shape has lines, i.e. the 'lines' attribute is not null.
     */

    private FloatBuffer lineVerticesBuffer;

    /**
     * Buffer to pass the color values of the triangles to the graphics hardware.
     * The buffer is initialized by the method setModelMatrixAndBuffers() from the color values of the triangles
     * and used by the draw() method.
     * Only valid if the shape has triangles, i.e. the 'triangles' attribute is not null.
     */

    private FloatBuffer triangleColorsBuffer;

    /**
     * Buffer to pass the uv coordinates to the graphics hardware.
     * Only valid if the shape has textured triangles.
     */

    private FloatBuffer uvBuffer;

    /**
     * Buffer to pass the color values of the lines to the graphics hardware.
     * The buffer is initialized by the method setModelMatrixAndBuffers() from the color values of the triangles
     * and used by the draw() method.
     * Only valid if the shape has lines, i.e. the 'lines' attribute is not null.
     */

    private FloatBuffer lineColorsBuffer;

    /**
     * The constructor will prepare the OpenGL code to be executed for this shape with the corresponding attribute values ('vertexBuffer' etc.).
     * The OpenGL code is not compiled by the constructor (which would not work that early)
     * but by a later separate call of initOpenGLPrograms() from the onSurfaceCreated() method of a corresponding renderer.
     * The constructor will also prepare the model matrix from the corresponding scaling, translation, and rotation matrix attributes.
     * @param id The ID of the shape.
     * @param triangles The triangles for the shape. Clones of these triangles will be assigned to the 'triangles' attribute.
     */

    public GLShapeCV(String id, GLTriangleCV[] triangles) {
        this(id,triangles,null,0);
    }

    /**
     * The constructor will prepare the OpenGL code to be executed for this shape with the corresponding attribute values ('vertexBuffer' etc.).
     * The OpenGL code is not compiled by the constructor (which would not work that early)
     * but by a later separate call of initOpenGLPrograms() from the onSurfaceCreated() method of a corresponding renderer.
     * The constructor will also prepare the model matrix from the corresponding scaling, translation, and rotation matrix attributes.
     * @param id The ID of the shape.
     * @param lines The lines for the shape. A clone of this array will be assigned to the 'lines' attribute. May be null if the 'triangles' parameter is not null.
     * @param lineWidth The width of the lines of the shape.
     */

    public GLShapeCV(String id, GLLineCV[] lines, float lineWidth) {
        this(id,null,lines,lineWidth);
    }

    /**
     * The constructor will prepare the OpenGL code to be executed for this shape with the corresponding attribute values ('vertexBuffer' etc.).
     * The OpenGL code is not compiled by the constructor (which would not work that early)
     * but by a later separate call of initOpenGLPrograms() from the onSurfaceCreated() method of a corresponding renderer.
     * The constructor will also prepare the model matrix from the corresponding scaling, translation, and rotation matrix attributes.
     * @param id The ID of the shape.
     * @param triangles The triangles for the shape. Clones of these triangles will be assigned to the 'triangles' attribute. May be null if the 'lines' parameter is not null.
     * @param lines The lines for the shape. A clone of this array will be assigned to the 'lines' attribute. May be null if the 'triangles' parameter is not null.
     * @param lineWidth The width of the lines of the shape.
     */

    public GLShapeCV(String id, GLTriangleCV[] triangles, GLLineCV[] lines, float lineWidth) {

        this.id = id;

        // prepare the matrices, the rotation axis and the rotation angle

        modelMatrix = new float[16];
        scalingMatrix = new float[16];
        rotationMatrix = new float[16];
        translationMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix,0);
        Matrix.setIdentityM(scalingMatrix,0);
        Matrix.setIdentityM(rotationMatrix,0);
        Matrix.setIdentityM(translationMatrix,0);

        // set the triangles building this shape

        if (triangles!=null) {
            this.triangles = new GLTriangleCV[triangles.length];
            for (int i = 0; i < triangles.length; i++)
                if (triangles[i] != null)
                    this.triangles[i] = triangles[i].clone();
        }
        colorArrayOfTriangles = null;

        // set the lines building this shape

        if (lines!=null) {
            this.lines = new GLLineCV[lines.length];
            for (int i = 0; i < lines.length; i++)
                if (lines[i] != null)
                    this.lines[i] = lines[i].clone();
        }

        this.lineWidth = lineWidth;

        // prepare the list of animators

        animators = new ArrayList<Animator>();

        controlThread = null;

        // set the model matrix from the scaling, translation, and rotation attributes
        // and the buffers from which the vertex coordinates, color values, and surface normals will be transferred to the hardware

        setModelMatrixAndBuffers();

        // calculate and store the intrinsic sizes in the three dimensions

        intrinsicSize = new float[3];
        intrinsicSize[0] = calculateIntrinsicSize(0);
        intrinsicSize[1] = calculateIntrinsicSize(1);
        intrinsicSize[2] = calculateIntrinsicSize(2);

    }

    /*
    private float[] makeNormalsArray(float[] coordinates) {
        float[] normals = new float[coordinates.length];
        // Schleife zum Durchlaufen aller Dreiecke.
        // Der Laufindex i ist jeweils der Begginn einer Neunergruppe von Koordinaten (siehe Kommentar oben)
        for (int i=0; i<coordinates.length; i+=9) {
            // Berechnung zweier Vektoren, die zwei Dreiecksseiten entsprechen
            float[] vec1 = new float[3];
            float[] vec2 = new float[3];
            for (int j=0; j<3; j++) {
                vec1[j] = coordinates[i+3+j]-coordinates[i+j];
                vec2[j] = coordinates[i+6+j]-coordinates[i+j];
            }
            // Berechnung des entsprechenden Normalenvektors
            // = Vektors, der auf vec1 und vec2 und somit auf der Dreiecksfläche senkrecht steht
            float[] normal = kreuzprodukt(vec1,vec2);
            // Zuweisung des Normalenvektors an den Rückgabearray (dreimal hintereinander für die drei Ecken des Dreiecks)
            for (int j=0; j<3; j++)
                normals[i+j] = normals[i+3+j] = normals[i+6+j] = normal[j];
        }
        return normals;
    }
    */


    /**
     * Makes a deep copy of this shape, i.e. makes a new shape with copies of all the triangles and lines.
     * @param id The id of the new shape.
     * @return A reference to the new shape.
     */

    synchronized public GLShapeCV copy(String id) {
        return new GLShapeCV(id,triangles,lines,lineWidth);
    }

    /**
     * Internal auxiliary method to set the model matrix and the buffers based on the triangles and lines of this shape.
     * Must be called when the set of triangles or lines is initialized or modified.
     */

    synchronized public void setModelMatrixAndBuffers() {

        // build the ModelMatrix 'modelMatrix' from the scaling, translation, and rotation matrix attributes.

        buildModelMatrix();

        // prepare the buffers with the vertex coordinates

        final int BYTES_PER_FLOAT = 4;

        if (triangles!=null) {
            float[] triangleCoordinates = coordinateArrayFromTriangles();
            ByteBuffer bbTri = ByteBuffer.allocateDirect(triangleCoordinates.length * BYTES_PER_FLOAT);
            bbTri.order(ByteOrder.nativeOrder());
            triangleVerticesBuffer = bbTri.asFloatBuffer();
            // long start = System.nanoTime();
            triangleVerticesBuffer.put(triangleCoordinates);
            // Log.v("GLDEMO",">>> put "+id+" ("+getNumberOfTriangles() +" triangles): "+(System.nanoTime()-start));
            triangleVerticesBuffer.position(0);
        }

        if (lines!=null) {
            float[] linesCoordinates = coordinateArrayFromLines();
            ByteBuffer bbLin = ByteBuffer.allocateDirect(linesCoordinates.length * BYTES_PER_FLOAT);
            bbLin.order(ByteOrder.nativeOrder());
            lineVerticesBuffer = bbLin.asFloatBuffer();
            lineVerticesBuffer.put(linesCoordinates);
            lineVerticesBuffer.position(0);
        }

        // prepare the buffer with the triangle normals

        if (triangles!=null) {
            float[] normals = normalsArrayFromTriangles();
            ByteBuffer bbNorm = ByteBuffer.allocateDirect(normals.length * BYTES_PER_FLOAT);
            bbNorm.order(ByteOrder.nativeOrder());
            triangleNormalsBuffer = bbNorm.asFloatBuffer();
            // long start = System.nanoTime();
            triangleNormalsBuffer.put(normals);
            // Log.v("GLDEMO",">>> put "+id+" ("+getNumberOfTriangles() +" triangles): "+(System.nanoTime()-start));
            triangleNormalsBuffer.position(0);

        }

        // determine the coloring type:
        // - if there exist triangles: the coloring type of the first triangle (assuming that all triangles have the same coloring type)
        // - if there exist lines but no triangles: COLORING_UNIFORM
        // (note that the current version of this class draws no lines if there exist textured triangles.)

        if (triangles!=null)
            coloringType = triangles[0].getColoringType();
        else
            coloringType = GLPlatformCV.COLORING_UNIFORM;

        if (triangles!=null) {
            // set colors or textures for the triangles
            switch (coloringType) {
                case GLPlatformCV.COLORING_UNIFORM:
                    // TODO: Hier auf Basis des Codes von GLPlatformCV.vertexShaderUniform programmieren, sobald dieser funktioniert
                case GLPlatformCV.COLORING_VARYING:
                    float[] triangleColors = colorArrayFromTriangles();
                    ByteBuffer bbColTriangles = ByteBuffer.allocateDirect(triangleColors.length * BYTES_PER_FLOAT);
                    bbColTriangles.order(ByteOrder.nativeOrder());  // native byte order of the device
                    triangleColorsBuffer = bbColTriangles.asFloatBuffer();
                    triangleColorsBuffer.put(triangleColors);
                    triangleColorsBuffer.position(0);  // set read index to the first buffer element
                    break;
                case GLPlatformCV.COLORING_TEXTURED:
                    int uvIndex = 0;
                    uvCoordinates = new float[triangles.length * 6];
                    for (GLTriangleCV triangle : triangles) {
                        float[] uvCoordinatesTriangle = triangle.getUvCoordinates();
                        for (int i = 0; i < 6; i++)
                            uvCoordinates[uvIndex++] = uvCoordinatesTriangle[i];
                    }
                    ByteBuffer bbUV = ByteBuffer.allocateDirect(uvCoordinates.length * BYTES_PER_FLOAT);
                    bbUV.order(ByteOrder.nativeOrder());
                    uvBuffer = bbUV.asFloatBuffer();
                    uvBuffer.put(uvCoordinates);
                    uvBuffer.position(0);
                    textureBitmaps = new Bitmap[triangles.length];
                    for (int i = 0; i < triangles.length; i++)
                        textureBitmaps[i] = triangles[i].getTexture();
                    textureNames = new int[textureBitmaps.length];
                    break;
            }

        }

        if (lines!=null&&coloringType!=GLPlatformCV.COLORING_TEXTURED) {
            // set colors for the lines
            float[] lineColors = colorArrayFromLines();
            ByteBuffer bbColLines = ByteBuffer.allocateDirect(lineColors.length * BYTES_PER_FLOAT);
            bbColLines.order(ByteOrder.nativeOrder());
            lineColorsBuffer = bbColLines.asFloatBuffer();
            lineColorsBuffer.put(lineColors);
            // for (int i=0; i<lineColors.length/4; i++)
            //     Log.v("DEMO","--------- "+lineColors[i*4]+" "+lineColors[i*4+1]+" "+lineColors[i*4+2]+" "+lineColors[i*4+3]+" ");
            lineColorsBuffer.position(0);
        }

        // long duration = System.nanoTime() - start;
        // Log.v("GLDEMO",">>> Put buffers: "+duration+" ns");
        // Log.v("GLDEMO",">>> Put Buffers: "+duration/1000000+" ms");

    }

    /**
     * Internal auxiliary method to build the model matrix (i.e. the 'modelMatrix' attribute) from the scaling, rotation, and translation matrix attributes of the shape.
     * For details, see the note in the introductory text on the order of transformation operations.
     */

    synchronized private void buildModelMatrix() {
        Matrix.setIdentityM(modelMatrix,0);
        Matrix.multiplyMM(modelMatrix, 0, scalingMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, translationMatrix, 0, modelMatrix, 0);
    }

    /**
     * To set the shaders and to compile and link the OpenGL program from these shader coders.
     * This method will be called from the onSurfaceCreated() method of the renderer that shall render the shade.
     * An earlier call (esp. from the shape constructor) will lead to an OpenGL link and/or compile error.
     * For textured shapes, always to be called together with initOpenGLPrograms().
     */

    synchronized public void initOpenGLPrograms() {

        switch (coloringType) {
            case GLPlatformCV.COLORING_UNIFORM:
                // TODO: Hier Code von GLPlatformCV.vertexShaderUniformColor zuweisen, sobald er funktioniert
                // vertexShaderCode = GLPlatformCV.vertexShaderUniformColor;
                // fragmentShaderCode = GLPlatformCV.fragmentShaderUniformColor;
                // break;
            case GLPlatformCV.COLORING_VARYING:
                vertexShaderCodeWithoutLighting = GLPlatformCV.vertexShaderVaryingColor;
                vertexShaderCodeLighting = GLPlatformCV.vertexShaderVaryingColorLighting;
                fragmentShaderCode = GLPlatformCV.fragmentShaderVaryingColor;
                break;
            case GLPlatformCV.COLORING_TEXTURED:
                vertexShaderCodeWithoutLighting = GLPlatformCV.vertexShaderTextured;
                vertexShaderCodeLighting = null;
                fragmentShaderCode = GLPlatformCV.fragmentShaderTextured;
                break;
            default:
                return;
        }

        // create OpenGL shaders

        int vertexShader = GLPlatformCV.loadShader(GLES20.GL_VERTEX_SHADER,this.vertexShaderCodeWithoutLighting);
        int vertexShaderLighting;
        if (this.vertexShaderCodeLighting!=null)
            vertexShaderLighting = GLPlatformCV.loadShader(GLES20.GL_VERTEX_SHADER,this.vertexShaderCodeLighting);
         else vertexShaderLighting = -1;
        int fragmentShader = GLPlatformCV.loadShader(GLES20.GL_FRAGMENT_SHADER,this.fragmentShaderCode);

        // create the programs

        openGLprogramWithoutLighting = GLES20.glCreateProgram();
        GLES20.glAttachShader(openGLprogramWithoutLighting, vertexShader);
        GLES20.glAttachShader(openGLprogramWithoutLighting, fragmentShader);
        GLES20.glLinkProgram(openGLprogramWithoutLighting);

        // debug information to see if the OpenGL code has been linked successfully
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(openGLprogramWithoutLighting, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0]==1) {
            // Log.v("GLDEMO", ">>> Linking successful");
            isCompiled = true;
        }
        else {
            // Log.v("GLDEMO2", ">>> Linking error: " + GLES20.glGetProgramInfoLog(openGLprogram));
        }

        if (vertexShaderLighting==-1) {
            openGLprogramWithLighting = -1;
            return;
        }

        openGLprogramWithLighting = GLES20.glCreateProgram();
        GLES20.glAttachShader(openGLprogramWithLighting, vertexShaderLighting);
        GLES20.glAttachShader(openGLprogramWithLighting, fragmentShader);
        GLES20.glLinkProgram(openGLprogramWithLighting);

        // debug information to see if the OpenGL code has been linked successfully
        GLES20.glGetProgramiv(openGLprogramWithLighting, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0]!=1)
            isCompiled = false;

    }

    /**
     * To prepare the textures (only for textured shapes). Always to be called together with initOpenGLPrograms().
     */

    synchronized public void prepareTextures() {

        if (coloringType==GLPlatformCV.COLORING_TEXTURED) {
            GLES20.glGenTextures(textureNames.length, textureNames, 0);
            for (int i = 0; i < textureBitmaps.length; i++) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[i]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmaps[i], 0);
            }
        }

    }

    /**
     * The method to be called by a renderer to draw the shape without lighting.
     * The view/projection matrix passed by the renderer is multiplied with the model matrix of the shape.
     * @param vpMatrix The view/projection matrix to be passed by the renderer.
     */

    synchronized public void draw(float[] vpMatrix) {    // Gesamtdauer für einen Würfel mit Kantenlinien: ca. 40-60 Mikrosek. (Zeitmessung 8.6.22)

        draw(vpMatrix,null,null,null,0,0);

        /*
        // Log.v("GLDEMO",">>>>> draw "+id);

        // set some fundamental constants

        final int COORDS_PER_VERTEX = 3;  // coordinates (3 = three-dimensional space)
        final int COLORS_PER_VERTEX = 4;  // number of color values per vertex (4 = RGBA)
        final int BYTES_PER_FLOAT = 4;
        final int triangleVertexCount = triangles!=null?triangles.length*3:0;    // total number of triangle vertices
        final int lineVertexCount = lines!=null?lines.length*2:0;    // total number of lines vertices

        // use the program defined in the constructor

        GLES20.glUseProgram(openGLprogramWithoutLighting);    // ca. 2 Mikrosek. (Zeitmessung 8.6.22)

        // calculate the MVP matrix from the model matrix of the shape and the view/projection matrix from the renderes

        // float[] mvpMatrix = new float[16];

        float[] mvpMatrix = new float[16];     // diese und die nächste Zeile: ca. 2 Mikrosek. (Zeitmessung 8.6.22)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // pass the MVP matrix to the program        // die nächsten 4 Operationen: ca. 4-5 Mikrosek. (Zeitmessung 8.6.22)

        int mMVPMatrixHandle = GLES20.glGetUniformLocation(openGLprogramWithoutLighting, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // get and activate a handle for the aPosition attribute of the vertex shader (coordinates of the vertices)

        int positionHandle = GLES20.glGetAttribLocation(openGLprogramWithoutLighting, "aPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);

        // draw the triangles

        // long start = System.nanoTime();

        if (triangles!=null&&triangles.length>0) {     // Zeichnen der 12 Dreiecke eines Würfels: ca. 8-10 Mikrosek. (Zeitmessung 8.6.22)
            // zum Vergleich: Zeichen von 96000 Dreiecken: ca. 2 Millisek.
            // connect the 'vertexBuffer' attribute containing the triangle vertex coordinates with the aPosition attribute
            // = pass the triangle coordinates to the graphics hardware

            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    0, triangleVerticesBuffer);

            switch (coloringType) {

                case GLPlatformCV.COLORING_UNIFORM:
                    // TODO: Hier auf Basis des Codes von GLPlatformCV.vertexShaderUniform programmieren, sobald dieser funktioniert
                    // Dann auch den Fall berücksichtigen, dass alle Triangles jeweils eine einheitliche Farbe haben, diese Farben aber unterschiedlich sind
                    // colorHandle = GLES20.glGetUniformLocation(openGLprogram, "vColor");  // für einfarbige Würfel
                    // GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
                    // break;
                case GLPlatformCV.COLORING_VARYING:
                    int colorHandle = GLES20.glGetAttribLocation(openGLprogramWithoutLighting, "aColor");
                    GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, COLORS_PER_VERTEX*BYTES_PER_FLOAT, triangleColorsBuffer);
                    GLES20.glEnableVertexAttribArray(colorHandle);
                    // draw the shape
                    // long start = System.nanoTime();
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleVertexCount);
                    // long duration = System.nanoTime() - start;
                    // Log.v("GLDEMO",">>> "+triangles.length+" triangles "+(duration/1000)+" microsec");
                    // deactivate the attribute arrays
                    GLES20.glDisableVertexAttribArray(positionHandle);
                    GLES20.glDisableVertexAttribArray(colorHandle);
                    break;
                case GLPlatformCV.COLORING_TEXTURED:
                    int textureHandle = GLES20.glGetAttribLocation(openGLprogramWithoutLighting, "aTexCoord");
                    // buffer for the uv coordinates
                    ByteBuffer bbUV = ByteBuffer.allocateDirect(uvCoordinates.length * BYTES_PER_FLOAT);
                    bbUV.order(ByteOrder.nativeOrder());
                    uvBuffer = bbUV.asFloatBuffer();
                    uvBuffer.put(uvCoordinates);
                    uvBuffer.position(0);
                    GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 2*BYTES_PER_FLOAT, uvBuffer);
                    GLES20.glEnableVertexAttribArray(textureHandle);
                    for (int i = 0; i < triangles.length; i++) {   // draw the triangles one by one, setting the texture anew for each individual triangle
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[i]);
                        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 3 * i, 3);
                    }
                    // disable the vertex array
                    GLES20.glDisableVertexAttribArray(positionHandle);
                    GLES20.glDisableVertexAttribArray(textureHandle);
                    break;
            }

        }

        // long duration = System.nanoTime() - start;
        // Log.v("GLDEMO",">>> "+triangles.length+" triangles "+duration+" ns");

        // draw the lines

        // if (coloringType==GLPlatformCV.COLORING_TEXTURED) return;  // Current version of this class: Lines only for colored triangles.

        if (lines!=null) {        // Zeichnen der Kantenlinien eines Würfels: ca. 7-10 Mikrosek. (Zeitmessung 8.6.22)
            // positionHandle = GLES20.glGetAttribLocation(openGLprogram, "aPosition");
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    0, lineVerticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            int colorHandle = GLES20.glGetAttribLocation(openGLprogramWithoutLighting, "aColor");
            GLES20.glDisableVertexAttribArray(colorHandle);
            GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, COLORS_PER_VERTEX*BYTES_PER_FLOAT, lineColorsBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            GLES20.glLineWidth(lineWidth);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertexCount);
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
        }

        */
    }

    /**
     * The method to be called by a renderer to draw the shape, optionally with lighting.
     * (NB: lighting does not work for lines and for textured shapes yet)
     * Lighting is only applied if at least one of the parameters pointLightPos and directionalLightVector is not null.
     * @param vpMatrix The view/projection matrix to be passed by the renderer. It will be multiplied with the model matrix of the shape.
     * @param vMatrix The view matrix to be passed by the renderer (only required if lighting is applied, may be null otherwise).
     * @param pointLightPos The position of the point light source (array of length 3 with the x, y, and z coordinates of the position). May be null if there shall be no point light.
     * @param directionalLightVector The position of the point light source (array of length 3 with the x, y, and z coordinates of the position). May be null if there shall be no directional light.
     * @param relativePointLightShare The relative point light share of the total amount of point light and directional light (as a value between 0.0 and 1.0; only valid if lighting is applied)
     * @param ambientLight The additional contribution of the ambient light (as a small value greater tham 0.0; 0 if there shall be no ambient light; only valid if lighting is applied)
     */

    synchronized public void draw(float[] vpMatrix, float[] vMatrix, float[] pointLightPos, float[] directionalLightVector, float relativePointLightShare, float ambientLight) {

        boolean withLighting = (pointLightPos!=null)||(directionalLightVector!=null);
        
        int openGLprogram;
        
        if (withLighting)
            openGLprogram = this.openGLprogramWithLighting;
          else
            openGLprogram = this.openGLprogramWithoutLighting;  

        if (withLighting) {
            if (pointLightPos==null) {
                pointLightPos = new float[3];
                relativePointLightShare = 0;
            }
            if (directionalLightVector==null) {
                directionalLightVector = new float[3];
                relativePointLightShare = 1;
            }
        }

        GLES20.glUseProgram(openGLprogram);
        
        // set some fundamental constants

        final int COORDS_PER_VERTEX = 3;  // coordinates (3 = three-dimensional space)
        final int COLORS_PER_VERTEX = 4;  // number of color values per vertex (4 = RGBA)
        final int BYTES_PER_FLOAT = 4;
        final int triangleVertexCount = triangles!=null?triangles.length*3:0;    // total number of triangle vertices
        final int lineVertexCount = lines!=null?lines.length*2:0;    // total number of lines vertices

        // calculate the MVP matrix and the MV matrix from the model matrix of the shape and the view/projection and view matrices from the renderer

        float[] mvpMatrix = new float[16];
        float[] mvMatrix = new float[16];

        Matrix.multiplyMM(mvMatrix, 0, vMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // pass the matrices to the program

        int mvpMatrixHandle = GLES20.glGetUniformLocation(openGLprogram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        int vMatrixHandle = GLES20.glGetUniformLocation(openGLprogram, "uVMatrix");
        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, vMatrix, 0);

        int mvMatrixHandle = GLES20.glGetUniformLocation(openGLprogram, "uMVMatrix");
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix, 0);

        if (withLighting) {   // pass lighting-related values to the hardware

            if (triangles!=null) {
                int normalHandle = GLES20.glGetAttribLocation(openGLprogram, "aNormal");
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, triangleNormalsBuffer);
                GLES20.glEnableVertexAttribArray(normalHandle);
            }

            int pointLightPosHandle = GLES20.glGetUniformLocation(openGLprogram, "uPointLightPos");
            GLES20.glUniform3f(pointLightPosHandle, pointLightPos[0], pointLightPos[1], pointLightPos[2]);

            int directionalLightVectorHandle = GLES20.glGetUniformLocation(openGLprogram, "uDirectionalLightVector");
            GLES20.glUniform3f(directionalLightVectorHandle, -directionalLightVector[0], -directionalLightVector[1], -directionalLightVector[2]);

            int relativePointLightShareHandle = GLES20.glGetUniformLocation(openGLprogram, "uRelativePointLightShare");
            GLES20.glUniform1f(relativePointLightShareHandle, relativePointLightShare);

            int ambientLightHandle = GLES20.glGetUniformLocation(openGLprogram, "uAmbientLight");
            GLES20.glUniform1f(ambientLightHandle, ambientLight);

        }

        int positionHandle = GLES20.glGetAttribLocation(openGLprogram, "aPosition");

        // draw the triangles

        if (triangles!=null&&triangles.length>0) {     // Zeichnen der 12 Dreiecke eines Würfels: ca. 8-10 Mikrosek. (Zeitmessung 8.6.22)
            // zum Vergleich: Zeichen von 96000 Dreiecken: ca. 2 Millisek.
            // connect the 'vertexBuffer' attribute containing the triangle vertex coordinates with the aPosition attribute
            // = pass the triangle coordinates to the graphics hardware

            // pass the vertex coordinates to the hardware

            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,0, triangleVerticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);

            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    0, triangleVerticesBuffer);

            switch (coloringType) {

                case GLPlatformCV.COLORING_UNIFORM:
                    // TODO: Hier auf Basis des Codes von GLPlatformCV.vertexShaderUniform programmieren, sobald dieser funktioniert
                    // Dann auch den Fall berücksichtigen, dass alle Triangles jeweils eine einheitliche Farbe haben, diese Farben aber unterschiedlich sind
                    // colorHandle = GLES20.glGetUniformLocation(openGLprogram, "vColor");  // für einfarbige Würfel
                    // GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
                    // break;
                case GLPlatformCV.COLORING_VARYING:
                    int colorHandle = GLES20.glGetAttribLocation(openGLprogram, "aColor");
                    GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, COLORS_PER_VERTEX*BYTES_PER_FLOAT, triangleColorsBuffer);
                    GLES20.glEnableVertexAttribArray(colorHandle);
                    // draw the shape
                    // long start = System.nanoTime();
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleVertexCount);
                    // long duration = System.nanoTime() - start;
                    // Log.v("GLDEMO",">>> "+triangles.length+" triangles "+(duration/1000)+" microsec");
                    // deactivate the attribute arrays
                    GLES20.glDisableVertexAttribArray(positionHandle);
                    GLES20.glDisableVertexAttribArray(colorHandle);
                    break;
                case GLPlatformCV.COLORING_TEXTURED:
                    int textureHandle = GLES20.glGetAttribLocation(openGLprogram, "aTexCoord");
                    // buffer for the uv coordinates
                    ByteBuffer bbUV = ByteBuffer.allocateDirect(uvCoordinates.length * BYTES_PER_FLOAT);
                    bbUV.order(ByteOrder.nativeOrder());
                    uvBuffer = bbUV.asFloatBuffer();
                    uvBuffer.put(uvCoordinates);
                    uvBuffer.position(0);
                    GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 2*BYTES_PER_FLOAT, uvBuffer);
                    GLES20.glEnableVertexAttribArray(textureHandle);
                    for (int i = 0; i < triangles.length; i++) {   // draw the triangles one by one, setting the texture anew for each individual triangle
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[i]);
                        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 3 * i, 3);
                    }
                    // disable the vertex array
                    GLES20.glDisableVertexAttribArray(positionHandle);
                    GLES20.glDisableVertexAttribArray(textureHandle);
                    break;
            }

        }

        // draw the lines

        // if (coloringType==GLPlatformCV.COLORING_TEXTURED) return;  // Current version of this class: Lines only for colored triangles.

        if (lines!=null) {        // Zeichnen der Kantenlinien eines Würfels: ca. 7-10 Mikrosek. (Zeitmessung 8.6.22)
            // positionHandle = GLES20.glGetAttribLocation(openGLprogram, "aPosition");
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    0, lineVerticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            int colorHandle = GLES20.glGetAttribLocation(openGLprogram, "aColor");
            GLES20.glDisableVertexAttribArray(colorHandle);
            GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, COLORS_PER_VERTEX*BYTES_PER_FLOAT, lineColorsBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            GLES20.glLineWidth(lineWidth);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertexCount);
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
        }

        /*
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glDisableVertexAttribArray(pointLightPosHandle);
        GLES20.glDisableVertexAttribArray(directionalLightVectorHandle);
        GLES20.glDisableVertexAttribArray(relativePointLightShareHandle);
        GLES20.glDisableVertexAttribArray(ambientLightHandle);

         */

    }

    synchronized public void setId(String id) {
        this.id = id;
    }

    synchronized public String getId() {
        return id;
    }

    synchronized public boolean isCompiled() {
        return isCompiled;
    }

    synchronized public void setSurfaceView(GLSurfaceViewCV surfaceView) {
        this.surfaceView = surfaceView;
    }

    synchronized public GLSurfaceViewCV getSurfaceView() {
        return surfaceView;
    }

    /**
     * Gets an array with copies of all triangles of the shape.
     * @return An array with copies of the triangles.
     */

    synchronized public GLTriangleCV[] getTriangles() {
        // long start = System.nanoTime();
        if (triangles==null) return null;
        GLTriangleCV[] trianglesCopy = new GLTriangleCV[triangles.length];
        for (int i=0; i<triangles.length; i++)
            trianglesCopy[i] = triangles[i].clone();
        // long duration = System.nanoTime() - start;
        // Log.v("GLDEMO",">>> getTriangles: "+duration+" ns");
        return trianglesCopy;
    }

    /**
     * Returns the number of triangles that belong to the shape.
     * @return The number of triangles.
     */

    synchronized public int getNumberOfTriangles() {
        if (triangles==null) return 0;
        return triangles.length;
    }

    /**
     * Adds a triangle to the shape.
     * @param newTriangle The triangle to be added.
     */

    synchronized public void addTriangle(GLTriangleCV newTriangle) {
        if (newTriangle==null) return;
        GLTriangleCV[] newTriangleArray = { newTriangle };
        addTriangles(newTriangleArray);
    }

    /**
     * Adds copies of triangles to the shape.
     * @param newTriangles The triangles to be added.
     */

    synchronized public void addTriangles(GLTriangleCV[] newTriangles) {
        addTriangles(newTriangles,true);
    }

    /**
     * Adds triangles to the shape.
     * @param newTriangles The triangles to be added.
     * @param makeCopies Specifies if the triangles of 'newTriangles' shall be copied before adding them.
     */

    synchronized public void addTriangles(GLTriangleCV[] newTriangles, boolean makeCopies) {
        if (newTriangles==null||newTriangles.length==0) return;
        if (triangles==null) {
            if (makeCopies)
                triangles = newTriangles.clone();
              else
                triangles = newTriangles;
            colorArrayOfTriangles = null;
            setModelMatrixAndBuffers();
            return;
        }
        GLTriangleCV[] newTriangleAttribute = new GLTriangleCV[triangles.length+newTriangles.length];
        for (int i=0; i<triangles.length; i++)
            newTriangleAttribute[i] = triangles[i];
        for (int i=0; i<newTriangles.length; i++)
            if (makeCopies)
                newTriangleAttribute[triangles.length+i] = newTriangles[i].clone();
              else
                newTriangleAttribute[triangles.length+i] = newTriangles[i];
        triangles = newTriangleAttribute;
        colorArrayOfTriangles = null;
        setModelMatrixAndBuffers();
        intrinsicSize = new float[3];
        intrinsicSize[0] = calculateIntrinsicSize(0);
        intrinsicSize[1] = calculateIntrinsicSize(1);
        intrinsicSize[2] = calculateIntrinsicSize(2);

    }

    /**
     * Gets an array with copies of all lines of the shape.
     * @return The array with the lines.
     */

    synchronized public GLLineCV[] getLines() {
        if (lines==null) return null;
        GLLineCV[] linesCopy = new GLLineCV[lines.length];
        for (int i=0; i<lines.length; i++)
            linesCopy[i] = lines[i].clone();
        return linesCopy;
    }

    /**
     * Returns the number of lines that belong to the shape.
     * @return The number of lines.
     */

    synchronized public int getNumberOfLines() {
        if (lines==null) return 0;
        return lines.length;
    }

    /**
     * Adds a line to the shape.
     * If the shape had no lines before, the lineWidth is set to 10.
     * @param newLine The line to be added.
     */

    synchronized public void addLine(GLLineCV newLine) {
        if (newLine==null) return;
        GLLineCV[] newLines = { newLine };
        addLines(newLines);
    }

    /**
     * Adds lines to the shape.
     * If the shape had no lines before, the lineWidth is set to 10.
     * @param newLines The lines to be added
     */

    synchronized public void addLines(GLLineCV[] newLines) {
        if (newLines==null||newLines.length==0) return;
        if (lines==null) {
            lines = newLines;
            lineWidth = 10;
            setModelMatrixAndBuffers();
            return;
        }
        GLLineCV[] newLinesAttribute = new GLLineCV[lines.length+newLines.length];
        for (int i=0; i<lines.length; i++)
            newLinesAttribute[i] = lines[i];
        for (int i=0; i<newLines.length; i++)
            newLinesAttribute[lines.length+i] = newLines[i];
        lines = newLinesAttribute;
        setModelMatrixAndBuffers();
        intrinsicSize = new float[3];
        intrinsicSize[0] = calculateIntrinsicSize(0);
        intrinsicSize[1] = calculateIntrinsicSize(1);
        intrinsicSize[2] = calculateIntrinsicSize(2);
    }

    /**
     * @param lineWidth The new line width (must be larger than 0)
     */

    synchronized public void setLineWidth(float lineWidth) {
        if (lineWidth<=0) return;
        this.lineWidth = lineWidth;
    }

    /**
     * @return The line width of the shape
     */

    synchronized public float getLineWidth() {
        return this.lineWidth;
    }

    /**
     * Gets an array with copies of the vertices of all triangles and lines of the shape.
     * @return A two-dimensional array containing float triples with the x, y, and z coordinates of the vertices in model coordinate space.
     * This array may contain duplicates.
     */

    synchronized public float[][] getVertices() {
        if (getNumberOfTriangles()==0&&getNumberOfLines()==0) return null;
        float[][] vertices = new float[getNumberOfTriangles()*3+getNumberOfLines()*2][3];
        int i = 0;
        if (triangles!=null)
            for (GLTriangleCV triangle : triangles) {
                float[][] v = triangle.getVertices();
                vertices[i++] = v[0];
                vertices[i++] = v[1];
                vertices[i++] = v[2];
            }
        if (lines!=null)
            for (GLLineCV line : lines) {
                vertices[i++] = line.getPoint1();
                vertices[i++] = line.getPoint2();
            }
        return vertices;
    }

    /**
     * Sets all triangle colors to the same uniform color.
     * @param color The color (if not valid the triangles remain unchanged)
     */

    synchronized public void setTrianglesUniformColor(float[] color) {
        if (!GLShapeFactoryCV.isValidColorArray(color)) return;
        if (triangles!=null) {
            for (GLTriangleCV triangle : triangles)
                triangle.setUniformColor(color);
                // triangle.setVertexColors(new float[][] {color, color, color});
            colorArrayOfTriangles=null;
            setModelMatrixAndBuffers();
        }
    }

    /**
     * Changes the center of the shape, i.e. the origin (0,0,0) of its local coordinate system ("model coordinate system"),
     * by translating all coordinate values of its triangles and lines by the same vector.
     * This will, in particular, affect future rotation operations on the shape as shapes are rotated around their origin.
     * <BR>
     * Note the difference between this method and the methods setScale(), setTrans() etc.:
     * This method changes the origin of the local coordinate system and hence modifies the coordinate values of its triangles and lines.
     * The other methods specify how the shape shall be placed into the real world,
     * i.e. how to calculate the model matrix (see method buildModelMatrixFromAttributes()),
     * but leave the coordinate values stored with the triangles and lines unchanged.
     * @param trans x, y, and y components of the translation vector.
     * @return a reference to the modified shape
     */

    synchronized public GLShapeCV moveCenterTo(float[] trans) {
        return moveCenterTo(trans[0],trans[1],trans[2]);
    }

    /**
     * Changes the center of the shape, i.e. the origin (0,0,0) of its local coordinate system ("model coordinate system"),
     * by translating all coordinate values of its triangles and lines by the same vector.
     * This will, in particular, affect future rotation operations on the shape as shapes are rotated around their origin.
     * <BR>
     * Note the difference between this method and the methods setScale(), setTrans() etc.:
     * This method changes the origin of the local coordinate system and hence modifies the coordinate values of its triangles and lines.
     * The other methods specify how the shape shall be placed into the real world,
     * i.e. how to calculate the model matrix (see method buildModelMatrixFromAttributes()),
     * but leave the coordinate values stored with the triangles and lines unchanged.
     * @param transX x component of the translation vector.
     * @param transY y component of the translation vector.
     * @param transZ z component of the translation vector.
     * @return a reference to the modified shape
     */

    synchronized public GLShapeCV moveCenterTo(float transX, float transY, float transZ) {
        // Log.v("GLDEMO","moveCenterTo: "+transX+" "+transY+" "+transZ);
        if (triangles!=null)
            for (GLTriangleCV triangle: triangles)
                triangle.translate(-transX,-transY,-transZ);
        if (lines!=null)
            for (GLLineCV line: lines)
                line.translate(-transX,-transY,-transZ);
        setModelMatrixAndBuffers();
        return this;
    }

    /**
     * Flips/mirrors the shape in the x, y, and/or z dimension
     * by changing the signs of all corresponding coordinate values of its triangles.
     * <BR>
     * Note the difference between this method and the methods setScale(), setTrans() etc.:
     * This method modifies the coordinate values of its triangles and lines.
     * The other methods specify how the shape shall be placed into the real world,
     * i.e. how to calculate the model matrix (see method buildModelMatrixFromAttributes()),
     * but leave the coordinate values stored with the triangles and lines unchanged.
     * @param flipX Specifies if the shape shall be flipped in the x dimension.
     * @param flipY Specifies if the shape shall be flipped in the y dimension.
     * @param flipZ Specifies if the shape shall be flipped in the z dimension.
     * @return a reference to the modified shape
     */

    synchronized public GLShapeCV flip(boolean flipX, boolean flipY, boolean flipZ) {
        if (triangles!=null)
            for (GLTriangleCV triangle: triangles)
                triangle.flip(flipX,flipY,flipZ);
        if (lines!=null)
            for (GLLineCV line: lines)
                line.flip(flipX,flipY,flipZ);
        setModelMatrixAndBuffers();
        return this;
    }

    /**
     * Gets the maximum difference between the coordinates of any two triangle or line vertices in the x, y, or z dimension.
     * i.e. the extension of the enclosing cube in the selected dimension.
     * The value refers to the local coordinate system (model coordinate system),
     * i.e. disregards the transformations specified by the model matrix.
     * @param dimension The dimension for which the size shall be calculated (0 = x, 1 = y, 2 = z).
     * @return The extension of the shape in the selected dimension (-1 if the parameter was not valid).
     */

    synchronized public float getIntrinsicSize(int dimension) {
        if (dimension<0||dimension>2) return -1;
        return intrinsicSize[dimension];
    }

    /**
     * Auxiliary method to calculate the intrinsic size in one dimension.
     * Gets the maximum difference between the coordinates of any two triangle or line vertices in the x, y, or z dimension.
     * i.e. the extension of the enclosing cube in the selected dimension.
     * The value refers to the local coordinate system (model coordinate system),
     * i.e. disregards the transformations specified by the model matrix.
     * @param dimension The dimension for which the size shall be calculated (0 = x, 1 = y, 2 = z).
     * @return The extension of the shape in the selected dimension (-1 if the parameter was not valid).
     */

    synchronized private float calculateIntrinsicSize(int dimension) {
        if (dimension<0||dimension>2) return -1;
        float min=Float.MAX_VALUE, max=Float.MIN_VALUE;
        if (triangles!=null) {
            for (GLTriangleCV triangle : triangles) {
                float[][] vertices = triangle.getVertices();
                for (int i = 0; i < 3; i++) {
                    if (vertices[i][dimension] < min)
                        min = vertices[i][dimension];
                    if (vertices[i][dimension] > max)
                        max = vertices[i][dimension];
                }
            }
        }
        if (lines!=null) {
            for (int i=0;i<lines.length;i++) {
                if (lines[i].getPoint1()[dimension]<min)
                    min = lines[i].getPoint1()[dimension];
                if (lines[i].getPoint1()[dimension]>max)
                    max = lines[i].getPoint1()[dimension];
                if (lines[i].getPoint2()[dimension]<min)
                    min = lines[i].getPoint1()[dimension];
                if (lines[i].getPoint2()[dimension]>max)
                    max = lines[i].getPoint2()[dimension];
            }
        }
        return max-min;
    }

    /**
     * Sets the scaling factor in the x dimension and updates the model matrix accordingly.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setScaleX(float scaleX) {
        scalingMatrix[0] = scaleX;
        buildModelMatrix();
        return this;
    }

    synchronized public float getScaleX() {
        return scalingMatrix[0];
    }

    /**
     * Sets the scaling factor in the x dimension and updates the model matrix accordingly.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setScaleY(float scaleY) {
        scalingMatrix[5] = scaleY;
        buildModelMatrix();
        return this;
    }

    synchronized public float getScaleY() {
        return scalingMatrix[5];
    }

    /**
     * Sets the scaling factor in the z dimension and updates the model matrix accordingly.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setScaleZ(float scaleZ) {
        scalingMatrix[10] = scaleZ;
        buildModelMatrix();
        return this;
    }

    synchronized public float getScaleZ() {
        return scalingMatrix[10];
    }

    /**
     * Gets a copy of the scaling matrix, i.e. the scalingMatrix attribute.
     * @return The copy of the scaling matrix.
     */

    synchronized public float[] getScalingMatrix() {
        return scalingMatrix.clone();
    }

    /**
     * Sets the attributes for the scaling factors and updates the model matrix accordingly.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setScale(float scaleX, float scaleY, float scaleZ) {
        scalingMatrix[0] = scaleX;
        scalingMatrix[5] = scaleY;
        scalingMatrix[10] = scaleZ;
        buildModelMatrix();
        // Log.v("GLDEMO","setScale: "+scaleX+" "+scaleY+" "+scaleZ);
        return this;
    }

    /**
     * Sets the scaling factors for all three dimensions to the same value and updates the model matrix accordingly.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setScale(float scale) {
        return setScale(scale,scale,scale);
    }

    /**
     * Sets the rotation based on three cardan angles / Euler angles, i.e. modifies the rotationMatrix attribute accordingly.
     * (Euler angles see e.g. https://en.wikipedia.org/wiki/Euler_angles, "Tate-Bryan angles" / "cardan angles".)
     * The rotation order is X > Z > Y. Note that this order is fixed because rotation operations are not commutative.
     * <BR>
     * The implementation is based on the code in
     * https://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm.
     * @param eulerAngles Array of length 3 with the rotation angles at positions 0, 1, and 2
     *                    relative to the x-, y-, and z-axes of the model coordinate system (i.e. the coordinate system of the shape).
     * @return The shape itself, such that calls of methods of this kind can be daisy chained, or null if the parameter is not valid.
     */

    synchronized public GLShapeCV setRotationByEulerAngles(float[] eulerAngles) {
        if (eulerAngles==null||eulerAngles.length!=3)
            return null;
        return setRotationByEulerAngles(eulerAngles[0],eulerAngles[1],eulerAngles[2]);
    }

    /**
     * Sets the rotation based on three cardan angles / Euler angles, i.e. modifies the rotationMatrix attribute accordingly.
     * (Euler angles see e.g. https://en.wikipedia.org/wiki/Euler_angles, "Tate-Bryan angles" / "cardan angles".)
     * The rotation order is X > Z > Y. Note that this order is fixed because rotation operations are not commutative.
     * <BR>
     * The implementation is based on the code in
     * https://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm.
     * @param eulerX rotation around the x axis in the model coordinate system (i.e. the coordinate system of the shape).
     * @param eulerY subsequent rotation around the y axis in the model coordinate system (i.e. the coordinate system of the shape).
     * @param eulerZ subsequent rotation around the z axis in the model coordinate system (i.e. the coordinate system of the shape).
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setRotationByEulerAngles(float eulerX, float eulerY, float eulerZ) {

        /* float cosX = (float) Math.cos(Math.PI*eulerX/180.0);
        float sinX = (float) Math.sin(Math.PI*eulerX/180.0);
        float cosY = (float) Math.cos(Math.PI*eulerY/180.0);
        float sinY = (float) Math.sin(Math.PI*eulerY/180.0);
        float cosZ = (float) Math.cos(Math.PI*eulerZ/180.0);
        float sinZ = (float) Math.sin(Math.PI*eulerZ/180.0);
        rotationMatrix[0] = cosY * cosZ;
        rotationMatrix[1] = sinY*sinX - cosY*sinZ*cosX;
        rotationMatrix[2] = cosY*sinZ*sinX + sinY*cosX;
        rotationMatrix[3] = 0.0f;
        rotationMatrix[4] = sinZ;
        rotationMatrix[5] = cosZ*cosX;
        rotationMatrix[6] = -cosZ*sinX;
        rotationMatrix[7] = 0.0f;
        rotationMatrix[8] = -sinY*cosZ;
        rotationMatrix[9] = sinY*sinZ*cosX + cosY*sinX;
        rotationMatrix[10] = -sinY*sinZ*sinX + cosY*cosX;
        rotationMatrix[11] = rotationMatrix[12] = rotationMatrix[13] = rotationMatrix[14] = 0.0f;
        rotationMatrix[15] = 1.0f; */

        rotationMatrix = GraphicsUtilsCV.rotationMatrixFromEulerAngles(eulerX,eulerY,eulerZ);
        buildModelMatrix();
        return this;
    }

    /**
     * Sets the rotation angle and the rotation matrix, i.e. modifies the rotationMatrix attribute accordingly.
     * <BR>
     * https://registry.khronos.org/OpenGL-Refpages/gl2.1/xhtml/glRotate.xml:
     * "This rotation follows the right-hand rule, so if the vector (= rotation axis) points toward the user, the rotation will be counterclockwise."
     * @param rotAngle The rotation angle to be set.
     * @param rotAxis The rotation axis to be set.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setRotation(float rotAngle, float[] rotAxis) {
        return setRotation(rotAngle,rotAxis[0],rotAxis[1],rotAxis[2]);
    }

    /**
     * Sets the rotation angle and the rotation matrix, i.e. modifies the rotationMatrix attribute accordingly.
     * <BR>
     * https://registry.khronos.org/OpenGL-Refpages/gl2.1/xhtml/glRotate.xml:
     * "This rotation follows the right-hand rule, so if the vector (= rotation axis) points toward the user, the rotation will be counterclockwise."
     * @param rotAngle The rotation angle to be set.
     * @param rotAxisX x component of the rotation axis to be set.
     * @param rotAxisY y component of the rotation axis to be set.
     * @param rotAxisZ z component of the rotation axis to be set.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setRotation(float rotAngle, float rotAxisX, float rotAxisY, float rotAxisZ) {
        Matrix.setRotateM(rotationMatrix,0,rotAngle,rotAxisX,rotAxisY,rotAxisZ);
        // Log.v("GLDEMO",rotAngle+" "+rotAxis[0]+" "+rotAxis[1]+" "+rotAxis[2]);
        buildModelMatrix();
        return this;
    }

    /* NOT NEEDED ANYMORE
     * Sets the rotation angle and the rotation matrix, i.e. modifies the rotationMatrix attribute accordingly.
     * This method is primarily to be used by the rotation animator (see GLAnimatorFactorCV.addAnimatorRot())
     * which requires a method with single parameter.
     * <BR>
     * https://registry.khronos.org/OpenGL-Refpages/gl2.1/xhtml/glRotate.xml:
     * "This rotation follows the right-hand rule, so if the vector (= rotation axis) points toward the user, the rotation will be counterclockwise."
     * @param rotAngleAndAxis The rotation angle (in position 0) and axis (in positions 1-3).
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.

    synchronized public GLShapeCV setRotationForAnimator(float[] rotAngleAndAxis) {
        // Log.v("GLDEMO",">>> "+id+": setRotationForAnimator");
        float[] rotAxis = new float[3];
        rotAxis[0] = rotAngleAndAxis[1];
        rotAxis[1] = rotAngleAndAxis[2];
        rotAxis[2] = rotAngleAndAxis[3];
        return setRotation(rotAngleAndAxis[0],rotAxis);
    }

    /*
    synchronized public GLShapeCV setRotationForAnimatorVs2(float angle, float axisX, float axisY, float axisZ) {
        Log.v("GLDEMO",">>> "+id+": setRotationForAnimatorVs2");
        float[] rotAxis = new float[3];
        rotAxis[0] = axisX;
        rotAxis[1] = axisY;
        rotAxis[2] = axisZ;
        return setRotation(angle,rotAxis);
    }
     */

    /**
     * Sets the rotation matrix, i.e. the rotationMatrix attribute.
     * @param rotationMatrix The values for the rotation matrix (a float array of length 16, as required by OpenGL).
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     * (null if the parameter is not valid, i.e. null or an array with length not equal 16 or no rotation matrix).
     */

    synchronized public GLShapeCV setRotationMatrix(float[] rotationMatrix) {
        return setRotationMatrix(rotationMatrix,true);
    }

    /**
     * Sets the rotation matrix, i.e. the rotationMatrix attribute.
     * @param rotationMatrix The values for the rotation matrix (a float array of length 16, as required by OpenGL).
     * @param matrixCheck Specifies whether it shall be checked that rotationMatrix is indeed a rotation matrix.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     * (null if the rotationMatrix parameter is not valid, i.e. null or an array with length not equal 16 or, if matrixCheck is set, is no rotationMatrix).
     */

    synchronized public GLShapeCV setRotationMatrix(float[] rotationMatrix, boolean matrixCheck) {
        if (matrixCheck&&!GraphicsUtilsCV.is4x4RotationMatrix(GraphicsUtilsCV.matrixFromArray(rotationMatrix,4,4))) {
            Log.v("GLDEMO","Error "+id+": This is no rotation matrix!");
            return null;
        }
        this.rotationMatrix = rotationMatrix.clone();
        buildModelMatrix();
        return this;
    }

    /**
     * Gets the rotation axis.
     * @return The rotation axis calculated from the rotationMatrix attribute.
     */

    synchronized public float[] getRotAxis() {
        return GraphicsUtilsCV.rotAxisFrom4x4RotationMatrix(GraphicsUtilsCV.matrixFromArray(rotationMatrix,4,4));
    }

    /**
     * Gets the rotation angle.
     * @return The rotation angle calculated from the rotationMatrix attribute.
     */

    synchronized public float getRotAngle() {
        return GraphicsUtilsCV.rotAngleFrom4x4RotationMatrix(GraphicsUtilsCV.matrixFromArray(rotationMatrix,4,4));
    }

    /**
     * Gets a copy of the rotation matrix, i.e. the rotationMatrix attribute.
     * @return The copy of the rotation matrix.
     */

    synchronized public float[] getRotationMatrix() {
        return rotationMatrix.clone();
    }

    /**
     * Adds a rotation around the shape's own x axis to the current rotation,
     * i.e. takes the current orientation of the shape in world space and lets it flip ("pitch") up or down,
     * and updates the model matrix accordingly.
     * @param angle The angle by which the shape shall be flipped/pitched.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV addRotationAroundOwnXAxis(float angle) {
        if (angle==0) return this;
        float[] xAxis = {1, 0, 0, 0};
        Matrix.multiplyMV(xAxis, 0, rotationMatrix, 0, xAxis, 0);
        float[] extraRotMatrix = new float[16];
        Matrix.setRotateM(extraRotMatrix, 0, angle, xAxis[0], xAxis[1], xAxis[2]);
        Matrix.multiplyMM(rotationMatrix, 0, extraRotMatrix, 0, rotationMatrix, 0);
        buildModelMatrix();
        return this;
    }

    /**
     * Adds a rotation around the shape's own y axis to the current rotation,
     * i.e. takes the current orientation of the shape in world space and lets it turn ("yaw") left or right,
     * and updates the model matrix accordingly.
     * @param angle The angle by which the shape shall be turned/yawed.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV addRotationAroundOwnYAxis(float angle) {
        if (angle==0) return this;
        float[] yAxis = {0, 1, 0, 0};
        Matrix.multiplyMV(yAxis, 0, rotationMatrix, 0, yAxis, 0);
        float[] extraRotMatrix = new float[16];
        Matrix.setRotateM(extraRotMatrix, 0, angle, yAxis[0], yAxis[1], yAxis[2]);
        Matrix.multiplyMM(rotationMatrix, 0, extraRotMatrix, 0, rotationMatrix, 0);
        buildModelMatrix();
        return this;
    }

    /**
     * Adds a rotation around the shape's own z axis to the current rotation,
     * i.e. takes the current orientation of the shape in world space and lets it flip ("roll") left or right,
     * and updates the model matrix accordingly.
     * @param angle The angle by which the shape shall be flipped/rolled.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV addRotationAroundOwnZAxis(float angle) {
        if (angle==0) return this;
        float[] zAxis = {0, 0, 1, 0};
        Matrix.multiplyMV(zAxis, 0, rotationMatrix, 0, zAxis, 0);
        float[] extraRotMatrix = new float[16];
        Matrix.setRotateM(extraRotMatrix, 0, angle, zAxis[0], zAxis[1], zAxis[2]);
        Matrix.multiplyMM(rotationMatrix, 0, extraRotMatrix, 0, rotationMatrix, 0);
        buildModelMatrix();
        return this;
    }

    /**
     * Sets the value of the 'transX' attribute and updates the model matrix accordingly.
     * @param transX The new value for the attribute.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setTransX(float transX) {
        translationMatrix[12] = transX;
        buildModelMatrix();
        return this;
    }

    synchronized public float getTransX() {
        return translationMatrix[12];
    }

    /**
     * Sets the value of the 'transY' attribute and updates the model matrix accordingly.
     * @param transY The new value for the attribute.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setTransY(float transY) {
        translationMatrix[13] = transY;
        buildModelMatrix();
        return this;
    }

    synchronized public float getTransY() {
        return translationMatrix[13];
    }

    /**
     * Sets the value of the 'transZ' attribute and updates the model matrix accordingly.
     * @param transZ The new value for the attribute.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setTransZ(float transZ) {
        translationMatrix[14] = transZ;
        buildModelMatrix();
        return this;
    }

    synchronized public float getTransZ() {
        return translationMatrix[14];
    }

    /**
     * Sets the values of the translation attributes and updates the model matrix accordingly.
     * @param transX The new value for the 'transX' attribute.
     * @param transY The new value for the 'transY' attribute.
     * @param transZ The new value for the 'transZ' attribute.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setTrans(float transX, float transY, float transZ) {
        translationMatrix[12] = transX;
        translationMatrix[13] = transY;
        translationMatrix[14] = transZ;
        buildModelMatrix();
        return this;
    }

    /**
     * Sets the values of the translation attributes and updates the model matrix accordingly.
     * @param trans The new values for the attributes (trans[0] = transX, trans[1] = transY, trans[2] = transZ).
     * @return The shape itself, such that calls of methods of this kind can be daisy chained.
     */

    synchronized public GLShapeCV setTrans(float[] trans) {
        // Log.v("GLDEMO","setTrans: "+trans[0]+" "+trans[1]+" "+trans[2]);
        return setTrans(trans[0],trans[1],trans[2]);
    }

    /**
     * Gets the current values of the translation attributes.
     * @return An array of length 3 with transX at pos. 0, transY at pos. 1, transZ at pos. 2)
     */

    synchronized public float[] getTrans() {
        float[] result = new float[3];
        result[0] = translationMatrix[12];
        result[1] = translationMatrix[13];
        result[2] = translationMatrix[14];
        return result;
    }

    /**
     * Gets a copy of the translation matrix, i.e. the translationMatrix attribute.
     * @return The copy of the translation matrix.
     */

    synchronized public float[] getTranslationMatrix() {
        return translationMatrix.clone();
    }

    /**
     * Aligns the shape with a given vector,
     * i.e. rotates it such that its x, y, or z axis lies in parallel with the vector,
     * and updates the model matrix accordingly.
     * @param axisToAlign The axis of the shape that shall be aligned with the vector - 0 = x axis, 1 = y axis, 2 = z axis
     * @param vector The vector to align the shape with.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained (or null if one of the parameters is not valid).
     */

    synchronized public GLShapeCV alignWith(int axisToAlign, float[] vector) {
        return alignWith(axisToAlign,vector,false,0);
    }

    /**
     * Aligns the shape with a given vector,
     * i.e. rotates it such that its x, y, or z axis lies in parallel with the vector,
     * and updates the model matrix accordingly.
     * It might be that after the alignment a.) the shape points into the "wrong" direction
     * and b.) an additional rotation around the aligned axis is required in order to balance the shape.
     * This can be handled by using the third and the fourth parameter.
     * @param axisToAlign The axis of the shape that shall be aligned with the vector - 0 = x axis, 1 = y axis, 2 = z axis
     * @param vector The vector to align the shape with.
     * @param flip Indicates whether the shape shall be turned by 180 degrees because it points into the "wrong" direction.
     * @param extraRot The angle for an additional rotation around the aligned axis.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained (or null if one of the parameters is not valid).
     */

    synchronized public GLShapeCV alignWith(int axisToAlign, float[] vector, boolean flip, float extraRot ) {
        if (vector==null||vector.length!=3||
                (vector[0]==0&&vector[1]==0&&vector[2]==0)
                || axisToAlign<0 || axisToAlign>2) return null;
        float[] axisToAlignVector = new float[3];
        axisToAlignVector[axisToAlign] = 1;
        float[] vectorNormalized = GraphicsUtilsCV.getNormalizedVectorCopy3D(vector);
        float[] rotAxis = GraphicsUtilsCV.crossProduct3D(axisToAlignVector,vectorNormalized);
        float rotAngle = (float)(180*Math.acos(GraphicsUtilsCV.dotProduct3D(axisToAlignVector,vectorNormalized))/Math.PI);
        if (!GraphicsUtilsCV.valuesEqual(rotAngle,0,0.0001)) {
            float[] rotMatrix = new float[16];
            Matrix.setRotateM(rotMatrix, 0, rotAngle, rotAxis[0], rotAxis[1], rotAxis[2]);
            setRotationMatrix(rotMatrix);
        }
        if (flip)
            switch (axisToAlign) {
                case 0:
                case 2: addRotationAroundOwnYAxis(180); break;
                case 1: addRotationAroundOwnXAxis(180); break;
            }
        if (extraRot!=0)
            switch (axisToAlign) {
                case 0: addRotationAroundOwnXAxis(extraRot); break;
                case 1: addRotationAroundOwnYAxis(extraRot); break;
                case 2: addRotationAroundOwnZAxis(extraRot); break;
            }
        return this;
    }

    /**
     * Aligns the shape with a given vector,
     * i.e. rotates it such that its x, y, or z axis lies in parallel with the vector,
     * and updates the model matrix accordingly.
     * @param axisToAlign The axis of the shape that shall be aligned with the vector - 0 = x axis, 1 = y axis, 2 = z axis
     * @param vector The vector to align the shape with.
     * @param extraRotX An extra rotation of the shape around its own x axis, applied after the alignment rotation.
     * @param extraRotY An extra rotation of the shape around its own y axis, applied after the alignment rotation and the extra x rotation.
     * @param extraRotZ An extra rotation of the shape around its own z axis, applied after the alignment rotation and the extra x and y rotations.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained (or null if one of the parameters is not valid).
     */

    synchronized public GLShapeCV alignWith(int axisToAlign, float[] vector, float extraRotX, float extraRotY, float extraRotZ ) {
        if (vector==null||vector.length!=3||
                (vector[0]==0&&vector[1]==0&&vector[2]==0)
                || axisToAlign<0 || axisToAlign>2) return null;
        float[] axisToAlignVector = new float[3];
        axisToAlignVector[axisToAlign] = 1;
        float[] vectorNormalized = GraphicsUtilsCV.getNormalizedVectorCopy3D(vector);
        float[] rotAxis = GraphicsUtilsCV.crossProduct3D(axisToAlignVector,vectorNormalized);
        float rotAngle = (float)(180*Math.acos(GraphicsUtilsCV.dotProduct3D(axisToAlignVector,vectorNormalized)/Math.PI));
        if (!GraphicsUtilsCV.valuesEqual(rotAngle,0,0.0001)) {
            float[] rotMatrix = new float[16];
            Matrix.setRotateM(rotMatrix, 0, rotAngle, rotAxis[0], rotAxis[1], rotAxis[2]);
            setRotationMatrix(rotMatrix);
        }
        addRotationAroundOwnXAxis(extraRotX);
        addRotationAroundOwnYAxis(extraRotY);
        addRotationAroundOwnZAxis(extraRotZ);
        return this;
        /*
        if (extraRotX!=0) {
            float[] xAxis = {1, 0, 0, 0};
            Matrix.multiplyMV(xAxis, 0, rotMatrix, 0, xAxis, 0);
            float[] extraRotMatrixX = new float[16];
            Matrix.setRotateM(extraRotMatrixX, 0, extraRotX, xAxis[0], xAxis[1], xAxis[2]);
            Matrix.multiplyMM(rotMatrix, 0, extraRotMatrixX, 0, rotMatrix, 0);
        }
        if (extraRotY!=0) {
            float[] yAxis = {0, 1, 0, 0};
            Matrix.multiplyMV(yAxis, 0, rotMatrix, 0, yAxis, 0);
            float[] extraRotMatrixY = new float[16];
            Matrix.setRotateM(extraRotMatrixY, 0, extraRotY, yAxis[0], yAxis[1], yAxis[2]);
            Matrix.multiplyMM(rotMatrix, 0, extraRotMatrixY, 0, rotMatrix, 0);
        }
        if (extraRotZ!=0) {
            float[] zAxis = {0, 0, 1, 0};
            Matrix.multiplyMV(zAxis, 0, rotMatrix, 0, zAxis, 0);
            float[] extraRotMatrixZ = new float[16];
            Matrix.setRotateM(extraRotMatrixZ, 0, extraRotZ, zAxis[0], zAxis[1], zAxis[2]);
            Matrix.multiplyMM(rotMatrix, 0, extraRotMatrixZ, 0, rotMatrix, 0);
        }
        */
    }

    /**
     * Aligns the shape with another shape by copying the rotation matrix of that shape.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained (or null if the parameter is null).
     */

    synchronized public GLShapeCV alignWith(GLShapeCV shapeToAlignWith) {
        if (shapeToAlignWith==null) return null;
        setRotationMatrix(shapeToAlignWith.getRotationMatrix());
        return this;
    }

    /**
     * Scales, rotates, and translates the shape such that it will "connect" two points in 3D space,
     * i.e. the bottom of the shape will be mapped to the first point and the top of the shape to the second point.
     * Scaling will be done in the y dimension.
     * Rotation will rotate the shape from the direction defined by the y axis (0,1,0) to the direction defined by the vector between the two points.
     * The method is primarily applicable to prisms, cuboids, and pyramids.
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The shape itself, such that calls of methods of this kind can be daisy chained (or null if one of the parameters is not valid).
     */

    synchronized public GLShapeCV placeBetweenPoints(float[] point1, float[] point2) {
        if (point1==null||point1.length!=3||point2==null||point2.length!=3) return null;
        // scaling in the y dimension
        setScaleY(GraphicsUtilsCV.distance3D(point1,point2)/getIntrinsicSize(1));
        // rotation of the shape:
        // - current orientation is assumed to be (0,1,0)
        // - shape must be rotated around a vector that is the cross product of the vectors (0,1,0) and (axisPoint1-axisPoint2),
        //   i.e. a vector that is perpendicular to the plane spanned by these two vectors
        //   (Details e.g.: https://stackoverflow.com/questions/69669771/calculate-rotation-to-align-object-with-two-points-in-3d-space)
        float[] y_axis = {0,1,0};
        float[] vectorBetweenPoints = GraphicsUtilsCV.vectorFromTo3D(point1,point2);
        if (Math.abs(vectorBetweenPoints[0])>10e-5||Math.abs(vectorBetweenPoints[2])>10e-5) {
            // rotate only if the rotation axis is not parallel to the y axis
            float[] rotAxisForShape = GraphicsUtilsCV.crossProduct3D(y_axis, vectorBetweenPoints);
            float rotAngleForShape = (float) Math.toDegrees(Math.acos(GraphicsUtilsCV.dotProduct3D(y_axis, GraphicsUtilsCV.getNormalizedVectorCopy3D(GraphicsUtilsCV.vectorFromTo3D(point1, point2)))));
            setRotation(rotAngleForShape,rotAxisForShape);
        }
        // center of the shape = the point in the middle between the two given points
        setTrans(GraphicsUtilsCV.midpoint3D(point1,point2));
        return this;
    }

    /**
     * Adds an animator to the list of animators that are to be started by a later call of startAnimators().
     * If the animator is of class AnimatorSet all animators of this set will be automatically removed from the animator list of this shape.
     * <BR>
     * Note that startAnimators() is called automatically when the shape is added by the GLShapeCV methods addShape() or addShapes().
     * If an animator is added to the shape after the shape has already been added to a surface view,
     * startAnimators() must be called again explicitly.
     * @param animator The animator to be added.
     * @return The added animator.
     */

    synchronized public Animator addAnimator(Animator animator) {
        if (animator==null) return null;
        animators.add(animator);
        animator.setTarget(this);
        if (animator.getClass()==AnimatorSet.class)
            for (Animator anim : ((AnimatorSet)animator).getChildAnimations()) {
                animators.remove(anim);
                anim.cancel();
            }
        return animator;
        /* It is not necessary to register an update listener
           because the renderer method onDrawFrame() will be called automatically
           when the animator has modified the shape's attributes.
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                try {
                    float[] viewProjectionMatrix = getSurfaceView().getRenderer().getViewProjectionMatrix();
                    // Log.v("DEMO",">>> onAnimationUpdate");
                    getSurfaceView().queueEvent(new Runnable() {
                        @Override
                        public void run() {
                                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
                                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                                GLES20.glEnable(GLES20.GL_DEPTH_TEST);  // such that fragments in the front ...
                                GLES20.glDepthFunc(GLES20.GL_LESS);     // ... hide fragments in the back
                                GLES20.glDepthMask( true );
                                // draw the shapes based on the current view projection matrix
                                ArrayList<GLShapeCV> shapesToRender = surfaceView.getShapesToRender();
                                for (GLShapeCV shape : shapesToRender)
                                    shape.draw(viewProjectionMatrix);
                            // draw(viewProjectionMatrix);
                        }
                    });
                } catch (NullPointerException exc) { return; }
            }
        });
        */
    }

    /**
     * Removes an animator from the list of animators.
     * @param animator The animator to be removed.
     */

    synchronized public void removeAnimator(Animator animator) {
        animators.remove(animator);
        animator.cancel();
    }

    /**
     * Starts the animators that are listed by the 'animators' attribute. After starting the animators the 'animators' attribute is cleared.
     * <BR>
     * This method will especially be called from the addShape() method of an GLSurfaceViewCV object, i.e. when the shape is added to a surface view.
     */

    synchronized public void startAnimators() {
        if (animators!=null)
            for (Animator animator : animators)
                animator.start();
        animators = new ArrayList<>();  // remove animators such that they will not be started again in the he next call of this method
        /*
        if (animators!=null&&!animators.isEmpty()) {
            AnimatorSet animset = new AnimatorSet();
            Animator animarray[] = new Animator[animators.size()];
            int i = 0;
            for (Animator animator : animators)
                animarray[i++] = animator;
            animset.playTogether(animarray);
            animset.start();
        }
         */
    }

    /**
     * Stops the control thread and all animators associated with the shape.
     */

    public void stopControlThreadAndAnimators() {
        if (controlThread!=null) {
            controlThread.interrupt();
        }
        if (animators!=null)
            for (Animator animator : animators)
                animator.end();
    }

    /**
     * Type of morphing animations: Triangle vertex coordinates
     */

    static public final int MORPHTYPE_TRIANGLE_VERTEX = 1;

    /**
     * Type of morphing animations: Triangle color values
     */

    static public final int MORPHTYPE_TRIANGLE_COLOR = 2;

    /**
     * Type of morphing animations: Line vertex coordinates
     */

    static public final int MORPHTYPE_LINE_VERTEX = 3;

    /**
     * Type of morphing animations: Line color values
     */

    static public final int MORPHTYPE_LINE_COLOR = 4;

    /**
     *  A class for control threads for shapes.
     *  The general idea is that such control threads modify selected vertex coordinates and color values and thus "morph" shapes in their model coordinate spaces.
     *  This morphing will be done by modifying entries of the 'triangleVerticesBuffer', 'triangleColorsBuffer', 'lineVerticesBuffer', and/or 'lineColorsBuffer'
     *  such that the coordinates and colors of the triangles and lines themselves, as defined by the 'triangles' and 'lines' attributes, will remain unchanged.
     *  N.B. Animation operations that affect the model matrix of shapes, i.e. their placement in the world,
     *  will be controlled by animators as specified by the 'animators' attribute and built by the methods of class GLAnimatorFactoryCV.
     */

    public class ControlThread extends Thread {

        /** The number of times per second the control thread shall become active */

        private final int stepsPerSecond;

        /** The value providers used for morphing operations,
         * i.e. for modifying / animating coordinate and color values entries */

        private ArrayList<GraphicsUtilsCV.ValueProvider> valueProviders;

        /** The first indices of the buffer intervals the value providers shall affect
         * (the ith entry of 'startIndices' referring to the ith provider in 'valueProviders').
         * The length of an interval is determined by the lengths of the arrays returned by the provider:
         * If the provider returns arrays of length n,
         * the buffer entries at positions 'startIndex', 'startIndex+1', ..., 'startIndex+n-1' will be set with these values. */

        private ArrayList<Integer> startIndices;

        ControlThread() { this.stepsPerSecond = 0; }

        /**
         * @param stepsPerSecond The number of times per second the control thread shall become active
         * @param valueProviders The value providers used to modify / animate entries of the 'triangleVerticesBuffer' (may be null if no triangles shall be animated)
         * @param startIndices The start indices of the 'triangleVerticesBuffer' intervals the providers shall affect - see the comment on the startIndicesTriangleVB attribute for more details (may also be null)
         */

        ControlThread(int stepsPerSecond, ArrayList<GraphicsUtilsCV.ValueProvider> valueProviders, ArrayList<Integer> startIndices) {
            this.stepsPerSecond = stepsPerSecond;
            if (valueProviders !=null) {
                this.valueProviders = (ArrayList<GraphicsUtilsCV.ValueProvider>) valueProviders.clone();
                this.startIndices = (ArrayList<Integer>) startIndices.clone();
            } else {
                this.valueProviders = null;
                this.startIndices = null;
            }
        }

        /**
         * The run method executes an infinite loop within which the thread becomes active 'stepsPerSecond' times per second.
         * It then calls the getNextValues() methods of all registered value providers and updates the 'triangleVerticesBuffer' and/or the 'lineVerticesBuffer' accordingly.
         */

        @Override
        public void run() {
            if ((valueProviders==null|| valueProviders.size()==0)) return;
            int sleepTime = 100;
            if (stepsPerSecond>0) sleepTime = 1000/stepsPerSecond;
            while (!interrupted()) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;  // quit the loop
                }
                if (valueProviders !=null)
                    for (int i = 0; i< valueProviders.size(); i++)
                        switch ((int)(valueProviders.get(i).getInfo())) {
                            case MORPHTYPE_TRIANGLE_VERTEX: setTriangleVerticesBuffer(startIndices.get(i), valueProviders.get(i).getNextValues()); break;
                            case MORPHTYPE_TRIANGLE_COLOR: setTriangleColorsBuffer(startIndices.get(i), valueProviders.get(i).getNextValues()); break;
                            case MORPHTYPE_LINE_VERTEX: setLineVerticesBuffer(startIndices.get(i), valueProviders.get(i).getNextValues()); break;
                            case MORPHTYPE_LINE_COLOR: setLineColorsBuffer(startIndices.get(i), valueProviders.get(i).getNextValues()); break;
                        }
            }
        }

    }

    /**
     * Initializes and starts a control thread for the shape.
     * @param stepsPerSecond The steps per second the thread shall execute
     * @param valueProvider The value provides for the morphing animation to be registered
     * @param startIndex The first index of the buffer interval that shall be affected by the provider
     */

    public void startControlThread(int stepsPerSecond, GraphicsUtilsCV.ValueProvider valueProvider, int startIndex) {
        ArrayList<GraphicsUtilsCV.ValueProvider> providerList = new ArrayList<>();
        providerList.add(valueProvider);
        ArrayList<Integer> indexList = new ArrayList<>();
        indexList.add(startIndex);
        controlThread = new ControlThread(stepsPerSecond,providerList,indexList);
        controlThread.start();
    }

    /**
     * Initializes and starts a control thread for the shape.
     * @param stepsPerSecond The steps per second the thread shall execute
     * @param valueProviders The value providers for morphing animations to be registered
     * @param startIndices The first indices of the buffer intervals that shall be affected by the providers
     * (the ith entry of 'startIndices' referring to the ith provider in 'valueProviders').
     */

    public void startControlThread(int stepsPerSecond, ArrayList<GraphicsUtilsCV.ValueProvider> valueProviders, ArrayList<Integer> startIndices) {
        controlThread = new ControlThread(stepsPerSecond,valueProviders,startIndices);
        controlThread.start();
    }

    /**
     * Method to set a sequence of 'triangleVerticesBuffer' entries in an atomic operation.
     * This method is primarily intended for the control thread.
     * @param startIndex The first index of the sequence in the 'triangleVerticesBuffer'
     * @param values The corresponding new values for the buffer entries, i.e. for positions startIndex, startIndex+1, ...
     */

    synchronized private void setTriangleVerticesBuffer(int startIndex, float[] values) {
        triangleVerticesBuffer.position(startIndex);
        triangleVerticesBuffer.put(values);
        triangleVerticesBuffer.position(0);
    }

    /**
     * Method to set a number of 'triangleVerticesBuffer' entries in an atomic operation.
     * This method is primarily intended for the control thread.
     * @param indices The indices of the buffer entries to be modified
     * @param values The corresponding new values for the buffer entries
     */

    synchronized private void setTriangleVerticesBuffer(int[] indices, float[] values) {
        for (int i=0; i<indices.length; i++) {
            triangleVerticesBuffer.position(indices[i]);
            triangleVerticesBuffer.put(values[i]);
        }
        triangleVerticesBuffer.position(0);
    }

    /**
     * Method to set a sequence of 'triangleColorsBuffer' entries in an atomic operation.
     * This method is primarily intended for the control thread.
     * @param startIndex The first index of the sequence in the 'triangleColorsBuffer'
     * @param values The corresponding new values for the buffer entries, i.e. for positions startIndex, startIndex+1, ...
     */

    synchronized private void setTriangleColorsBuffer(int startIndex, float[] values) {
        triangleColorsBuffer.position(startIndex);
        triangleColorsBuffer.put(values);
        triangleColorsBuffer.position(0);
    }

    /**
     * Method to set a sequence of 'lineVerticesBuffer' entries in an atomic operation.
     * This method is primarily intended for the control thread.
     * @param startIndex The first index of the sequence in the 'lineVerticesBuffer'
     * @param values The corresponding new values for the buffer entries, i.e. for positions startIndex, startIndex+1, ...
     */

    synchronized private void setLineVerticesBuffer(int startIndex, float[] values) {
        lineVerticesBuffer.position(startIndex);
        lineVerticesBuffer.put(values);
        lineVerticesBuffer.position(0);
    }

    /**
     * Method to set a sequence of 'lineColorsBuffer' entries in an atomic operation.
     * This method is primarily intended for the control thread.
     * @param startIndex The first index of the sequence in the 'lineColorsBuffer'
     * @param values The corresponding new values for the buffer entries, i.e. for positions startIndex, startIndex+1, ...
     */

    synchronized private void setLineColorsBuffer(int startIndex, float[] values) {
        lineColorsBuffer.position(startIndex);
        lineColorsBuffer.put(values);
        lineColorsBuffer.position(0);
    }

    /** Auxiliary method to get a one-dimensional float array with the vertex coordinates of the triangles */

    synchronized private float[] coordinateArrayFromTriangles() {
        float[] coordinateArray = new float[triangles.length*9];
        for (int triangleNo = 0; triangleNo<this.triangles.length; triangleNo++) {
            float[] coordsCurrentTriangle = this.triangles[triangleNo].getVertexCoordinates();
            // long start = System.nanoTime();
            for (int i=0; i<9; i++)    // a for loop is faster than System.arraycopy !
                coordinateArray[triangleNo*9+i] = coordsCurrentTriangle[i];
            // System.arraycopy(coordsCurrentTriangle,0,coordinateArray,triangleNo*9,9);
            //  Log.v("GLDEMO",">>> "+(System.nanoTime()-start));
        }
        return coordinateArray;
    }

    /** Auxiliary method to get a one-dimensional float array with the normals of the triangles */

    synchronized private float[] normalsArrayFromTriangles() {
        float[] normalsArray = new float[triangles.length*9];
        for (int triangleNo = 0; triangleNo<this.triangles.length; triangleNo++) {
            float[] normalCurrentTriangle = this.triangles[triangleNo].getNormal();
            // assign the normal coordinates three times in a row, i.e. for each of the three triangle vertices
            for (int i=0; i<9; i++)
                normalsArray[triangleNo * 9 + i] = normalCurrentTriangle[i%3];
        }
        return normalsArray;
    }

    /** Auxiliary method to get a one-dimensional float array with the vertex coordinates of the lines */

    synchronized private float[] coordinateArrayFromLines() {
        float[] coordinateArray = new float[lines.length*6];
        for (int lineNo = 0; lineNo<this.lines.length; lineNo++) {
            for (int i = 0; i < 3; i++)
                coordinateArray[lineNo * 6 + i] = this.lines[lineNo].getPoint1()[i];
            for (int i = 0; i < 3; i++)
                coordinateArray[lineNo * 6 + 3 + i] = this.lines[lineNo].getPoint2()[i];
        }
        return coordinateArray;
    }

    /** Auxiliary variable that specifies the colors of all triangles */

    private float[] colorArrayOfTriangles;

    /** Auxiliary method to get a one-dimensional float array with the vertex colors of the triangles */

    synchronized private float[] colorArrayFromTriangles() {
        if (colorArrayOfTriangles != null)
            return colorArrayOfTriangles;
        colorArrayOfTriangles = new float[triangles.length * 12];
        for (int i = 0; i < triangles.length; i++)      // all triangles
            for (int j = 0; j < 3; j++)    // all vertices of a triangle
                System.arraycopy(triangles[i].getVertexColor(j),0,colorArrayOfTriangles,i * 12 + j * 4,4);
                //      float[] vertexColor = triangles[i].getVertexColor(j);
                // for (int k = 0; k < 4; k++) {   // RGBA values of a vertex
                //  colorArrayOfTriangles[i * 12 + j * 4 + k] = vertexColor[k];
                // }
        return colorArrayOfTriangles;
    }

    /** Auxiliary method to get a one-dimensional float array with the colors of the lines */

    synchronized private float[] colorArrayFromLines() {
        float[] colorArray = new float[lines.length * 8];
        for (int i = 0; i < lines.length; i++) {      // all lines
            float[] lineColor = lines[i].getColor();
            for (int j = 0; j < 4; j++) {   // RGBA values of a line
                colorArray[i * 8 + j] = lineColor[j];  // first end point
                colorArray[i * 8 + 4 + j] = lineColor[j];  // second end point
            }
        }
        return colorArray;
    }

}

    // TODO colorArrayFromLines()

// ---- OBSOLETE CODE ----

/* In the following: methods needed for an external "morphing" thread/animator that modifies individual vertex coordinate values in the model coordinate space.

/**
 * Sets the values of a vertex of a triangle of the shape.
 * If multiple vertices shall be set, the method setTriangleVertices() should be used in order to avoid multiple costly updates of the shape buffers.
 * @param triangleID The ID of the triangle.
 * @param vertexNo The number of the triangle vertex (0, 1, or 2).
 * @param values the new coordinate values of the triangle vertex (x, y, and z).
 * @return true if the operation was successful; false if there is no triangle with such ID or one of the two other parameters is not correct.

synchronized public boolean setTriangleVertex(String triangleID, int vertexNo, float[] values) {
        for (GLTriangleCV triangle : triangles)
        if (triangle.getId().equals(triangleID)) {
        if (!triangle.setVertex(vertexNo,values)) return false;
        setModelMatrixAndBuffers();
        return true;
        }
        return false;
        }

/**
 * Sets the values of some triangle vertices of a shape.
 * @param triangleIDs The IDs of the affected triangles.
 * @param vertexNos vertexNos[i] = the number of the vertex of triangle[i] that shall be set (0, 1, or 2).
 * @param values values[i][] = the new coordinate values for this vertex (x, y, and z).

synchronized public void setTriangleVertices(String[] triangleIDs, int[] vertexNos, float[][] values) {
        for (int i=0;i<triangleIDs.length;i++)
        for (GLTriangleCV triangle : triangles)
        if (triangle.getId().equals(triangleIDs[i])) {
        triangle.setVertex(vertexNos[i], values[i]);
        }
        setModelMatrixAndBuffers();
        }

/**
 * Sets a vertex coordinate entry in the triangleVertexBuffer and consequently also in the GPU hardware.
 * The GLTriangleCV objects constituting the shape, i.e. the entries of the triangles[], are not affected.
 * Therefore, this method should be used with care.
 * It is primarily intended for animators that animate vertices in the model coordinate space.
 * @param index The index of the entry to be modified.
 * @param value The new value for the entry.

public synchronized void setTriangleVertexBufferEntry(int index, float value) {
        try {
        triangleVerticesBuffer.position(index);
        triangleVerticesBuffer.put(value);
        triangleVerticesBuffer.position(0);
        } catch (Exception e) {}
        }

/**
 * Sets the vertex coordinate entries in the triangleVertexBuffer and consequently also in the GPU hardware.
 * The GLTriangleCV objects constituting the shape, i.e. the entries of the triangles[], are not affected.
 * Therefore, this method should be used with care.
 * It is primarily intended for animators that animate vertices in the model coordinate space.
 * @param values The new values for the entries.

public synchronized void setTriangleVertexBufferEntries(float[] values) {
        try {
        // Diese Operation benötigt kaum Zeit - Messung 31.10.22 auf Samsung S21: Unter 0.1 ms für values-Array der Länge 36909
        // long startTime = System.nanoTime();
        triangleVerticesBuffer.position(0);
        triangleVerticesBuffer.put(values);
        triangleVerticesBuffer.position(0);
        // Log.v("GLDEMO","setTriangleVertexBufferEntries: "+(System.nanoTime()-startTime)/1000000.0+" ms ["+values.length+" float values]");
        } catch (Exception e) {}
        }

/**
 * Gets a vertex coordinate entry from the triangleVertexBuffer.
 * @param index The index of the entry.
 * @return The entry value.

public synchronized float getTriangleVertexBufferEntry(int index) {
        try {
        return triangleVerticesBuffer.get(index);
        } catch (Exception e) { return 0; }
        }

*/

/**
 * Control thread for bird shapes
 */

    /*
    private class BirdControlThread extends ControlThread {
        final int stepsPerSecond;
        BirdControlThread(int stepsPerSecond) {
            this.stepsPerSecond = stepsPerSecond;
        }
        @Override
        public void run() {
            super.run();
            float wingTailTipYAdd = 0;
            float stepWingsTail = 0.3f;
            float beakTipYAdd = 0;
            float stepBeak = 0.05f;
            final float beakY = 0.3f;
            final float tailtipY = 0.75f;
            int sleepTime = 100;
            if (stepsPerSecond>0) sleepTime = 1000/stepsPerSecond;
            while (!interrupted()) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (Exception e) {}
                int[] indices = { 7, 16, 36904, 36907, 36883, 36891 };
                float[] values = { wingTailTipYAdd, wingTailTipYAdd, tailtipY+wingTailTipYAdd, tailtipY+wingTailTipYAdd, beakY+beakTipYAdd, beakY+beakTipYAdd };
                modifyTriangleVerticesBuffer(indices,values);
                --- ODER:
                    setTriangleVertexBufferEntry(7,wingTailTipYAdd);
                    setTriangleVertexBufferEntry(wings2Offset+7,wingTailTipYAdd);
                    setTriangleVertexBufferEntry(tailOffset+4,tailtipY+wingTailTipYAdd);
                    setTriangleVertexBufferEntry(tailOffset+7,tailtipY+wingTailTipYAdd);
                    setTriangleVertexBufferEntry(beakUpperOffset+1,beakY+beakTipYAdd);
                    setTriangleVertexBufferEntry(beakLowerOffset+1,beakY-beakTipYAdd);
                wingTailTipYAdd += stepWingsTail;
                if (wingTailTipYAdd>1.25f||wingTailTipYAdd<-1.25f) stepWingsTail=-stepWingsTail;
                beakTipYAdd += stepBeak;
                if (beakTipYAdd<=0||beakTipYAdd>=0.3) stepBeak=-stepBeak;
            }
        }
    }

    /**
     * Control thread for propeller plane shapes

    private class PropellerPlaneControlThread extends ControlThread {
        final int stepsPerSecond;
        PropellerPlaneControlThread(int stepsPerSecond) {
            this.stepsPerSecond = stepsPerSecond;
        }
        @Override
        public void run() {
            int angle = 0;
            int propellersOffset = 5184;
            int trianglesOffset = 576;
            float[] propellerTriangleCoordinates = new float[216];
            for (int i=trianglesOffset; i<trianglesOffset+24; i++)
                for (int j=0;j<9;j++)
                    propellerTriangleCoordinates[9*(i-trianglesOffset)+j] = triangles[i].getVertexCoordinates()[j];
            ValueProvider evaluator = new ValueProviderRotation(propellerTriangleCoordinates);
            int sleepTime = 100;
            if (stepsPerSecond>0) sleepTime = 1000/stepsPerSecond;
            int[] indices = new int[propellerTriangleCoordinates.length];
            for (int i=0; i<propellerTriangleCoordinates.length;i++)
                indices[i] = propellersOffset+i;
            while (!interrupted()) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (Exception e) {}
                modifyTriangleVerticesBuffer(indices,evaluator.nextCoordinates());
                angle = (angle+10)%360;
            }
        }
    }

    private class PropellerPlaneControlThreadVs2 extends ControlThread {
        final int stepsPerSecond;
        PropellerPlaneControlThreadVs2(int stepsPerSecond) {
            this.stepsPerSecond = stepsPerSecond;
        }
        @Override
        public void run() {
            super.run();
            int angle = 0;
            int propellersOffset = 5184;
            int trianglesOffset = 576;
            float[] propellerTriangleCoordinates = new float[216];
            for (int i=trianglesOffset; i<trianglesOffset+24; i++)
                for (int j=0;j<9;j++)
                    propellerTriangleCoordinates[9*(i-trianglesOffset)+j] = triangles[i].getVertexCoordinates()[j];
            ValueProvider[] evaluators = new ValueProvider[72];
            for (int i=0; i<evaluators.length; i++) {
                float[] param = new float[3];
                param[0] = propellerTriangleCoordinates[3*i];
                param[1] = propellerTriangleCoordinates[3*i+1];
                param[2] = propellerTriangleCoordinates[3*i+2];
                evaluators[i] = new ValueProviderRotation(param);
            }
            int sleepTime = 100;
            if (stepsPerSecond>0) sleepTime = 1000/stepsPerSecond;
            int[] indices = new int[propellerTriangleCoordinates.length];
            for (int i=0; i<propellerTriangleCoordinates.length;i++)
                indices[i] = propellersOffset+i;
            float[] values = new float[propellerTriangleCoordinates.length];
            while (!interrupted()) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (Exception e) {}
                for (int i=0;i<evaluators.length;i++) {
                    float[] nextCoords = evaluators[i].nextCoordinates();
                    for (int j=0; j<3;j++)
                        values[i*3+j] = nextCoords[j];
                }
                modifyTriangleVerticesBuffer(indices,values);
                angle = (angle+10)%360;
            }
        }
    }

    /**
     * EXPERIMENTAL: Control thread for propeller plane shapes

    private class PropellerPlaneControlThreadVs3 extends ControlThread {
        final int stepsPerSecond;
        PropellerPlaneControlThreadVs3(int stepsPerSecond) {
            this.stepsPerSecond = stepsPerSecond;
        }
        @Override
        public void run() {
            super.run();
            int angle = 0;
            int propellersOffset = 5184;
            float[] propellerTriangleCoordinates = new float[216];
            int index=0;
            for (int i=576; i<600; i++) {
                float[] triangleCoords = triangles[i].getVertexCoordinates();
                for (int j=0;j<9;j++)
                    propellerTriangleCoordinates[index++] = triangleCoords[j];
            }
            int sleepTime = 100;
            if (stepsPerSecond>0) sleepTime = 1000/stepsPerSecond;
            while (!interrupted()) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (Exception e) {}
                double sin = Math.sin(angle), cos = Math.cos(angle);
                int[] indices = new int[propellerTriangleCoordinates.length];
                float[] values = new float[propellerTriangleCoordinates.length];
                for (int i=0;i<propellerTriangleCoordinates.length;i++) {
                    indices[i] = propellersOffset+i;
                    switch (i%3) {
                        case 0: values[i] = (float)(propellerTriangleCoordinates[i]*cos-propellerTriangleCoordinates[i+1]*sin); break;
                        case 1: values[i] = (float)(propellerTriangleCoordinates[i-1]*sin+propellerTriangleCoordinates[i]*cos); break;
                        case 2: values[i] = propellerTriangleCoordinates[i]; break;
                    }
                }
                modifyTriangleVerticesBuffer(indices,values);
                angle = (angle+10)%360;
            }
        }
    }
        */

