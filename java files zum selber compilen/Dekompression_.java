/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */



import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static java.lang.System.arraycopy;


public class Dekompression_ implements PlugIn {
/*Da im vorhinein kein Bild geöffnet wird lässt sich für den Dekodierer das PluginFIlter Interface nicht nutzen,
* aus diesem Grund wird hier nur das PlugIn Interface genutzt*/

    private String bytefilepath;
    private int height;
    private int width;
    private int[][] r;
    private int[][] g;
    private int[][] b;
    boolean rgb;
    private byte[] huffmanbytes;
    private byte[] encodedbytes;

    @Override
    public void run(String argument) {


        if (showDialog()) {

            huffman_decoding();
            readbytes();
            byte[] decoded = run_length_decode();
            bytetoRGB(decoded);
            rebuild();

        }
    }

    private boolean showDialog() {
        //GenericDialog gd = new GenericDialog("Kompression");
        OpenDialog gd = new OpenDialog("Datei");
        String path = gd.getPath();

        if (path == null)//sollte keine Datei vom Nutzer geöffner werden so ist der path 0 und das Plugin wird abgebrochen
            return false;


        this.bytefilepath = path;
        return true;
    }


    public void showAbout() {
        IJ.showMessage("Dekompressionsplugin",
                "Ein Plugin welches Bilder mit Hilfe einer Lauflängencodierung verlustfrei dekomprimiert."
        );
    }

    private void huffman_decoding(){
        OutputStream os = new ByteArrayOutputStream();
        String inputFile = bytefilepath;
        Huffman huf = new Huffman(inputFile,os);
        huf.expand();

        this.huffmanbytes = ((ByteArrayOutputStream) os).toByteArray();
    }
    private void readbytes() {

            byte[] array = huffmanbytes;
            byte[] width = new byte[4];
            byte[] height = new byte[4];
            byte[] encodedbytes = new byte[array.length - 9];// arrays werden erstellt um das gesamte array in teilarrays zu zerlegen
            if (array[8] == (byte) 0xFF) {
                rgb = true;
            } else {
                rgb = false;
            }// diese Stelle erkennt ob es sich um ein RGB oder Graustufenbild handelt
            arraycopy(array, 0, width, 0, 4);
            arraycopy(array, 4, height, 0, 4);
            arraycopy(array, 9, encodedbytes, 0, array.length - 9); // kopiert Teile aus dem Gesamtarrays in kleinere Arrays
            ByteBuffer bb = ByteBuffer.wrap(width);
            this.width = bb.getInt();
            bb = ByteBuffer.wrap(height);
            this.height = bb.getInt();// mit Hilfe von bytebuffers werden aus den byte[] für Höhe und Breite Integer Werte gemacht
            this.encodedbytes = encodedbytes;// die encodedbytes enthalten die noch komprimierten Bildinformationen, welche später noch dekomprimiert werden


    }



    private byte[] run_length_decode()
    /*
    * Mit dieser Funktion werden die Bildinformationen wieder dekomprimiert,
    * da immer ein matchCount auf eine Bildinformation folgt lässt sich dieser
    * Algorithmus mit zwei geschachtelten Schleifen umsetzen
    *
    * */{
        byte[] encodedbytes = this.encodedbytes;
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        //int j = 1;
        for (int i = 0; i < encodedbytes.length - 1; i += 2) {

            byte b = encodedbytes[i+1];
            int matchCount = encodedbytes[i] & 0xff;
            for (int l = 0; l < matchCount; l++) {
                dest.write(b);
            }
          //  j += 2;
        }

        return dest.toByteArray();
    }


    private void bytetoRGB(byte[] decodedbytes)
    /*
    * Diese Funktion zerlegt die dekodierten Bildinformation in farblich getrennte Arrays
    * */{
        int h = this.height;
        int w = this.width;
        int[][] r = new int[h][w];
        int[][] g = new int[h][w];
        int[][] b = new int[h][w];
        int x = 0;
        if (rgb) {

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    r[j][i] = decodedbytes[x++];
                }
            }

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    g[j][i] = decodedbytes[x++];
                }
            }

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    b[j][i] = decodedbytes[x++];
                       }
            }

            this.r = r;
            this.g = g;
            this.b = b;

        } else {

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    r[j][i] = decodedbytes[x++];
                   }
            }
            this.r = r;
            this.g = r;
            this.b = r;
        }


    }


    private void rebuild()
    {
        ImagePlus IP;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);// es wird ein bufferedimage mit den bekannten werten für Breite und Höhe mit dem Typ RGB initialisiert

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = b[y][x] & 0xff;
                rgb = (rgb << 8) + (g[y][x] & 0xff);
                rgb = (rgb << 8) + (r[y][x] & 0xff);
                image.setRGB(x, y, rgb);
            }
        }// Diese geschachtelte Schleife geht das Bild Pixel für Pixel ab und setzt dort die rgb werte ein
        IP = new ImagePlus("Wiederhergestelltes Bild", image); //aus den BufferedImage wird ein ImageJ spezifisches ImagePlus erstellt
        IP.show();// Diese Funktion zeigt das ImagePlus an
    }


   /* public static void main(String[] args) {

	/*Die Main Methode ist nur zum testen in der IDE notwendig mit der Funktionweise des
	endgültigen Plugins hat sie nichts zu tun
        Class<?> clazz = Dekompression_.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();


        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }*/
}
