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
import boofcv.misc.Compare_S32;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Implementation of {@link SelectDisparityBasicWta} for scores of type S32.
 * </p>
 *
 * <p>
 * DO NOT MODIFY. Generated by {@link GenerateSelectRectBasicWta}.
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectErrorBasicWta_S32_U8 extends SelectDisparityBasicWta<int[],GrayU8> implements Compare_S32
{
	@Override
	public void process(int row, int[] scores) {

		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride + minDisparity;

		for( int col = minDisparity; col < imageWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(col);

			int indexScore = col-minDisparity;

			int bestDisparity = 0;
			int scoreBest = scores[indexScore];
			indexScore += imageWidth;

			for( int i = 1; i < localMax; i++ ,indexScore += imageWidth) {
				int s = scores[indexScore];
				if( s < scoreBest ) {
					scoreBest = s;
					bestDisparity = i;
				}
			}

			imageDisparity.data[indexDisparity++] = (byte)bestDisparity;
		}
	}

	@Override
	public DisparitySelect<int[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override
	public Class<GrayU8> getDisparityType() {
		return GrayU8.class;
	}

	@Override
	public int compare(int scoreA, int scoreB) {
		return Integer.compare(-scoreA, -scoreB);
	}
}