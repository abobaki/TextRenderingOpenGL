// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 17.3.2022

package de.thkoeln.cvogt.android.opengl_utilities;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Class with <I>String</I> constants for OpenGL ES programs
 * and corresponding constants and methods.
 */

public class GLPlatformCV {

    // TODO Hierhin alles Plattformnahe, auch Füllen der Puffer etc. (siehe Kommentare in OpenGLShape)

    /** Constant specifying the coloring / texturing type: undefined */
    public static final int COLORING_UNDEF = 0;

    /** Constant specifying the coloring / texturing type: uniform color for the whole shape */
    public static final int COLORING_UNIFORM = 1;

    /** Constant specifying the coloring / texturing type: for shapes with triangles of different colors and possibly also with color gradients */
    public static final int COLORING_VARYING = 2;

    /** Constant specifying the coloring / texturing type: textured */
    public static final int COLORING_TEXTURED = 3;

    // uniformly colored shapes
    //
    // TODO Der folgende Code allein funktioniert so nicht. Daher werden bis auf weiteres auch einfarbige Shapes mit dem OpenGL-Code für Farbgradienten gerendert.
    // Zu Lösung die Klasse GLShapeCV entsprechend erweitern. Grundlegende Unterscheidung, die dabei zu treffen ist:
    // 1.) Shapes, die aus mehreren Dreiecken bestehen, von denen jedes "uniformly colored" ist und die alle dieselbe Farbe haben.
    //   > dann könnte dieser Code hier verwendet werden
    //   > das könnte evtl. zu einer Beschleunigung führen (aber sicher ist das nicht)
    //      - Messung (8.9.22 auf Samsung S21) in der App OpenGLAndroid, Klassen ShapeTriangleBasic vs. ShapeTriangleMulticolor vs. ShapeCubeMVPColor:
    //        > drawArrays()-Aufruf bei einem einheitlich gefärbten Dreieck, ohne MVP-Matrix: 20-60 Mikrosekunden
    //        > drawArrays()-Aufruf bei einem Dreieck mit Farbverlauf, ohne MVP-Matrix: 4-8 Milli(!!)sekunden
    //        > ABER(!!) drawArrays()-Aufruf bei Würfel mit 12 Dreiecken mit Farbverläufen, mit MVP-Matrix: 30-60 Mikrosekunden
    //                     [ähnliche Zeitwerte in dieser App OpenGLUtilities für Würfels mit 12 unterschiedlich, aber jeweils einheitlich gefärbten Dreiecken]
    // 2.) Shapes, die aus mehreren Dreiecken bestehen, von denen jedes "uniformly colored" ist, die aber unterschiedliche Farben haben.
    //   > wie sähe entsprechender Code aus??

    public static String vertexShaderUniformColor =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 aPosition;" +   // aPosition liefert dem OpenGL-Rendering die Koordinaten der Eckpunkte des Shapes (Typ vec4 = vierdimensionaler Vektor),
        "void main() {" +
        "  gl_Position = uMVPMatrix * aPosition;" +  // gl_Position ist ein 'defined output', der der Hardware angibt, wo auf dem Display ein Eckpunkt dargestellt werden soll
        "}";

    public static String fragmentShaderUniformColor =
        "precision mediump float;" +
        "uniform vec4 uColor;" +     // vColor liefert dem OpenGL-Rendering die Farbe der Fläche (Typ vec4 = vierdimensionaler Vektor)
        "void main() {" +
        "  gl_FragColor = uColor;" +  // gl_FragColor ist ein 'defined output', der der Hardware die Farbwerte der Eckpunkte angibt
        "}";

    /**
     * OpenGL ES code: vertex shader for colored shapes (shapes with triangles of different colors and possibly color gradients, currently also for shapes with a uniform color).
     */

    public static String vertexShaderVaryingColor =
            "attribute vec4 aPosition;" +   // aPosition liefert dem OpenGL-Rendering die Koordinaten der Eckpunkte des Shapes (Typ vec4 = vierdimensionaler Vektor),
            "attribute vec4 aColor;" +	    // Farbwerte der Eckpunkte. Wertzuweisung wie bei aPosition in der Methode draw().
            "uniform mat4 uMVPMatrix;" +    // MVP-Matrix (Typ mat4 = Matrix mit vier Zeilen und Spalten)
            "varying vec4 vColor;" +		// Speicher zur Weitergabe von Farbwerten an den Fragment Shader
            "void main() {" +
            "  vColor = aColor;" +		    // Weitergabe der Farbwerte an den Fragment Shader
            "  gl_Position = uMVPMatrix * aPosition;" +  // gl_Position ist ein 'defined output', der der Hardware angibt, wo auf dem Display ein Eckpunkt dargestellt werden soll.
            // Hier: Eckpunkte des Würfels, transformiert durch die MVP-Matrix.
            "}";

    /**
     * OpenGL ES code: fragment shader for colored shapes (shapes with triangles of different colors and possibly color gradients, currently also for shapes with a uniform color).
     */

    public static String fragmentShaderVaryingColor =
            "precision mediump float;" +
                    "varying vec4 vColor;" +        // vColor liefert dem OpenGL-Rendering Informationen zur Färbung des Shapes,
                    "void main() {" +
                    "  gl_FragColor = vColor;" +    // gl_FragColor ist ein 'defined output', der der Hardware Farbinformationen liefert.
                    "}";

