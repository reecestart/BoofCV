/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.filter.convolve.normalized;

import boofcv.generate.CodeGeneratorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class GenerateConvolveNormalizedStandardSparse {
	String className = "ConvolveNormalizedStandardSparse";

	PrintStream out;

	public GenerateConvolveNormalizedStandardSparse() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
	}

	public void generate() {
		printPreamble();
		printConvolve("F32", "GrayF32","float","");
		printConvolve("I32", "GrayU8","int"," & 0xFF");
		printConvolve("I32", "GrayS16","int","");
		out.println("}");
	}

	private void printPreamble() {
		out.print(CodeGeneratorUtil.copyright);
		out.print("package boofcv.alg.filter.convolve.normalized;\n" +
				"\n" +
				"import boofcv.struct.convolve.Kernel1D_F32;\n" +
				"import boofcv.struct.convolve.Kernel1D_I32;\n" +
				"import boofcv.struct.image.GrayF32;\n" +
				"import boofcv.struct.image.GrayS16;\n" +
				"import boofcv.struct.image.GrayU8;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Straight forward implementation of {@link boofcv.alg.filter.convolve.ConvolveNormalizedSparse} with minimal\n" +
				" * optimizations.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: Do not modify.  Automatically generated by {@link GenerateConvolveNormalizedStandardSparse}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" {\n\n");
	}

	private void printConvolve(String kernelType , String inputType ,  String sumType , String bitWise) {

		String divide = sumType.compareTo("int") == 0 ? "(total+div/2)/div" : "total/div";

		out.print("\tpublic static "+sumType+" convolve( Kernel1D_"+kernelType+" horizontal, Kernel1D_"+kernelType+" vertical,\n" +
				"\t\t\t\t\t\t\t\t"+inputType+" input, int c_x , int c_y, "+sumType+" storage[] )\n" +
				"\t{\n" +
				"\t\t// convolve horizontally first\n" +
				"\t\tint width = horizontal.getWidth();\n" +
				"\t\tint radius = width/2;\n" +
				"\n" +
				"\t\tint x0 = c_x-radius;\n" +
				"\t\tint x1 = c_x+radius+1;\n" +
				"\t\tint y0 = c_y-radius;\n" +
				"\t\tint y1 = c_y+radius+1;\n" +
				"\n" +
				"\t\tif( x0 < 0 ) x0 = 0;\n" +
				"\t\tif( y0 < 0 ) y0 = 0;\n" +
				"\t\tif( x1 > input.width ) x1 = input.width;\n" +
				"\t\tif( y1 > input.height ) y1 = input.height;\n" +
				"\n" +
				"\t\tfinal int startJ = x0-c_x+radius;\n" +
				"\t\tfinal int endJ = width-(c_x+radius+1-x1);\n" +
				"\t\tint indexStorage = y0-c_y+radius;\n"+
				"\t\tfor( int i = y0; i < y1; i++ ,indexStorage++) {\n" +
				"\t\t\tint indexImg = input.startIndex + i*input.stride + x0;\n" +
				"\n" +
				"\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t"+sumType+" div = 0;\n" +
				"\t\t\tfor( int j = startJ; j < endJ; j++ ,indexImg++) {\n" +
				"\t\t\t\tfinal "+sumType+" kerVal = horizontal.data[j];\n" +
				"\t\t\t\ttotal += (input.data[indexImg]"+bitWise+")*kerVal;\n" +
				"\t\t\t\tdiv += kerVal;\n" +
				"\t\t\t}\n" +
				"\t\t\tstorage[indexStorage] = "+divide+";\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// convolve vertically\n" +
				"\t\t"+sumType+" total = 0;\n" +
				"\t\t"+sumType+" div = 0;\n" +
				"\t\tfinal int endI = width-(c_y+radius+1-y1);\n" +
				"\t\tfor( int i = y0-c_y+radius; i < endI; i++ ) {\n" +
				"\t\t\tfinal "+sumType+" kerVal = vertical.data[i];\n" +
				"\t\t\ttotal += storage[i]*kerVal;\n" +
				"\t\t\tdiv += kerVal;\n" +
				"\t\t}\n" +
				"\t\treturn "+divide+";\n" +
				"\t}\n\n");
	}

	public static void main(String args[]) throws FileNotFoundException {
		GenerateConvolveNormalizedStandardSparse gen = new GenerateConvolveNormalizedStandardSparse();
		gen.generate();
	}
}
