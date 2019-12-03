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

package boofcv.alg.feature.disparity.sgm.cost;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.disparity.sgm.SgmDisparityCost;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

/**
 * Base class for computing SGM cost using single pixel error metrics.
 *
 * @author Peter Abeles
 */
public class SgmCostBase<T extends ImageBase<T>> implements SgmDisparityCost<T> {
	T left, right;

	protected ComputeErrors<T> errorComputer;

	public SgmCostBase() {
	}

	public SgmCostBase(ComputeErrors<T> errorComputer) {
		this.errorComputer = errorComputer;
	}

	@Override
	public void process(T left, T right, int disparityMin, int disparityRange, Planar<GrayU16> costYXD) {
		init(left, right, disparityRange, costYXD);

		int maxDisparity = disparityMin +disparityRange-1;

		for (int y = 0; y < left.height; y++) {
			GrayU16 costXD = costYXD.getBand(y);

			int idxLeft  = left.startIndex  + y*left.stride;

			for (int x = 0; x < left.width; x++, idxLeft++) {
				int idxOut = costXD.startIndex + x*costYXD.stride;

				// the maximum disparity in which the pixel will be inside the right image
				int m = Math.min(x,maxDisparity);
				// start reading the right image at the smallest disparity then increase disparity size
				int idxRight = right.startIndex + y*right.stride + x - disparityMin;

				errorComputer.process(idxLeft,idxRight,idxOut,disparityMin,m, costXD);

				// Fill in the disparity values outside the image with max cost
				for (int d =m+1; d <= maxDisparity; d++) {
					costXD.data[idxOut+d] = SgmDisparityCost.MAX_COST;
				}
			}
		}
	}

	protected void init(T left, T right, int disparityRange, Planar<GrayU16> costYXD) {
		InputSanityCheck.checkSameShape(left,right);
		this.left = left;
		this.right = right;

		// Declare the "tensor" with shape (lengthY,lengthX,lengthD)
		costYXD.reshape(disparityRange,left.width,left.height);
	}

	public void setErrorComputer(ComputeErrors<T> errorComputer) {
		this.errorComputer = errorComputer;
		this.errorComputer.setOwner(this);
	}

	public interface ComputeErrors<T extends ImageBase<T>> {
		void setOwner( SgmCostBase<T> owner );

		void process(int idxLeft, int idxRight, int idxOut, int disparityMin, int disparityMax, GrayU16 costXD );
	}
}
