package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;

public class VideoEncoder {

	public static BigInteger fact(int x) {
		BigInteger res = BigInteger.valueOf(1);
		
		for (int i = 1; i <= x; i++) {
			res = res.multiply(BigInteger.valueOf(i));
		}
		
		return res;
	}
	
	public static BigInteger nCk(int n, int k) {
		return fact(n).divide(fact(k).multiply(fact(n-k)));
	}
	
	public static void main(String[] args) throws IOException {
		
		int frameWidth = 64;
		int frameHeight = 64;
		int fps = 30;
		int seconds = 10;
		String input_file_name = "data/a2/a2_raw.dat";
		String output_file_name = "data/a2/a2-compressed-unifrom-dist.dat";

		int range_bit_width = 40;

		System.out.println("Encoding text file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		
		// Analyze file for frequency counts
		
		FileInputStream fis = new FileInputStream(input_file_name);
		
		int[][] startingFrame = new int[frameWidth][frameHeight];
		
		for(int i = 0; i < frameHeight; i++) {
			for(int j = 0; j < frameWidth; j++) {
				startingFrame[i][j] = fis.read();
				//System.out.print(startingFrame[i][j] + " ");
			}
			//System.out.println();
		}
		Integer[] symbols = new Integer[511];
		int[] symbol_counts = new int[511];

		// Create new model with analyzed frequency counts with linear dist
//		for(int i = -255; i < 256; i++) {
//			symbols[i + 255] = i;
//			int ct = 256;
//			if(i < 0) {
//				ct = ct + i;
//			}else if(i > 0) {
//				ct = ct - i;
//			}
//			symbol_counts[i+255] = ct;
//		}
		
		//Uniform dist
//		for(int i = -255; i < 256; i++) {
//			symbols[i + 255] = i;
//			symbol_counts[i+255] = 1;
//		}
		
		// Normal distribution
		
		for(int i = 0; i < 511; i++) {
			symbols[i] = i - 255;
			symbol_counts[i] = Math.max(1, (int) ((nCk(510,i).doubleValue() * Math.pow(.5, 510)) * 999999999.0));
		}
		
		FreqCountIntegerSymbolModel model = new FreqCountIntegerSymbolModel(symbols, symbol_counts);
		
		
		ArithmeticEncoder<Integer> encoder = new ArithmeticEncoder<Integer>(range_bit_width);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		// First 64 * 64 bytes are the values of the starting frame 
		for(int i = 0; i < frameHeight; i++) {
			for(int j = 0; j < frameWidth; j++) {
				bit_sink.write(startingFrame[i][j], 8);
			}
		}
		
		for(int f = 1; f < fps * seconds; f++) {
			for(int y = 0; y < frameHeight; y++) {
				for(int x = 0; x < frameWidth; x++) {
					int curPixel = fis.read();
					int diff = curPixel - startingFrame[y][x];
					encoder.encode(diff, model, bit_sink);
					startingFrame[y][x] = curPixel;
				}
			}
		}
		
		fis.close();
		
		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
}
