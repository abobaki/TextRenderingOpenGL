package de.thkoeln.abobaki.android.opengl_textrendering;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.thkoeln.cvogt.android.opengl_utilities.GLAnimatorFactoryCV_DEPRAC;
import de.thkoeln.cvogt.android.opengl_utilities.GLShapeCV;
import de.thkoeln.cvogt.android.opengl_utilities.GLTriangleCV;
import de.thkoeln.cvogt.android.opengl_utilities.GraphicsUtilsCV;

/**
 * SchriftzeichenUtility ist ein Utility-Klasse mit statischen Methoden zu den verschiedene Hilfsmehoden
 */
public final class SchriftzeichenUtility {

    private static SchriftzeichenDao schriftzeichenDao;
    private static Toast letzterToast;

    /**
     * Um Verzögerungen wie den Datenbankzugriff während des Aufrufs der Methode "textdarstellen()" zu reduzieren,
     * werden diese Attribute (breite - hoehe) verwendet, um die Informationen im Voraus zu speichern
     * und dann von der Methode "textdarstellen()" abzurufen.
     */
    private static float[] breite;
    private static float[] hoehe;

    private SchriftzeichenUtility() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Die Methode initialisierung wird jedes Mal aufgerufen, wenn die App startet,
     * um sicherzustellen, dass alle vorhandenen OBJ-Dateien in der Datenbank gespeichert sind,
     * indem der Dateiname der "OBJ-Datei" mit dem in der Datenbank vorhandenen Namen abgeglichen wird.
     * Außerdem erfolgt eine Konfiguration der Datenbank, indem die Attribute schriftzei-chenDao initialisiert wird.
     * @param context Context der Anwendung
     */
    public static void initialisierung(Context context) {
        AssetManager assetManager = context.getAssets();
        String database = "SchriftzeichenDatabase";
        SchriftzeichenDatabase schriftzeichenDatabase = Room.databaseBuilder(context.getApplicationContext(), SchriftzeichenDatabase.class, database)
                .allowMainThreadQueries().fallbackToDestructiveMigrationOnDowngrade().build();
        schriftzeichenDao = schriftzeichenDatabase.schriftzeichendao();
        try {
            String[] files = assetManager.list("");
            // Jede OBJ-Datei im Assets-Ordner muss mit einem vorhandenen Namen in der Datenbank übereinstimmen.
            // Falls eine Übereinstimmung auftritt, wird die Datei nicht in der Datenbank gespeichert.
            // Falls keine Übereinstimmung auftritt, wird die Datei vom Parser gelesen und in der Datenbank gespeichert.
            for (String dateiName : files)
                if (dateiName.endsWith(".obj") && !(schriftzeichenDao.isNameExists(SchriftzeichenUtility.returnFileName(dateiName)))) {
                    schriftzeichenDao.insert(SchriftzeichenUtility.objParser(context, dateiName));
                }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Diese Methode liest OBJ-Dateien und dann extrahiert darin enthaltenen 3D-Modelldaten
     * @param context   Context der Anwendung
     * @param dateiName Name der ausgewählte Datei
     * @return ein objekte von Entity-Klasse Schriftzeichen
     */
    public static Schriftzeichen objParser(Context context, String dateiName) {

        int numOfVertices = 0;
        int positionIndex = 0;
        int numTriangle = 0;

        //Array vom Typ float, das aus drei Zeilen und zwei Spalten besteht.
        //Jede Zeile des Arrays repräsentiert eine Achse des kartesischen Koordinatensystems (x, y und z)
        //Jede Spalte speichert die Koordinaten des größten und kleinsten Punktes entlang dieser Achse
        float[][] begrenzungsrahmen = new float[3][2];

        //Die Vertices des Dreiecks.
        float[][] verticesTriangle = new float[3][3];

        ArrayList<Float> verticesZwischenspeicher = new ArrayList<>();
        ArrayList<String> faces = new ArrayList<>();

        try {
            InputStreamReader in = new InputStreamReader(context.getAssets().open(dateiName));
            BufferedReader reader = new BufferedReader(in);
            String line;

            //jede zeile von OBJ-Datei wird gelesen
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if ("v".equals(parts[0])) {
                    numOfVertices++;
                    verticesZwischenspeicher.add(Float.valueOf(parts[1]));
                    verticesZwischenspeicher.add(Float.valueOf(parts[2]));
                    verticesZwischenspeicher.add(Float.valueOf(parts[3]));
                } else if ("f".equals(parts[0])) {
                    faces.add(parts[1]);
                    faces.add(parts[2]);
                    faces.add(parts[3]);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("DEMO_AB", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("DEMO_AB", "Error reading file: " + e.getMessage());
        }

        final float[] zentrum = new float[3];
        GLTriangleCV[] triangles = new GLTriangleCV[faces.size() / 3];
        for (int j = 0; j < 2; j++) {
            begrenzungsrahmen[0][j] = verticesZwischenspeicher.get(0);
            begrenzungsrahmen[1][j] = verticesZwischenspeicher.get(1);
            begrenzungsrahmen[2][j] = verticesZwischenspeicher.get(2);
        }

        for (String face : faces) {
            String[] parts = face.split("/");
            int index = 3 * (Short.parseShort(parts[0]) - 1);
            verticesTriangle[positionIndex][0] = verticesZwischenspeicher.get(index++);
            verticesTriangle[positionIndex][1] = verticesZwischenspeicher.get(index++);
            verticesTriangle[positionIndex][2] = verticesZwischenspeicher.get(index);

            if (begrenzungsrahmen[0][0] > verticesTriangle[positionIndex][0])
                begrenzungsrahmen[0][0] = verticesTriangle[positionIndex][0];

            if (begrenzungsrahmen[0][1] < verticesTriangle[positionIndex][0])
                begrenzungsrahmen[0][1] = verticesTriangle[positionIndex][0];

            if (begrenzungsrahmen[1][0] > verticesTriangle[positionIndex][1])
                begrenzungsrahmen[1][0] = verticesTriangle[positionIndex][1];

            if (begrenzungsrahmen[1][1] < verticesTriangle[positionIndex][1])
                begrenzungsrahmen[1][1] = verticesTriangle[positionIndex][1];

            if (begrenzungsrahmen[2][0] > verticesTriangle[positionIndex][2])
                begrenzungsrahmen[2][0] = verticesTriangle[positionIndex][2];

            if (begrenzungsrahmen[2][1] < verticesTriangle[positionIndex][2])
                begrenzungsrahmen[2][1] = verticesTriangle[positionIndex][2];

            positionIndex++;
            if (positionIndex % 3 == 0) {
                triangles[numTriangle] = new GLTriangleCV("TriangleObj" + numTriangle, verticesTriangle);
                numTriangle++;
                positionIndex = 0;
            }
        }

        // x-, y- und z-Koordinaten der Zentrum berechnet
        // indem jeweils der  Durchschnitt der größten und kleinsten Werte des Begrenzungsrahmens
        zentrum[0] = (begrenzungsrahmen[0][0] + begrenzungsrahmen[0][1]) / 2;
        zentrum[1] = (begrenzungsrahmen[1][0] + begrenzungsrahmen[1][1]) / 2;
        zentrum[2] = (begrenzungsrahmen[2][0] + begrenzungsrahmen[2][1]) / 2;

        //ändert den Mittelpunkt der Form zu den Ursprung (0,0,0) seines lokalen Koordinatensystems,
        //indem alle Dreiecke des Objekts in Richtung des Zentrums verschoben werden.
        for (GLTriangleCV triangle : triangles) {
            triangle.translate(-zentrum[0], -zentrum[1], -zentrum[2]);
        }

        // Die Breite und die Höhe des Modells (der 3D-Form) werden berechnet,
        // indem die Differenz zwischen dem größten und dem kleinsten x-Wert des Begrenzungsrahmens für die Breite
        // und die Differenz zwischen dem größten und dem kleinsten y-Wert des Begrenzungsrahmens für die Höhe berechnet wird.
        final float schriftzeichenBreite = Math.abs(begrenzungsrahmen[0][1] - begrenzungsrahmen[0][0]);
        final float schriftzeichenHoehe = Math.abs(begrenzungsrahmen[1][1] - begrenzungsrahmen[1][0]);

        String fileName = returnFileName(dateiName);

        return new Schriftzeichen(triangles, schriftzeichenBreite, schriftzeichenHoehe, fileName, numOfVertices);
    }

    /**
     * Diese Methode konvertiert den eingegebenen Text in ein Array von GLShapeCV-Objekten.
     * Darüber hinaus werden die Länge und Breite der 3D-Schriftzeichen-Modelle in statischen Attributen gespeichert.
     * @param text Der eingegebene Text.
     */
    private static GLShapeCV[] erzeugeSchriftzeichen(String text) {
        String eingabeOhneLeerzeichen = leerzeichenEntfernen(text);
        int len = eingabeOhneLeerzeichen.length();
        GLShapeCV[] schriftzeichen = new GLShapeCV[len];
        breite = new float[len];
        hoehe = new float[len];
        aeussereSchleife:
        for (int i = 0; i < len; i++) {
            String zeichen = charAt(eingabeOhneLeerzeichen, i);
            breite[i] = schriftzeichenDao.findBreiteByName(zeichen);
            if ("g".equals(zeichen) || "j".equals(zeichen) || "p".equals(zeichen) || "q".equals(zeichen) || "y".equals(zeichen))
                hoehe[i] = schriftzeichenDao.findHoeheByName(zeichen) / 4;
            else
                hoehe[i] = schriftzeichenDao.findHoeheByName(zeichen) / 2;
            //Wenn ein Zeichen wiederholt wird, anstatt aus der Datenbank abgerufen zu werden, wird es mit der copy-methode kopiert.
            for (int j = 0; j < i; j++) {
                if (Objects.equals(getLinkesWort(schriftzeichen[j].getId()), zeichen)) {
                    schriftzeichen[i] = schriftzeichen[j].copy(zeichen + "," + i);
                    continue aeussereSchleife;
                }
            }
            schriftzeichen[i] = new GLShapeCV(zeichen + "," + i, Konverter.stringZuGLTriangleCV(schriftzeichenDao.findDreieckeByName(zeichen)));
        }
        return schriftzeichen;
    }

    /**
     * Diese Methode ist für die Darstellung und Anpassung als TextEditor von schriftzeichen zuständig.
     * @param text eingegebene Text
     * @param textAnimation Wenn true ist, wird eine Animationssequenz (die 3D-Modelle aus verschiedenen Orten kommen)
     *                      gestartet. Wenn false ist, wird der Text ohne diese Animation sofort erstellt.
     * @return Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     */
    public static GLShapeCV[] textDarstellen(String text, boolean textAnimation) {
        GLShapeCV[] glShapeCVS = erzeugeSchriftzeichen(text);
        textDarstellen(glShapeCVS, text, 6.5f, 0, textAnimation);
        return glShapeCVS;
    }

    /**
     * Diese Methode ist für die Darstellung und Anpassung als TextEditor von schriftzeichen zuständig.
     * @param text eingegebene Text
     * @param skalierungsfaktor  Die Ziel-Skalierungsfaktor der Animation.
     * @param abstandsanpassung  Abstand zwischen Wörtern und Zeichen
     * @param textAnimation Wenn true ist, wird eine Animationssequenz (die 3D-Modelle aus verschiedenen Orten kommen)
     *                      gestartet. Wenn false ist, wird der Text ohne diese Animation sofort erstellt.
     * @return Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     */
    public static GLShapeCV[] textDarstellen(String text, float skalierungsfaktor, float abstandsanpassung, boolean textAnimation) {
        GLShapeCV[] glShapeCVS = erzeugeSchriftzeichen(text);
        textDarstellen(glShapeCVS, text, skalierungsfaktor, abstandsanpassung, textAnimation);
        return glShapeCVS;
    }

    /**
     * Diese Methode setzt die entsprechene Skalierung, Animation usw. für jedes Zeichen und passt diesen Text als TextEditor von schriftzeichen an.
     * @param glshapeCVs        Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     * @param text              eingegebene Text
     * @param skalierungsfaktor Die Ziel-Skalierungsfaktor der Animation.
     * @param abstandsanpassung Abstand zwischen Wörtern und Zeichen
     * @param textAnimation     Wenn true ist, wird eine Animationssequenz (die 3D-Modelle aus verschiedenen Orten kommen)
     *                          gestartet. Wenn false ist, wird der Text ohne diese Animation sofort erstellt.
     */
    public static void textDarstellen(GLShapeCV[] glshapeCVs, String text, float skalierungsfaktor, float abstandsanpassung, boolean textAnimation) {

        //Grenze der Zeile von Rechts
        float zeileBegrenzungVonRechts;
        //Position eines Zeichens
        float zeichenposition = -2.6f;
        //wortabstand für den Abstand zwischen Wörtern
        float wortabstand = (skalierungsfaktor * 0.02f) + abstandsanpassung;
        //zeichenabstand für den Abstand zwischen Zeichen
        float zeichenabstand = (skalierungsfaktor * 0.009f) + abstandsanpassung;
        //zeilenabstand
        float zeilenabstand = (skalierungsfaktor * 0.059f);

        int anzahlZeilen = 0;
        int anzahlLeerzeichen = 0;
        boolean neueZeileErforderlich = false;
        List<Integer> leerzeichenIndex = letzteZeichenIndices(text);

        for (int i = 0; i < glshapeCVs.length; i++) {
            int j = i;
            //Hier wird überprüft, ob das Zeichen nach der Leerzeichen befindet.
            if (leerzeichenIndex.get(anzahlLeerzeichen) < i) {
                zeichenposition += wortabstand;
                zeileBegrenzungVonRechts = zeichenposition;
                anzahlLeerzeichen++;

                //Die Länge des nächsten Worts wird komplett berechnet und dann wird zu diesen variable
                // zeileBegrenzungVonRechts hinzufügt.
                while (leerzeichenIndex.get(anzahlLeerzeichen) >= j && j != 0) {
                    zeileBegrenzungVonRechts += zeichenabstand + (((breite[j - 1] * skalierungsfaktor) + (breite[j] * skalierungsfaktor)) / 2);
                    j++;
                }

                //Hier wird überprüft, ob das Wort die rechte Grenze überschritten hat
                //wenn ja, wird dann ein neue Zeile hinzufügt und die zeichenPosition zurückgesetzt
                if (zeileBegrenzungVonRechts > 2.6) {
                    zeichenposition = -2.6f;
                    anzahlZeilen++;
                    neueZeileErforderlich = true;
                }
            }

            if (i != 0 && !neueZeileErforderlich) {
                zeichenposition += zeichenabstand + (((breite[i - 1] * skalierungsfaktor) + (breite[i] * skalierungsfaktor)) / 2);
            } else
                neueZeileErforderlich = false;

            //Wenn true ist, wird eine Animationssequenz gestartet, wenn false ist, wird der Text ohne diese Animation sofort erstellt
            if (textAnimation) {
                Random rand = new Random();
                glshapeCVs[i].setTrans(new float[]{(rand.nextFloat() * 6) - 3, (rand.nextFloat() * 12) - 6, (rand.nextFloat() * 6) - 3});
                glshapeCVs[i].setScale(skalierungsfaktor);
                glshapeCVs[i].addAnimator(GLAnimatorFactoryCV_DEPRAC.makeAnimatorTransBezier(new float[]{0, 0, 0},
                        new float[]{ zeichenposition, (hoehe[i] * skalierungsfaktor) - (anzahlZeilen * zeilenabstand), 0}, -1, 6000, 3500));
            } else {
                glshapeCVs[i].setTransX(zeichenposition);
                glshapeCVs[i].setTransY((hoehe[i] * skalierungsfaktor) - (anzahlZeilen * zeilenabstand));
                glshapeCVs[i].setScale(skalierungsfaktor);
            }

        }
    }

    /**
     * Diese Methode ist dafür verantwortlich, dass Zeichen vertauschen, wenn der Benutzer die Berührung in eine bestimmte Richtung verwendet
     * @param glShapeCVs              Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     * @param text                    eingegebene Text
     * @param beruehrteSchriftzeichen Schriftzeichen, die vom Benutzer berührt wurde
     * @param richtung                Richtung, die vom Benutzer augewählt wurde
     * @return der Text wird als String zurückgegeben, nachdem die Zeichen vertauscht werden.
     */
    public static String tauscheSchriftzeichen(GLShapeCV[] glShapeCVs, String text, GLShapeCV beruehrteSchriftzeichen, String richtung) {
        int indexBeruhrteZeichen = SchriftzeichenUtility.indexSchriftzeichen(glShapeCVs, beruehrteSchriftzeichen);
        int indexGezielteZeichen;

        if (Objects.equals(richtung, "left") && (indexBeruhrteZeichen == 0)) {
            indexGezielteZeichen = glShapeCVs.length - 1;
        } else if (Objects.equals(richtung, "right") && (glShapeCVs.length - 1) == indexBeruhrteZeichen) {
            indexGezielteZeichen = 0;
        } else if (Objects.equals(richtung, "right")) {
            indexGezielteZeichen = indexBeruhrteZeichen + 1;
        } else if (Objects.equals(richtung, "left")) {
            indexGezielteZeichen = indexBeruhrteZeichen - 1;
        } else return null;

        text = text.trim();
        List<Integer> whitespaceIndices = SchriftzeichenUtility.indizesLeerzeichen(text);
        text = SchriftzeichenUtility.leerzeichenEntfernen(text);
        char[] chars = text.toCharArray();

        char tmp1 = chars[indexBeruhrteZeichen];
        chars[indexBeruhrteZeichen] = chars[indexGezielteZeichen];
        chars[indexGezielteZeichen] = tmp1;

        float tmp2 = hoehe[indexBeruhrteZeichen];
        hoehe[indexBeruhrteZeichen] = hoehe[indexGezielteZeichen];
        hoehe[indexGezielteZeichen] = tmp2;

        float tmp3 = breite[indexBeruhrteZeichen];
        breite[indexBeruhrteZeichen] = breite[indexGezielteZeichen];
        breite[indexGezielteZeichen] = tmp3;

        GLShapeCV tmp4 = glShapeCVs[indexBeruhrteZeichen];
        glShapeCVs[indexBeruhrteZeichen] = glShapeCVs[indexGezielteZeichen];
        glShapeCVs[indexGezielteZeichen] = tmp4;

        text = new String(chars);
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < whitespaceIndices.size(); i++) {
            int index = whitespaceIndices.get(i);
            sb.insert(index, " ");
        }
        text = sb.toString();
        return text;
    }

    /**
     * Die Methode getRichtung erhält zwei Punkte als Eingabe: den ersten Punkt, der berührt wird,
     * und den letzten Punkt. Anschließend wird der Winkel zwischen diesen beiden Punkten ermittelt.
     * Basierend auf diesem Winkel wird die entsprechende Richtung bestimmt.
     */
    public static String getRichtung(float x1, float y1, float x2, float y2) {
        //Weitere Informationen über (https://stackoverflow.com/a/26387629/11434585)
        double rad = Math.atan2(y1 - y2, x2 - x1) + Math.PI;
        double winkel = (rad * 180 / Math.PI + 180) % 360;
        if (winkel >= 45 && winkel < 135) {
            return "up";
        } else if (winkel >= 0 && winkel < 45 || winkel >= 315 && winkel < 360) {
            return "right";
        } else if (winkel >= 225 && winkel < 315) {
            return "down";
        } else {
            return "left";
        }
    }

    /**
     * Die Methode toastAnzeigen wird verwendet, um Benachrichtigungen mittels eines XML-Layouts anzuzeigen.
     * Dies bietet mehr Vorteile im Vergleich zur Verwendung von Toast.makeText. Wenn der Benutzer zwei Aktionen ausführt,
     * die das Erscheinen von zwei Toast-Nachrichten ermöglichen, wird der zweite Toast nicht warten,
     * bis der erste vollstän-dig angezeigt wurde. Stattdessen wird der erste Toast unterbrochen und der zweite wird angezeigt.
     * Diese Methode ermöglicht es, mehr als zwei Zeilen Text in der Toast-Nachricht anzuzeigen.
     * @param context Context der Anwendung
     * @param message Der Text, der angezeigt werden soll
     * @param duration Die Dauer, die der Toast auf dem Bildschirm bleiben soll.
     */
    public static void toastAnzeigen(Context context, String message, int duration) {
        // Weitere Informationen über https://stackoverflow.com/a/35173068 https://stackoverflow.com/a/31301563
        if (letzterToast != null) {
            letzterToast.cancel();
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.toast_popup, null);
        TextView text = layout.findViewById(R.id.toast_text);
        Typeface typeface = Typeface.create("Arial", Typeface.BOLD);
        text.setTypeface(typeface);
        text.setBackgroundColor(Color.BLACK);
        text.setTextColor(Color.WHITE);
        text.setText(message);
        text.setTextSize(21);
        letzterToast = new Toast(context);
        letzterToast.setView(layout);
        letzterToast.setDuration(duration);
        if (Objects.equals(message, "Ungültiger Index!") || Objects.equals(message, "Bitte geben Sie einen Index größer als 0 ein")) {
            letzterToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, letzterToast.getXOffset(), letzterToast.getYOffset() * 5);
        }
        letzterToast.show();
    }

    /**
     * ändert die Farbe von Schriftzeichen mithilfe von setTrianglesUniformColor-Methode in GLShapeCV-Klasse
     * @param glShapeCVS  Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     * @param colorString die ausgewählte Farbe
     */
    public static void farbeAendern(GLShapeCV[] glShapeCVS, String colorString) {
        for (GLShapeCV glShapeCV : glShapeCVS) {
            glShapeCV.setTrianglesUniformColor(getFarbe(colorString));
        }
    }

    /**
     * Der Dateiname wird durch einen neuen Namen ersetzt, der dem entsprechenden Zeichen entspricht.
     * wie Z.B QuestionMark = ?
     * @param dateiName Name der OBJ-Datei
     * @return ein einziges Zeichen, das ein Dateiname ausdrückt
     */
    public static String returnFileName(String dateiName) {
        String schriftzeichenName = null;
        if (dateiName.startsWith("small_"))
            schriftzeichenName = dateiName.substring(6, dateiName.length() - 4);
        else if (dateiName.equals("Colon.obj")) schriftzeichenName = ":";
        else if (dateiName.equals("ForwardSlash.obj")) schriftzeichenName = "/";
        else if (dateiName.equals("GreaterThan.obj")) schriftzeichenName = ">";
        else if (dateiName.equals("SmallerThan.obj")) schriftzeichenName = "<";
        else if (dateiName.equals("QuestionMark.obj")) schriftzeichenName = "?";
        else if (dateiName.equals("Dot.obj")) schriftzeichenName = ".";
        else if (dateiName.equals("asterisk.obj")) schriftzeichenName = "*";
        else if (dateiName.equals("doubleQuote.obj")) schriftzeichenName = "\"";
        else if (dateiName.endsWith(".obj"))
            schriftzeichenName = dateiName.substring(0, dateiName.length() - 4);
        return schriftzeichenName;
    }

    /**
     * entfernt ein beruehrteSchriftzeichen aus einem Array von GLShapeCV-Objekten und gibt das aktualisierte Array zurück.
     * @param glShapeCVS              glShapeCVs Die 3D-Schriftzeichen-Modellen als Array von GLShapeCV-Objekte
     * @param beruehrteSchriftzeichen Schriftzeichen, die vom Benutzer berührt wurde
     * @return aktualisierte Array
     */
    public static GLShapeCV[] schriftzeichenEntfernen(GLShapeCV[] glShapeCVS, GLShapeCV beruehrteSchriftzeichen) {
        int index = indexSchriftzeichen(glShapeCVS, beruehrteSchriftzeichen);
        if (index == -1) {
            return glShapeCVS;
        } else {
            GLShapeCV[] newShapes = new GLShapeCV[glShapeCVS.length - 1];
            int destIndex = 0;
            for (int srcIndex = 0; srcIndex < glShapeCVS.length; srcIndex++) {
                if (srcIndex != index) {
                    newShapes[destIndex++] = glShapeCVS[srcIndex];
                }
            }
            return newShapes;
        }
    }

    /**
     * gibt den Index des einzigeShape im Array zurück
     * @param glShapeCVS   Array von GLShapeCV
     * @param einzigeShape ausgewählte GLShapeCV
     * @return Index des ausgewählte GLShapeCV
     */
    public static int indexSchriftzeichen(GLShapeCV[] glShapeCVS, GLShapeCV einzigeShape) {
        for (int i = 0; i < glShapeCVS.length; i++) {
            if (glShapeCVS[i].equals(einzigeShape)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Diese Methode gibt die Indizes der Leerzeichen im Eingabestring zurück
     * Z.b ("ABC DEF GHI J"): -> = [3, 7, 11]
     */
    private static ArrayList<Integer> indizesLeerzeichen(String text) {
        // https://stackoverflow.com/a/4731081
        ArrayList<Integer> indizes = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\S\\s"); // match any non-whitespace character followed by whitespace
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            indizes.add(matcher.end() - 1); // add the index of the whitespace character
        }
        return indizes;
    }

    /**
     * Diese Methode gibt die Indices des letzten Zeichens für jedes zusammenhängende Zeichenfolge zurück, ohne die Indices von Leerzeichen zu berücksichtigen.
     * Z.b ("ABC DEF % GHI"): -> = [2, 5, 6, 8]
     */
    public static List<Integer> letzteZeichenIndices(String text) {
        List<Integer> charCounts = new ArrayList<>();
        int charCount = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ') {
                charCounts.add(i - charCount);
                charCount = i + 1;
            }
        }
        charCounts.add(text.length() - charCount);
        for (int i = 1; i < charCounts.size(); i++) {
            charCounts.set(i, charCounts.get(i) + charCounts.get(i - 1));
        }
        //make every Value -1
        for (int i = 0; i < charCounts.size(); i++) {
            int newValue = charCounts.get(i) - 1;
            charCounts.set(i, newValue);
        }
        return charCounts;
    }

    public static String getLinkesWort(String eingabe) {
        int kommaIndex = eingabe.indexOf(',');
        return eingabe.substring(0, kommaIndex);
    }

    public static String charAt(String input, int index) {
        if (index >= 0 && index < input.length()) {
            return input.substring(index, index + 1);
        }
        return "Index out of bounds";
    }

    private static float[] getFarbe(String farbeAlsString) {
        switch (farbeAlsString) {
            case "Weiß":
                return GraphicsUtilsCV.white;
            case "Schwarz":
                return GraphicsUtilsCV.black;
            case "Gelb":
                return GraphicsUtilsCV.yellow;
            case "Hellgelb":
                return GraphicsUtilsCV.lightyellow;
            case "Orange":
                return GraphicsUtilsCV.orange;
            case "Rot":
                return GraphicsUtilsCV.red;
            case "Hellrot":
                return GraphicsUtilsCV.lightred;
            case "Blau":
                return GraphicsUtilsCV.blue;
            case "Hellblau":
                return GraphicsUtilsCV.lightblue;
            case "Grün":
                return GraphicsUtilsCV.green;
            case "Hellgrün":
                return GraphicsUtilsCV.lightgreen;
            case "Dunkelgrün":
                return GraphicsUtilsCV.darkgreen;
            case "Türkis":
                return GraphicsUtilsCV.cyan;
            case "Magenta":
                return GraphicsUtilsCV.magenta;
            case "Lila":
                return GraphicsUtilsCV.purple;
            default:
                return new float[0];
        }
    }

    public static int anzahlVertices(String eingabe) {
        int anzahl = 0;
        eingabe = leerzeichenEntfernen(eingabe);
        for (int i = 0; i < eingabe.length(); i++) {
            anzahl = anzahl + schriftzeichenDao.findAnzahlVonVerticesByName(charAt(eingabe, i));
        }
        return anzahl;
    }

    private static String leerzeichenEntfernen(String str) {
        return str.replaceAll("\\s+", "");
    }

    public static void elementEntfernen(int index) {
        hoehe = elementEntfernen(hoehe, index);
        breite = elementEntfernen(breite, index);
    }

    public static float[] elementEntfernen(float[] arr, int index) {
        float[] newArr = new float[arr.length - 1];
        int j = 0;
        for (int i = 0; i < arr.length; i++) {
            if (i != index) {
                newArr[j] = arr[i];
                j++;
            }
        }
        return newArr;
    }

    public static String zeichenEntfernenAnIndexOhneLeerzeichen(String str, int index) {
        int nonWhitespaceCharCount = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                nonWhitespaceCharCount++;
            }
            if (nonWhitespaceCharCount == index) {
                if (Character.isWhitespace(c)) {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s{2}", " ");
    }
}
