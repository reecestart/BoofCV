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

package boofcv.alg.feature.disparity.impl;

import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateSelectRectStandardBase extends CodeGeneratorBase {

	String dataAbr;
	String sumType;
	String sumNumType;
	boolean isFloat;

	@Override
	public void generate() throws FileNotFoundException {
		createFile(false);
		createFile(true);
	}

	public void createFile(  boolean isFloat ) throws FileNotFoundException {
		this.isFloat = isFloat;
		if( isFloat ) {
			sumType = "float";
			dataAbr = "F32";
			sumNumType = "Float";
		} else {
			sumType = "int";
			dataAbr = "S32";
			sumNumType = "Integer";
		}

		setOutputFile("ImplSelectRectStandardBase_"+dataAbr);

		printPreamble();
		printSmallFuncs();
		printProcess();
		printSelectRightToLeft();

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.feature.disparity.SelectRectStandard;\n" +
				"import boofcv.struct.image.ImageGray;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementation of {@link SelectRectStandard} as a base class for arrays of type "+dataAbr+".\n" +
				" * Extend for different output image types.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * DO NOT MODIFY. Generated by {@link GenerateSelectRectStandardBase}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public abstract class "+className+"<T extends ImageGray<T>>\n" +
				"\t\textends SelectRectStandard<"+sumType+"[],T>\n" +
				"{\n" +
				"\t// scores organized for more efficient processing\n" +
				"\t"+sumType+" columnScore[] = new "+sumType+"[1];\n" +
				"\tint imageWidth;\n" +
				"\n" +
				"\t// texture threshold, use an integer value for speed.\n");
		if( isFloat ) {
			out.print("\tprotected float textureThreshold;\n");
		} else {
			out.print("\tprotected int textureThreshold;\n" +
					"\tprotected static final int discretizer = 10000;\n");
		}

		out.print("\n" +
				"\tpublic "+className+"(int maxError, int rightToLeftTolerance, double texture) {\n" +
				"\t\tsuper(maxError,rightToLeftTolerance,texture);\n" +
				"\t}\n\n");
	}

	private void printSmallFuncs() {
		if( isFloat ) {
			out.print("\t@Override\n" +
					"\tpublic void setTexture(double threshold) {\n" +
					"\t\ttextureThreshold = (float)threshold;\n" +
					"\t}\n");
		} else {
			out.print("\t@Override\n" +
					"\tpublic void setTexture(double threshold) {\n" +
					"\t\ttextureThreshold = (int)(discretizer*threshold);\n" +
					"\t}\n");
		}

		out.print("\n" +
				"\t@Override\n" +
				"\tpublic void configure(T imageDisparity, int minDisparity, int maxDisparity , int radiusX ) {\n" +
				"\t\tsuper.configure(imageDisparity,minDisparity,maxDisparity,radiusX);\n" +
				"\n" +
				"\t\tif( columnScore.length < maxDisparity )\n" +
				"\t\t\tcolumnScore = new "+sumType+"[maxDisparity];\n" +
				"\t\timageWidth = imageDisparity.width;\n" +
				"\t}\n\n");
	}

	private void printProcess() {
		out.print("\t@Override\n" +
				"\tpublic void process(int row, "+sumType+"[] scores ) {\n" +
				"\n" +
				"\t\tint indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride + radiusX + minDisparity;\n" +
				"\n" +
				"\t\tfor( int col = minDisparity; col <= imageWidth-regionWidth; col++ ) {\n" +
				"\t\t\t// Determine the number of disparities that can be considered at this column\n" +
				"\t\t\t// make sure the disparity search doesn't go outside the image border\n" +
				"\t\t\tlocalMax = maxDisparityAtColumnL2R(col);\n" +
				"\n" +
				"\t\t\t// index of the element being examined in the score array\n" +
				"\t\t\tint indexScore = col - minDisparity;\n" +
				"\n" +
				"\t\t\t// select the best disparity\n" +
				"\t\t\tint bestDisparity = 0;\n" +
				"\t\t\t"+sumType+" scoreBest = columnScore[0] = scores[indexScore];\n" +
				"\t\t\tindexScore += imageWidth;\n" +
				"\n" +
				"\t\t\tfor( int i = 1; i < localMax; i++ ,indexScore += imageWidth) {\n" +
				"\t\t\t\t"+sumType+" s = scores[indexScore];\n" +
				"\t\t\t\tcolumnScore[i] = s;\n" +
				"\t\t\t\tif( s < scoreBest ) {\n" +
				"\t\t\t\t\tscoreBest = s;\n" +
				"\t\t\t\t\tbestDisparity = i;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// detect bad matches\n" +
				"\t\t\tif( scoreBest > maxError ) {\n" +
				"\t\t\t\t// make sure the error isn't too large\n" +
				"\t\t\t\tbestDisparity = invalidDisparity;\n" +
				"\t\t\t} else if( rightToLeftTolerance >= 0 ) {\n" +
				"\t\t\t\t// if the associate is different going the other direction it is probably noise\n" +
				"\n" +
				"\t\t\t\tint disparityRtoL = selectRightToLeft(col-bestDisparity-minDisparity,scores);\n" +
				"\n" +
				"\t\t\t\tif( Math.abs(disparityRtoL-bestDisparity) > rightToLeftTolerance ) {\n" +
				"\t\t\t\t\tbestDisparity = invalidDisparity;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t// test to see if the region lacks sufficient texture if:\n" +
				"\t\t\t// 1) not already eliminated 2) sufficient disparities to check, 3) it's activated\n" +
				"\t\t\tif( textureThreshold > 0 && bestDisparity != invalidDisparity && localMax >= 3 ) {\n" +
				"\t\t\t\t// find the second best disparity value and exclude its neighbors\n" +
				"\t\t\t\t"+sumType+" secondBest = "+sumNumType+".MAX_VALUE;\n" +
				"\t\t\t\tfor( int i = 0; i < bestDisparity-1; i++ ) {\n" +
				"\t\t\t\t\tif( columnScore[i] < secondBest ) {\n" +
				"\t\t\t\t\t\tsecondBest = columnScore[i];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tfor( int i = bestDisparity+2; i < localMax; i++ ) {\n" +
				"\t\t\t\t\tif( columnScore[i] < secondBest ) {\n" +
				"\t\t\t\t\t\tsecondBest = columnScore[i];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// similar scores indicate lack of texture\n" +
				"\t\t\t\t// C = (C2-C1)/C1\n");
		if( isFloat ) {
			out.print("\t\t\t\tif( secondBest-scoreBest <= textureThreshold*scoreBest )\n");
		} else {
			out.print("\t\t\t\tif( discretizer *(secondBest-scoreBest) <= textureThreshold*scoreBest )\n");
		}
		out.print("\t\t\t\t\tbestDisparity = invalidDisparity;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tsetDisparity(indexDisparity++ , bestDisparity );\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printSelectRightToLeft() {
		out.print("\t/**\n" +
				"\t * Finds the best disparity going from right to left image.\n" +
				"\t *\n" +
				"\t */\n" +
				"\tprivate int selectRightToLeft( int col , "+sumType+"[] scores ) {\n" +
				"\t\t// see how far it can search\n" +
				"\t\tint localMax = Math.min(imageWidth-regionWidth,col+maxDisparity)-col-minDisparity;\n" +
				"\n" +
				"\t\tint indexBest = 0;\n" +
				"\t\tint indexScore = col;\n" +
				"\t\t"+sumType+" scoreBest = scores[col];\n" +
				"\t\tindexScore += imageWidth+1;\n" +
				"\n" +
				"\t\tfor( int i = 1; i < localMax; i++ ,indexScore += imageWidth+1) {\n" +
				"\t\t\t"+sumType+" s = scores[indexScore];\n" +
				"\n" +
				"\t\t\tif( s < scoreBest ) {\n" +
				"\t\t\t\tscoreBest = s;\n" +
				"\t\t\t\tindexBest = i;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn indexBest;\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateSelectRectStandardBase gen = new GenerateSelectRectStandardBase();

		gen.generate();
	}
}
