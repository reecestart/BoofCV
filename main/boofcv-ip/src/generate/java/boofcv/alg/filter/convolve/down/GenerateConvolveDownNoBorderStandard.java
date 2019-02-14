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

package boofcv.alg.filter.convolve.down;

import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * Generates straight forward unoptimized implementations of {@link ConvolveImageDownNoBorder}.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveDownNoBorderStandard extends CodeGeneratorBase {

	String className = "ConvolveDownNoBorderStandard";

	String kernelType;
	String inputType;
	String outputType;
	String kernelData;
	String inputData;
	String outputData;
	String bitWise;
	boolean hasDivisor;
	String declareHalf;
	String divide;

	@Override
	public void generate() throws FileNotFoundException {
		setOutputFile(className);
		printPreamble();

		print_F32_F32();
		print_U8_I16();
		print_S16_I16();
		print_U8_I8_Div();
		print_S16_I16_Div();

		out.println("}");
	}

	private void print_F32_F32() {
		kernelType = "F32";
		inputType = outputType = "GrayF32";
		kernelData = inputData = outputData = "float";
		bitWise = "";
		hasDivisor = false;

		printAll();
	}

	private void print_U8_I16() {
		kernelType = "I32";
		inputType = "GrayU8";
		outputType = "ImageInt16";
		kernelData = "int";
		inputData = "byte";
		outputData = "short";
		bitWise = "& 0xFF";
		hasDivisor = false;

		printAll();
	}
	
	private void print_S16_I16() {
		kernelType = "I32";
		inputType = "GrayS16";
		outputType = "ImageInt16";
		kernelData = "int";
		inputData = "short";
		outputData = "short";
		bitWise = "";
		hasDivisor = false;

		printAll();
	}

	private void print_U8_I8_Div() {
		kernelType = "I32";
		inputType = "GrayU8";
		outputType = "ImageInt8";
		kernelData = "int";
		inputData = "byte";
		outputData = "byte";
		bitWise = "& 0xFF";
		hasDivisor = true;

		printAll();
	}

	private void print_S16_I16_Div() {
		kernelType = "I32";
		inputType = "GrayS16";
		outputType = "ImageInt16";
		kernelData = "int";
		inputData = "short";
		outputData = "short";
		bitWise = "";
		hasDivisor = true;

		printAll();
	}

	private void printAll() {
		String typeCast = kernelData.compareTo(outputData) != 0 ? "("+outputData+")" : "";

		boolean isInteger = kernelType.compareTo("I32") == 0;
		declareHalf = isInteger && hasDivisor ? "\t\tint halfDivisor = divisor/2;\n" : "";
		divide = isInteger && hasDivisor ? "(total+halfDivisor)/divisor" : "total/divisor";

		printHorizontal(typeCast);
		printVertical(typeCast);
		printConvolve(typeCast);
	}

	private void printPreamble() {
		out.print("import boofcv.struct.convolve.Kernel1D_F32;\n" +
				"import boofcv.struct.convolve.Kernel1D_I32;\n" +
				"import boofcv.struct.convolve.Kernel2D_F32;\n" +
				"import boofcv.struct.convolve.Kernel2D_I32;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Standard implementation of {@link boofcv.alg.filter.convolve.ConvolveDownNoBorder} where no special\n" +
				" * optimization has been done.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * DO NOT MODIFY: This class was automatically generated by {@link "+getClass().getSimpleName()+"}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" {\n\n");
	}

	private void printHorizontal( String typeCast) {
		out.print("\tpublic static void horizontal( Kernel1D_"+kernelType+" kernel ,\n" +
				"\t\t\t\t\t\t\t\t   "+inputType+" input, "+outputType+" output ,\n");
		if( hasDivisor ) {
			out.print("\t\t\t\t\t\t\t\t   int skip , "+kernelData+" divisor) {\n");
		} else {
			out.print("\t\t\t\t\t\t\t\t   int skip ) {\n");
		}

		out.print(
				"\t\tif( kernel.offset != kernel.width/2 || kernel.width%2 != 1)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Non symmetric odd kernels not supported\");\n\n");

		out.print("\t\tfinal "+inputData+"[] dataSrc = input.data;\n" +
				"\t\tfinal "+outputData+"[] dataDst = output.data;\n" +
				"\t\tfinal "+kernelData+"[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				declareHalf +
				"\n" +
				"\t\tfinal int widthEnd = UtilDownConvolve.computeMaxSide(input.width,skip,radius);\n" +
				"\t\tfinal int height = input.height;\n" +
				"\n" +
				"\t\tfinal int offsetX = UtilDownConvolve.computeOffset(skip,radius); \n" +
				"\n" +
				"\t\tfor( int i = 0; i < height; i++ ) {\n" +
				"\t\t\tint indexDst = output.startIndex + i*output.stride + offsetX/skip;\n" +
				"\t\t\tint j = input.startIndex + i*input.stride - radius;\n" +
				"\t\t\tfinal int jEnd = j+widthEnd;\n" +
				"\n" +
				"\t\t\tfor( j += offsetX; j <= jEnd; j += skip ) {\n" +
				"\t\t\t\t"+kernelData+" total = 0;\n" +
				"\t\t\t\tint indexSrc = j;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++] "+bitWise+") * dataKer[k];\n" +
				"\t\t\t\t}\n" +
				"\n");
		if( hasDivisor ) {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"("+divide+");\n");
		} else {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"total;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printVertical( String typeCast) {
		out.print("\tpublic static void vertical( Kernel1D_"+kernelType+" kernel,\n" +
				"\t\t\t\t\t\t\t\t "+inputType+" input, "+outputType+" output,\n");
		if( hasDivisor ) {
			out.print("\t\t\t\t\t\t\t\t int skip , "+kernelData+" divisor ) {\n");
		} else {
			out.print("\t\t\t\t\t\t\t\t int skip ) {\n");
		}
		out.print("\t\tfinal "+inputData+"[] dataSrc = input.data;\n" +
				"\t\tfinal "+outputData+"[] dataDst = output.data;\n" +
				"\t\tfinal "+kernelData+"[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				declareHalf +
				"\n" +
				"\t\tfinal int width = input.width;\n" +
				"\t\tfinal int heightEnd = UtilDownConvolve.computeMaxSide(input.height,skip,radius);\n" +
				"\n" +
				"\t\tfinal int offsetY = UtilDownConvolve.computeOffset(skip,radius);\n" +
				"\n" +
				"\t\tfor( int y = offsetY; y <= heightEnd; y += skip ) {\n" +
				"\t\t\tint indexDst = output.startIndex + (y/skip)*output.stride;\n" +
				"\t\t\tint i = input.startIndex + (y-radius)*input.stride;\n" +
				"\t\t\tfinal int iEnd = i + width;\n" +
				"\n" +
				"\t\t\tfor( ; i < iEnd; i++ ) {\n" +
				"\t\t\t\t"+kernelData+" total = 0;\n" +
				"\t\t\t\tint indexSrc = i;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc] "+bitWise+") * dataKer[k];\n" +
				"\t\t\t\t\tindexSrc += input.stride;\n" +
				"\t\t\t\t}\n" +
				"\n");
		if( hasDivisor ) {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"("+divide+");\n");
		} else {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"total;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvolve( String typeCast) {
		out.print("\tpublic static void convolve( Kernel2D_"+kernelType+" kernel ,\n" +
				"\t\t\t\t\t\t\t\t "+inputType+" input , "+outputType+" output , ");
		if( hasDivisor ) {
			out.print("int skip , "+kernelData+" divisor )\n");
		} else {
			out.print("int skip )\n");
		}
		out.print("\t{\n" +
				"\t\tfinal "+inputData+"[] dataSrc = input.data;\n" +
				"\t\tfinal "+outputData+"[] dataDst = output.data;\n" +
				"\t\tfinal "+kernelData+"[] dataKernel = kernel.data;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int widthEnd = UtilDownConvolve.computeMaxSide(input.width,skip,radius);\n" +
				"\t\tfinal int heightEnd = UtilDownConvolve.computeMaxSide(input.height,skip,radius);\n" +
				declareHalf +
				"\n" +
				"\t\tfinal int offset = UtilDownConvolve.computeOffset(skip,radius); \n" +
				"\n" +
				"\t\tfor( int y = offset; y <= heightEnd; y += skip ) {\n" +
				"\t\t\tint indexDst = output.startIndex + (y/skip)*output.stride + offset/skip;\n" +
				"\t\t\tfor( int x = offset; x <= widthEnd; x += skip ) {\n" +
				"\t\t\t\t"+kernelData+" total = 0;\n" +
				"\t\t\t\tint indexKer = 0;\n" +
				"\t\t\t\tfor( int ki = -radius; ki <= radius; ki++ ) {\n" +
				"\t\t\t\t\tint indexSrc = input.startIndex+(y+ki)*input.stride+ x;\n" +
				"\t\t\t\t\tfor( int kj = -radius; kj <= radius; kj++ ) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc+kj] "+bitWise+")* dataKernel[indexKer++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n");
		if( hasDivisor ) {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"("+divide+");\n");
		} else {
			out.print("\t\t\t\tdataDst[indexDst++] = "+typeCast+"total;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateConvolveDownNoBorderStandard gen = new GenerateConvolveDownNoBorderStandard();

		gen.generate();

	}
}
