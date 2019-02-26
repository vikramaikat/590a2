package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class VideoDecoder {
	
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

	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/a2/a2-compressed-binomial-dist.dat";
		String output_file_name = "data/a2/reuncompressed.dat";

		FileInputStream fis = new FileInputStream(input_file_name);

		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		// Read in symbol counts and set up model
		
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
		

		int range_bit_width = 40;
		int frameWidth = 64;
		int frameHeight = 64;
		int fps = 30;
		int seconds = 10;
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(range_bit_width);

		// Decode and produce output.
		
		System.out.println("Uncompressing file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);
		
		int[][] startingFrame = new int[frameWidth][frameHeight];
		
		for(int i = 0; i < frameHeight; i++) {
			for(int j = 0; j < frameWidth; j++) {
				startingFrame[i][j] = fis.read();
				fos.write(startingFrame[i][j]);
				//System.out.print(startingFrame[i][j] + " ");
			}
			//System.out.println();
		}
		
		for(int f = 1; f < fps * seconds; f++) {
			for(int y = 0; y < frameHeight; y++) {
				for(int x = 0; x < frameWidth; x++) {
					int diff = decoder.decode(model, bit_source);
					int curPixel = diff + startingFrame[y][x];
					fos.write(curPixel);
					startingFrame[y][x] = curPixel;
				}
			}
		}
		
		System.out.println("Done.");
		fos.flush();
		fos.close();
		fis.close();
	}
}