    /**
     * OpenGL ES code: vertex shader for colored shapes (shapes with triangles of different colors and possibly color gradients, currently also for shapes with a uniform color)
     * with lighting.
     */

    public static String vertexShaderVaryingColorLighting =
                    "attribute vec4 aPosition;" +
                    "attribute vec4 aColor;" +
                    "attribute vec3 aNormal;" +   // Normalvektoren für die Flächen des Würfels, d.h. Vektoren, die auf den Flächen des Würfels senkrecht stehen.
                    // Sie werden benötigt, um den Winkel der Lichtquelle relativ zur Fläche zu berechnen und damit die Stärke des reflektierten Lichts.
                    "uniform mat4 uVMatrix;"  +   // View-Matrix zur Transformation der Licht-Koordinaten aus dem World Space in den Eye Space
                    "uniform mat4 uMVMatrix;"  +  // ModelView-Matrix, also MVP-Matrix ohne Projektionsmatrix
                    "uniform mat4 uMVPMatrix;" +  // ModelViewProjection(MVP)-Matrix
                    "uniform vec3 uPointLightPos;"  +  // Position der Point-Light-Quelle im World Space
                    "uniform vec3 uDirectionalLightVector;"  +  // Vektor des "Directional Lights" (entsprechend der Richtung einer sehr weit entfernten Lichtquelle, z.B. der Sonne) im World Space
                    "uniform float uRelativePointLightShare;" + // relativer Anteil des Point Lights an der Gesamtheit von Point Light und Directional Light
                    "uniform float uAmbientLight;" +  // zusätzlicher Beitrag des Ambient Light
                    "varying vec4 vColor;" +  // darzustellende Farben, die sich (siehe unten) aus Ausgangsfarben und Beleuchtung ergibt
                    "void main() {" +
                    "  vec3 pointLightPosEyeSpace = vec3(uVMatrix * vec4(uPointLightPos,1.0));" +  // Transformation der Koordinaten der Point-Light-Quelle in den Eye Space
                    "  vec3 directionalLightVectorEyeSpace = vec3(uVMatrix * vec4(uDirectionalLightVector,0.0));" +  // Transformation des Directional-Light-Vectors in den Eye Space
                    "  vec3 modelViewVertex = vec3(uMVMatrix * aPosition);" +  // Transformation der Würfel-Koordinaten in den Eye Space
                    "  vec3 modelViewNormal = normalize(vec3(uMVMatrix * vec4(aNormal, 0.0)));" +  // Transformation der Normalvektor-Koordinaten in den Eye Space
                    "  float pointLightDistance = length(pointLightPosEyeSpace - modelViewVertex);" +  // Abstand zwischen der punktuellen Lichtquelle und Punkt auf der Würfelfläche
                    "  vec3 pointLightVector = normalize(pointLightPosEyeSpace - modelViewVertex);  " +  // normalisierter Richtungsvektor von der punktuellen Lichtquelle zu Punkt auf der Würfelfläche
                    "  float pointLightDiffuse = max(dot(modelViewNormal, pointLightVector), 0.005);  " +  // Skalarprodukt von 1.) Normalvektor auf der Würfeloberfläche und 2.) Richtungsvektor von der punktuellen Lichtquelle,
                    // bestimmt den Winkel zwischen beiden Vektoren (genauer: dessen Cosinus) und damit die Stärke des reflektierten Lichts.
                    "  pointLightDiffuse = pointLightDiffuse * (1.0 / (1.0 + (0.001 * pointLightDistance * pointLightDistance)));" +  // Dämpfung des Lichts abhängig vom Abstand der Lichtquelle
                    "  float directionalLightDiffuse = max(dot(modelViewNormal, directionalLightVectorEyeSpace), 0.01);  " +  // entsprechendes Skalarprodukt (vgl. pointLightDiffuse)
                    "  vColor = aColor * (uRelativePointLightShare * pointLightDiffuse + (1.0-uRelativePointLightShare) * directionalLightDiffuse);" +
                    // Berechnung der darzustellenden Farbe aus der Ausgangsfarbe und den relativen Anteilen von Point Light und Directional Light
                    "  vColor = min(vColor+uAmbientLight,1.0);" + // Addition des Ambient Light
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    /**
     * OpenGL ES code: vertex shader for textured shapes.
     */

    public static String vertexShaderTextured =    // Vertex Shader: Informationen über die Eckpunkte des Dreiecks
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec2 aTexCoord;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "  vTexCoord = aTexCoord;" +
                    "}";

     // TODO vertex shader for textured triangles with lighting

    /**
     * OpenGL ES code: fragment shader for textured shapes.
     */

    public static String fragmentShaderTextured =   // Fragment Shader: Abbildung von Pixeln des Textur-Bilds auf die einzelnen "Fragments" = Pixel der Dreiecksfläche
            "precision mediump float;" +
                    "varying vec2 vTexCoord;" +
                    "uniform sampler2D sTexture;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D( sTexture, vTexCoord );" +
                    "}";

    /**
     * Auxiliary method to compile shader code
     */

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        /*
        // debug output
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0]==0)
            Log.v("GLDEMO",">>> Compile error: "+GLES20.glGetShaderInfoLog(shader));
          else
            Log.v("GLDEMO",">>> Compilation sucessful");
        */
        return shader;
    }

}
