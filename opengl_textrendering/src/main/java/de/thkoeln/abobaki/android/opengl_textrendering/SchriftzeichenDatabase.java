package de.thkoeln.abobaki.android.opengl_textrendering;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Datenbank-Klasse
 * <p>
 * SchriftzeichenDatabase verwaltet die Datenbank für Schriftzeichen-Objekte und legt die Konfiguration der Datenbank fest.
 *
 * @see Schriftzeichen
 * @see SchriftzeichenDao
 * @see Konverter
 */
@Database(entities = {Schriftzeichen.class}, version = 1)
@TypeConverters({Konverter.class})
public abstract class SchriftzeichenDatabase extends RoomDatabase {
    public abstract SchriftzeichenDao schriftzeichendao();
}
