/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.dots;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector and tracker for Uchiya Markers (a.k.a. Random Dot)
 *
 * @see boofcv.alg.feature.describe.llah.LlahOperations
 *
 * @author Peter Abeles
 */
public class TrackUchiyaMarkers<T extends ImageBase<T>> {

	@Getter GrayU8 binary = new GrayU8(1,1);

	@Getter InputToBinary<T> inputToBinary;
	@Getter BinaryEllipseDetectorPixel ellipseDetector;
	@Getter LlahOperations llahOps;

	/** Threshold used to filter false positives. 0 to 1. higher the more strict */
	@Getter @Setter double landmarkThreshold = 0.2;
	/** Minimum number of hits a dot needs to a landmark to be considered a pair */
	@Getter @Setter int minDotHits = 5;
	/** Sets if tracking is turned on or not */
	@Getter @Setter boolean tracking = true;

	List<Point2D_F64> centers = new ArrayList<>();
	List<LlahOperations.FoundDocument> foundDocs = new ArrayList<>();

	// Data structures for tracking
	LlahOperations llahTrackingOps;

	// work space data structures
	FastQueue<PointIndex2D_F64> matches = new FastQueue<>(PointIndex2D_F64::new);

	Ransac<Homography2D_F64, AssociatedPair> ransac;
	FastQueue<AssociatedPair> ransacPairs = new FastQueue<>(AssociatedPair::new); // landmark -> dots

	public TrackUchiyaMarkers(InputToBinary<T> inputToBinary,
							  BinaryEllipseDetectorPixel ellipseDetector,
							  LlahOperations llahOps,
							  Ransac<Homography2D_F64, AssociatedPair> ransac) {
		this.inputToBinary = inputToBinary;
		this.ellipseDetector = ellipseDetector;
		this.llahOps = llahOps;
		this.ransac = ransac;

		llahTrackingOps = new LlahOperations(llahOps.getNumberOfNeighborsN(),llahOps.getSizeOfCombinationM(),llahOps.getHasher());
	}

	public void reset() {

	}

	public void process( T input ) {
		inputToBinary.process(input,binary);
		ellipseDetector.process(binary);
		List<BinaryEllipseDetectorPixel.Found> foundEllipses = ellipseDetector.getFound();

		// Convert ellipses to points that LLAH understands
		centers.clear();
		for (int i = 0; i < foundEllipses.size(); i++) {
			centers.add(foundEllipses.get(i).ellipse.center);
		}

		// Detect new markers
		llahOps.lookupDocuments(centers, landmarkThreshold, foundDocs);

		for( int i = 0; i < foundDocs.size(); i++ ) {
			LlahOperations.FoundDocument f = foundDocs.get(i);

			// if it's being tracked already skip

			// create a new track
		}

		// look up all tracked documents and update the LLAH apperance

		//

		System.out.println("Found documents "+foundDocs.size());
		for( var r : foundDocs ) {
			System.out.println("Doc #"+r.document.documentID+" matched = "+r.countMatches()+" / "+r.document.landmarks.size);
		}

		// see if there are any matches

		// Try to match points to markers which are being tracked

		// take matches and fit a homography to them

		// estimate pose
	}

	private boolean fitHomography( List<Point2D_F64> dots , LlahOperations.FoundDocument observed ) {
		// create the ransac pairs
		ransacPairs.reset();
		for (int landmarkIdx = 0; landmarkIdx < observed.landmarkToDots.size; landmarkIdx++) {
			final Point2D_F64 landmark = observed.document.landmarks.get(landmarkIdx);
			TIntObjectHashMap<LlahOperations.DotCount> dotToLandmark = observed.landmarkToDots.get(landmarkIdx);
			dotToLandmark.forEachEntry((key,dc)-> {
				if( dc.counts >= minDotHits) {
					ransacPairs.grow().set(landmark,dots.get(dc.dotIdx));
				}
				return false;
			});
		}
		if( ransacPairs.size < ransac.getMinimumSize() )
			return false;

		if( !ransac.process(ransacPairs.toList()) )
			return false;

		return true;
	}

	/**
	 * Given the current tracked markers, update their LLAH descriptors for looking them up in the next frame
	 */
	private void updateTrackingLlah() {

	}

	public static class Found
	{
		public int documentID;
		// pixel coordinates of landmarks in the image

		// homography for landmark to image pixel coordinate
	}
}
