/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.SaveDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.*;
import java.nio.ByteBuffer;


import static ij.IJ.getDirectory;
import static ij.IJ.showMessage;


public class Kompression_ implements PlugInFilter {
	/*Das Interface PluginFilter wird implementiert um das in ImageJ bereits geöffnete Bild zu nehmen und weiterzuverarbeiten*/

	private int height;
	private int width;
	protected ImagePlus IP;
	private ImageProcessor ImageProc;
	public int[] pixels;
	private int[][]r;
	private int[][]g;
	private int[][]b;
	private String bytefilepath;
	private boolean rgb;
	private byte[] encodedbytes;
	private boolean grey;

	@Override
	public int setup(String arg, ImagePlus imp) { // die Setup Methode nimmt das in ImageJ geöffnete Bild und weist es der lokalen Imagevariable zu
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		this.IP = imp;

		if(imp.getBitDepth()==8){
		System.out.println("8");
		grey=true;
			return DOES_8G;
		}

		if(imp.getBitDepth()==24){
			grey=false;
			return DOES_RGB; }
			else{
			return DONE;
		}
	}

	@Override
	public void run(ImageProcessor ip) {


		String dir = getDirectory("image");
		String name = IP.getTitle();
		this.ImageProc = ip;
		this.width = ip.getWidth();
		this.height = ip.getHeight();

	//	this.bytefilepath=dir+name;

			setPixels();
			setRGBPixels();
			run_length_coding();
			finalizebytearray();
			huffman_encoding();
			IJ.error("Komprimierung abgeschlossen.");

	}


	public void setPixels()
	/*Diese Methode liest pixelweise Bilder ein und speichert
	 die Werte für jeden einzelnen Pixel in einem Array*/
	{

if(!grey){
			this.pixels = (int[]) ImageProc.getPixels();
		} else {
	//	showMessage("kein RGB Bild!");
	byte[] byt = (byte[]) ImageProc.getPixels();

	// notlösung aus zeitgründen
	int[] pix = new int[byt.length * 3];

	for (int i = 0; i < pix.length; i++) {
		pix[i] = byt[i % byt.length] & 0xff;
	}
	this.pixels = pix;
	System.out.println("bytlength" + byt.length);
}


	}

