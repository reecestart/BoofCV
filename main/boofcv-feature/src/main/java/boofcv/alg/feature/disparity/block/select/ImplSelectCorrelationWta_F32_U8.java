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

package boofcv.alg.feature.disparity.block.select;

import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.block.SelectDisparityBasicWta;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Implementation of {@link SelectDisparityBasicWta} for scores of type F32 and correlation. Since it's correlation
 * it pixels the score with the highest value. tODO smoothng. cite paper
 * </p>
 *
 * <p>
 * DO NOT MODIFY. Generated by {@link GenerateSelectRectBasicWta}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplSelectCorrelationWta_F32_U8 extends SelectDisparityBasicWta<float[],GrayU8>
{
	Kernel1D_F32 box = FactoryKernel.table1D_F32(2,true);
//	float[] scoresByD;
//	float[] smoothed;

	@Override
	public void configure(GrayU8 imageDisparity, int minDisparity, int maxDisparity, int radiusX) {
		super.configure(imageDisparity, minDisparity, maxDisparity, radiusX);
	}

	@Override
	public void process(int row, float[] blockOfScores) {
		// declare it to be the maximum length possible
		float[] scoresByD = new float[ maxDisparity-minDisparity+1];
		float[] smoothed = new float[ scoresByD.length ];

		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride + radiusX + minDisparity;

		for( int col = minDisparity; col <= imageWidth-regionWidth; col++ ) {
			boolean print = ((col+radiusX)==34&&row==131);

			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(col);


			int indexScore = col-minDisparity;
			for( int i = 0; i < localMax; i++ ,indexScore += imageWidth) {
				scoresByD[i] = blockOfScores[indexScore];
//				if( print ) System.out.printf("%2d %5.2f : ",i,scoresByD[i]);
			}
//			if( print )System.out.println("   done");

//			if( localMax >= box.width )
//				KernelMath.convolveSmooth(box,scoresByD,smoothed,localMax);
//			else
				System.arraycopy(scoresByD,0,smoothed,0,localMax);

			float maxValue = smoothed[0];
			int maxIndex = 0;
			for (int i = 1; i < localMax; i++) {
				float v = smoothed[i];
				if( v > maxValue ) {
					maxValue = v;
					maxIndex = i;
				}
			}

			imageDisparity.data[indexDisparity++] = (byte)maxIndex;
		}
	}

	@Override
	public DisparitySelect<float[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override
	public Class<GrayU8> getDisparityType() {
		return GrayU8.class;
	}
}