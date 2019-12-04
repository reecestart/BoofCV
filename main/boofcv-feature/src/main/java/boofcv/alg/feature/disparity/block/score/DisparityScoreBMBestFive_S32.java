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

package boofcv.alg.feature.disparity.block.score;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.disparity.DisparityBlockMatchBestFive;
import boofcv.alg.feature.disparity.block.BlockRowScore;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.concurrency.BoofConcurrency;
import boofcv.concurrency.IntRangeObjectConsumer;
import boofcv.misc.Compare_S32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

import javax.annotation.Generated;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreWindowFive} for processing
 * images of type {@link GrayU8}.
 * </p>
 *
 * <p>
 * DO NOT MODIFY. This code was automatically generated by GenerateDisparityBMBestFive_SAD.
 * <p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.feature.disparity.impl.GenerateDisparityBMBestFive_SAD")
public class DisparityScoreBMBestFive_S32<T extends ImageBase<T>,DI extends ImageGray<DI>>
		extends DisparityBlockMatchBestFive<T,DI>
{
	// Computes disparity from scores
	DisparitySelect<int[], DI> disparitySelect0;

	BlockRowScore<T,int[]> scoreRows;

	// reference to input images;
	T left, right;
	DI disparity;

	FastQueue workspace = new FastQueue<>(WorkSpace.class, WorkSpace::new);
	ComputeBlock computeBlock = new ComputeBlock();

	public DisparityScoreBMBestFive_S32(int minDisparity, int maxDisparity,
										int regionRadiusX, int regionRadiusY,
										BlockRowScore<T,int[]> scoreRows,
										DisparitySelect<int[], DI> computeDisparity) {
		super(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);
		this.scoreRows = scoreRows;
		this.disparitySelect0 = computeDisparity;
		workspace.grow();

		if( !(computeDisparity instanceof Compare_S32) )
			throw new IllegalArgumentException("computeDisparity must also implement Compare_S32");
	}

	@Override
	public void _process(T left , T right , DI disparity ) {
		InputSanityCheck.checkSameShape(left,right);
		disparity.reshape(left.width,left.height);
		this.left = left;
		this.right = right;
		this.disparity = disparity;
		scoreRows.setInput(left,right);

		if( BoofConcurrency.USE_CONCURRENT ) {
			BoofConcurrency.loopBlocks(0,left.height,regionHeight,workspace,computeBlock);
		} else {
			computeBlock.accept((WorkSpace)workspace.get(0),0,left.height);
		}
	}

	class WorkSpace {
		// stores the local scores for the width of the region
		int[] elementScore;
		// scores along horizontal axis for current block
		int[][] horizontalScore;
		// summed scores along vertical axis
		// Save the last regionHeight scores in a rolling window
		int[][] verticalScore;
		int[][] verticalScoreNorm;
		// In the rolling verticalScore window, which one is the active one
		int activeVerticalScore;
		// Where the final score it stored that has been computed from five regions
		int[] fiveScore;

		DisparitySelect<int[], DI> computeDisparity;

		public void checkSize() {
			if( horizontalScore == null || verticalScore.length < lengthHorizontal ) {
				horizontalScore = new int[regionHeight][lengthHorizontal];
				verticalScore = new int[regionHeight][lengthHorizontal];
				if( scoreRows.isRequireNormalize() )
					verticalScoreNorm = new int[regionHeight][lengthHorizontal];
				elementScore = new int[ left.width ];
				fiveScore = new int[ lengthHorizontal ];
			}
			if( computeDisparity == null ) {
				computeDisparity = disparitySelect0.concurrentCopy();
			}
			computeDisparity.configure(disparity,minDisparity,maxDisparity);

//			for (int i = 0; i < regionHeight; i++) {
//				Arrays.fill(horizontalScore[i],Integer.MAX_VALUE);
//				Arrays.fill(verticalScore[i],Integer.MAX_VALUE);
//			}
//			Arrays.fill(elementScore,Integer.MAX_VALUE);
//			Arrays.fill(fiveScore,Integer.MAX_VALUE);
		}
	}

	private class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {
		@Override
		public void accept(WorkSpace workspace, int minInclusive, int maxExclusive)
		{
			workspace.checkSize();

			// The image border will be skipped, so it needs to back track some
			int row0 = Math.max(0,minInclusive-2*radiusY);
			int row1 = Math.min(left.height,maxExclusive+2*radiusY);

			// initialize computation
			computeFirstRow(row0, workspace);

			// efficiently compute rest of the rows using previous results to avoid repeat computations
			computeRemainingRows(row0,row1, workspace);
		}
	}

	/**
	 * Initializes disparity calculation by finding the scores for the initial block of horizontal
	 * rows.
	 */
	private void computeFirstRow( final int row0 , final WorkSpace workSpace ) {
		workSpace.activeVerticalScore = 1;

		// compute horizontal scores for first row block
		for( int row = 0; row < regionHeight; row++ )
		{
			int[] scores = workSpace.horizontalScore[row];
			scoreRows.scoreRow(row0+row, scores, minDisparity, maxDisparity, regionWidth, workSpace.elementScore);
		}

		// compute score for the top possible row
		final int firstRow[] = workSpace.verticalScore[0];
		for( int i = 0; i < lengthHorizontal; i++ ) {
			int sum = 0;
			for( int row = 0; row < regionHeight; row++ ) {
				sum += workSpace.horizontalScore[row][i];
			}
			firstRow[i] = sum;
		}

		if( scoreRows.isRequireNormalize() )
			scoreRows.normalizeRegionScores(row0+radiusY,
					firstRow,minDisparity,maxDisparity,regionWidth,regionHeight,workSpace.verticalScoreNorm[0]);
	}

	/**
	 * Using previously computed results it efficiently finds the disparity in the remaining rows.
	 * When a new block is processes the last row/column is subtracted and the new row/column is
	 * added.
	 */
	private void computeRemainingRows(final int row0 , final int row1, final WorkSpace workSpace )
	{
		for( int row = row0+regionHeight; row < row1; row++ , workSpace.activeVerticalScore++) {
			int activeIndex = workSpace.activeVerticalScore % regionHeight;
			int oldRow = (row-row0)%regionHeight;
			int previous[] = workSpace.verticalScore[ (workSpace.activeVerticalScore -1) % regionHeight ];
			int active[] = workSpace.verticalScore[ activeIndex ];

			// subtract first row from vertical score
			int[] scores = workSpace.horizontalScore[oldRow];
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] = previous[i] - scores[i];
			}

			scoreRows.scoreRow(row, scores, minDisparity,maxDisparity,regionWidth,workSpace.elementScore);

			// add the new score
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] += scores[i];
			}

			if( scoreRows.isRequireNormalize() )
				scoreRows.normalizeRegionScores(row-radiusY,
						active,minDisparity,maxDisparity,regionWidth,regionHeight,workSpace.verticalScoreNorm[activeIndex]);

			if( workSpace.activeVerticalScore >= regionHeight-1 ) {
				int[] top, middle, bottom;
				if( scoreRows.isRequireNormalize() ) {
					top = workSpace.verticalScoreNorm[ (workSpace.activeVerticalScore -2*radiusY) % regionHeight ];
					middle = workSpace.verticalScoreNorm[ (workSpace.activeVerticalScore -radiusY) % regionHeight ];
					bottom = workSpace.verticalScoreNorm[workSpace. activeVerticalScore % regionHeight ];
				} else {
					top = workSpace.verticalScore[ (workSpace.activeVerticalScore -2*radiusY) % regionHeight ];
					middle = workSpace.verticalScore[ (workSpace.activeVerticalScore -radiusY) % regionHeight ];
					bottom = workSpace.verticalScore[workSpace. activeVerticalScore % regionHeight ];
				}

				computeScoreFive(top,middle,bottom,workSpace.fiveScore,left.width,(Compare_S32)workSpace.computeDisparity);
				workSpace.computeDisparity.process(row - (1 + 4*radiusY) + 2*radiusY+1, workSpace.fiveScore );
			}
		}
	}

	/**
	 * Compute the final score by sampling the 5 regions.  Four regions are sampled around the center
	 * region.  Out of those four only the two with the smallest score are used.
	 */
	protected void computeScoreFive( int top[] , int middle[] , int bottom[] , int score[] , int width ,
									 Compare_S32 compare  ) {

		// disparity as the outer loop to maximize common elements in inner loops, reducing redundant calculations
		for( int d = minDisparity; d <= maxDisparity; d++ ) {
			// take in account the different in image border between the sub-regions and the effective region
			int indexSrc = (d-minDisparity)*width + (d-minDisparity);
			int indexDst = (d-minDisparity)*width + (d-minDisparity);

			// At the left border just score using the right two regions
			{
				int end = indexSrc + radiusX;
				while( indexSrc < end ) {
					int val0 = top[indexSrc];
					int val1 = top[indexSrc+radiusX];
					int val2 = bottom[indexSrc];
					int val3 = bottom[indexSrc+radiusX];
					// select the two best scores from outer for regions
					if (compare.compare(val0, val1) < 0) {
						int temp = val0;
						val0 = val1;
						val1 = temp;
					}

					if (compare.compare(val2, val3) < 0) {
						int temp = val2;
						val2 = val3;
						val3 = temp;
					}

					int s;
					if (compare.compare(val0, val3) < 0) {
						s = val2 + val3;
					} else if (compare.compare(val1, val2) < 0) {
						s = val2 + val0;
					} else {
						s = val0 + val1;
					}

					score[indexDst++] = s + middle[indexSrc++];

				}
			}

			// Handle the inner image
			{
				int end = indexSrc + (width-d-2*radiusX);
				while (indexSrc < end) {
					// sample four outer regions at the corners around the center region
					int val0 = top[indexSrc - radiusX];
					int val1 = top[indexSrc + radiusX];
					int val2 = bottom[indexSrc - radiusX];
					int val3 = bottom[indexSrc + radiusX];

					// select the two best scores from outer for regions
					if (compare.compare(val0, val1) < 0) {
						int temp = val0;
						val0 = val1;
						val1 = temp;
					}

					if (compare.compare(val2, val3) < 0) {
						int temp = val2;
						val2 = val3;
						val3 = temp;
					}

					int s;
					if (compare.compare(val0, val3) < 0) {
						s = val2 + val3;
					} else if (compare.compare(val1, val2) < 0) {
						s = val2 + val0;
					} else {
						s = val0 + val1;
					}

					score[indexDst++] = s + middle[indexSrc++];
				}
			}

			// Time for the right border. Score using the left two regions due to lack of other choices
			{
				int end = indexSrc + radiusX;
				while( indexSrc < end ) {
					int val0 = top[indexSrc-radiusX];
					int val2 = bottom[indexSrc-radiusX];
					score[indexDst++] = val0 + val2 + middle[indexSrc++];
				}
			}
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return scoreRows.getImageType();
	}

	@Override
	public Class<DI> getDisparityType() {
		return disparitySelect0.getDisparityType();
	}

}