	public void setRGBPixels()
	/*  Mit dieser Methode wird das pixel[] in 3 farbarrays (r&g&b) zerlegt
	, am Ende der Methode wird noch der Grayscalecheck durchgeführt */
	{

		int[] pixels = this.pixels;
		int w = width, h = height;
		int[][] r = new int[h][w];
		int[][] g = new int[h][w];
		int[][] b = new int[h][w];

		if(grey){

			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					int value = pixels[i + w * j];
					// value is a bit-packed RGB value
					r[j][i] = value;
					g[j][i] = value;
					b[j][i] = value;

				}


		}
		else {
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					int value = pixels[i + w * j];
					// value is a bit-packed RGB value
					r[j][i] = value & 0xff;
					g[j][i] = (value >> 8) & 0xff;
					b[j][i] = (value >> 16) & 0xff;

				}
		}
		// System.out.println(r[0][0]+" "+g[0][0]+" "+b[0][0]);
		this.r = r;
		this.g = g;
		this.b = b;
		greyscalecheck();
	}

	public void greyscalecheck(){
		/*Sollte es sich um ein RGB Bild handeln, welches nur Graustufen enthält
		 so wird das mit dieser Methode erkannt, wodurch Graustufenbilder später effizienter gespeichert werden können*/
		int h = this.height;
		int w = this.width;
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				if ((r[j][i] != b[j][i])| (b[j][i] != g[j][i])){this.rgb = true; break;}

			}
		}
	}

	public byte[] toByte() {
		/* Diese Methode wandelt, die einzelnen Farbwerte um in ein byte[] */
		int h = this.height;
		int w = this.width;
		int[][] r = this.r;
		int[][] g = this.g;
		int[][] b = this.b;


		System.out.println("isrgb?"+rgb);
		if (!rgb) {
			byte[] bytes = new byte[h * w];
			int x = 0;
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					bytes[x++] = (byte) r[j][i];
					//  System.out.println("i:"+i+"j:"+j+"x:"+x);
				}
			}

			return bytes;
		} else {

			byte[] bytes = new byte[h * w * 3];
			int x = 0;
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					bytes[x++] = (byte) r[j][i];
					//  System.out.println("i:"+i+"j:"+j+"x:"+x);
				}
			}

			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					bytes[x++] = (byte) g[j][i];

				}
			}

			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					bytes[x++] = (byte) b[j][i];

				}
			}
			return bytes;
		}

	}

	public void run_length_coding(){
		/*
		* In dieser Funktion wird die Lauflängencodierung umgesetzt
		* , da diese Byteweise erfolgt und immer eine Lauflänge gefolgt von einem Wert geschrieben wird
		* können keine längeren Lauflängen als 255 realisiert werden.*/
		byte[] imageByteArray = this.toByte();
		ByteArrayOutputStream dest = new ByteArrayOutputStream();
		byte lastByte = imageByteArray[0];
		int matchCount = 1;
		for(int i=1; i < imageByteArray.length; i++){
			byte thisByte = imageByteArray[i];
			if ((lastByte == thisByte) & matchCount<255 ) {
				matchCount++;

			}
			else {
				dest.write((byte)matchCount);
				dest.write((byte)lastByte);
				matchCount=1;
				lastByte = thisByte;
			}
		}

		dest.write((byte)matchCount);
		dest.write((byte)lastByte);
		this.encodedbytes = dest.toByteArray();
	}

	public void huffman_encoding(){
		InputStream is = new ByteArrayInputStream(this.encodedbytes);
		SaveDialog gd = new SaveDialog("Speichern unter...",IP.getTitle(),".rlchuf");


		String path = gd.getDirectory()+gd.getFileName();


		if (gd.getFileName()!= null)
		{
		String out = path;
		System.out.println(path);
		Huffman huf = new Huffman(is,out);
		huf.compress();}

		else {showMessage("Bitte wähle einen Ort zum Speichern der komprimierten Datei aus!"); huffman_encoding();}

	}


	public void finalizebytearray() {
		/*
		 * Diese Methode schreibt das zuvor ertellte byte Array in ein File
		 * , welches später wieder vom Decoder geöffnet werden kann.
		 * Bevor das codierte ByteArray in die Datei geschrieben wird, werden noch
		 * Informationen für die Wiederherstellung vorher abgelegt.*/
		try {
			byte[] bytes = this.encodedbytes;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(this.width);
			byte[] w = bb.array();
			for (int i = 0; i < w.length; i++) {
				System.out.println(w[i]);
			}
			bos.write(bb.array());//die ersten 4 bytes enthalten die Weite
			ByteBuffer ba = ByteBuffer.allocate(4);
			ba.putInt(this.height);
			bos.write(ba.array()); // die nächsten 4 bytes die Höhe
			if (rgb) {
				bos.write(0xFF);
			} else {
				bos.write(0x00);
			} //Dieses Byte signalisiert, ob es sich um ein RGB Bild handelt
			bos.write(bytes); // die bytes mit den Bildinformationen werden geschrieben

			this.encodedbytes = bos.toByteArray();
		}catch (IOException e){e.printStackTrace();}
	}


	public void showAbout() {
		IJ.showMessage("Kompressionsplugin",
			"Ein Plugin welches Bilder mit Hilfe einer Lauflängencodierung verlustfrei komprimiert."
		);
	}

	/*public static void main(String[] args) {

	/*Die Main Methode ist nur zum testen in der IDE notwendig mit der Funktionweise des
	endgültigen Plugins hat sie nichts zu tun
		Class<?> clazz = Kompression_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("/home/colin/Documents/Plugin IJ/Testbilder/colortest.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}*/
}
