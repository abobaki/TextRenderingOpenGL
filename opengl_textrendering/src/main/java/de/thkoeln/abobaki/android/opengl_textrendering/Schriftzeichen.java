package de.thkoeln.abobaki.android.opengl_textrendering;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import de.thkoeln.cvogt.android.opengl_utilities.GLTriangleCV;

/**
 * Eine Entity-Klasse, die eine Tabelle in der Datenbank repräsentiert.
 * Die Attribute dieser Klasse entsprechen Spalten in der Tabelle und jedes Objekt repräsentiert eine Zeile in der Tabelle.
 */

@Entity
public class Schriftzeichen {

    /**
     * Der Primärschlüssel der Tabelle, der automatisch generiert wird
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Die GLTriangleCV-Objekte, die das Schriftzeichen darstellen.
     */
    @ColumnInfo(name = "dreiecke")
    public GLTriangleCV[] dreiecke;

    /**
     * Der Name des Schriftzeichen
     */
    @ColumnInfo(name = "modellName")
    public String modellName;

    /**
     * Die Bereite des Schriftzeichens.
     */
    @ColumnInfo(name = "modelBreite")
    public float modelBreite;

    /**
     * Die Höhe des Schriftzeichens.
     */
    @ColumnInfo(name = "modelHoehe")
    public float modelHoehe;

    /**
     * Die Anzahl von Vertices des Schriftzeichens.
     */
    @ColumnInfo(name = "vertices")
    public int anzahlVertices;

    /**
     * Der Konstruktor der Klasse
     */
    public Schriftzeichen(GLTriangleCV[] dreiecke, float modelBreite, float modelHoehe, String modellName, int anzahlVertices) {
        this.dreiecke = dreiecke;
        this.modelBreite = modelBreite;
        this.modelHoehe = modelHoehe;
        this.modellName = modellName;
        this.anzahlVertices = anzahlVertices;
    }

}
