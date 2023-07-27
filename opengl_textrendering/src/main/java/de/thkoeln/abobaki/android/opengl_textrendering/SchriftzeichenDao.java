package de.thkoeln.abobaki.android.opengl_textrendering;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Data Access Object Interface (DAO) für Schriftzeichen-Objekte.
 * <p>
 * SchriftzeichenDao ist ein Interface, das die Methoden zum Zugriff auf die Schriftzeichen-Datenbank definiert.
 * Die Implementierung dieses Interfaces wird vom Compiler generiert.
 *
 * @see Schriftzeichen
 */
@Dao
public interface SchriftzeichenDao {
    /**
     * Durch diesen Methoden wird ein Schriftzeichen-Objekt in die Datenbank einfügt.
     * @param schriftzeichen ist ein Objekt von Entity-Klasse Schriftzeichen.
     */
    @Insert
    void insert(Schriftzeichen schriftzeichen);

    /**
     * Abfrage-Methode, die die GLTriangleCV-Objekte von Triangles-Spalte mithilfe des angegebenen Schriftzeichennamens zurückgibt.
     *
     * @param name Der Name des Schriftzeichens, nach dem gesucht wird.
     * @return Array von GLTriangleCV-Objekten als String.
     */
    @Query("SELECT dreiecke FROM Schriftzeichen WHERE modellName = :name")
    String findDreieckeByName(String name);

    @Query("SELECT modelBreite FROM Schriftzeichen WHERE modellName = :name")
    float findBreiteByName(String name);

    @Query("SELECT modelHoehe FROM Schriftzeichen WHERE modellName = :name")
    float findHoeheByName(String name);

    @Query("SELECT vertices FROM Schriftzeichen WHERE modellName = :name")
    int findAnzahlVonVerticesByName(String name);

    @Query("SELECT EXISTS(SELECT 1 FROM Schriftzeichen WHERE modellName=:name)")
    boolean isNameExists(String name);

}
