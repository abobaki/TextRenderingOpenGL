package de.thkoeln.abobaki.android.opengl_textrendering;

import androidx.room.TypeConverter;
import com.google.gson.Gson;

import de.thkoeln.cvogt.android.opengl_utilities.GLTriangleCV;

/**
 * Konverter ist eine Utility-Klasse, die TypeConveter-Methoden hat.
 * Diese Methoden konvertieren GLTriangleCV-Objekte in JSON-Strings und umgekehrt, damit sie
 * von Room zur Speicherung in der Datenbank verwendet werden können.
 */
public class Konverter {

    private Konverter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Diese Methode wird durch genertieten Klassen benutzt, wenn ein Array von GLTriangleCV-Objekten in Datenbank gespeichert wird.
     * Diese Methode konvertiert die GLTriangleCV zu den JSON-String durch Google Gson.
     * @param dreiecke Ein Array von GLTriangleCV-Objekten
     * @return Ein JSON-String, der das Array von GLTriangleCV-Objekten enthält
     */
    @TypeConverter
    public static String glTriangleCVZuString(GLTriangleCV[] dreiecke) {
        return new Gson().toJson(dreiecke);
    }

    /**
     * Diese Methode wird benutzt, wenn die JSON-String zu ein Array von GLTriangleCV-Objekten konvertiert wird.
     * @param wert Ein JSON-String, der das Array von GLTriangleCV-Objekten enthält
     * @return Ein Array von GLTriangleCV-Objekten
     */
    @TypeConverter
    public static GLTriangleCV[] stringZuGLTriangleCV(String wert) {
        return new Gson().fromJson(wert, GLTriangleCV[].class) ;
    }
}