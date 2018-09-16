/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SpecializedOps_DDRM;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScaleSceneStructure {

	@Test
	public void computePointStatistics() {
		SceneStructureMetric scene = new SceneStructureMetric(false);
		createProjectiveScene(scene,0xBEEF);

		ScaleSceneStructure alg = new ScaleSceneStructure();
		alg.computePointStatistics(scene.points);

		// See if it's near the center of the distribution, crudely
		assertTrue(alg.medianPoint.distance(new Point3D_F64(0,0,3)) < 1 );
		assertTrue( alg.medianDistancePoint > 0 && alg.medianDistancePoint < 2);
	}


	@Test
	public void apply_undo_metric() {
		for (int h = 0; h < 2; h++) {
			boolean homogenous = h == 1;

			ScaleSceneStructure alg = new ScaleSceneStructure();

			alg.medianPoint.set(0.5,0.9,1.3);
			alg.medianDistancePoint = 1.2;

			SceneStructureMetric expected = new SceneStructureMetric(homogenous);
			SceneStructureMetric found = new SceneStructureMetric(homogenous);

			BundleAdjustmentObservations obs = createProjectiveScene(found,0xBEEF);
			createProjectiveScene(expected,0xBEEF);

			// Should have perfect observations
			GenericBundleAdjustmentMetricChecks.checkReprojectionError(found,obs,1e-4);

			alg.applyScale(found,obs);

			// Make sure it was changed
			for (int i = 0; i < expected.views.length; i++) {
				assertNotEquals( expected.views[i].worldToView.T.distance(found.views[i].worldToView.T) , UtilEjml.TEST_F64);
			}

			// Must still have perfect observations if scaling was correctly applied. Otherwise solution will be changed when optimizing
			GenericBundleAdjustmentMetricChecks.checkReprojectionError(found,obs,1e-4);

			// Undo scaling and see if it got the original parameters back
			alg.undoScale(found,obs);

			GenericBundleAdjustmentMetricChecks.assertEquals(expected,found,1e-4,1e-8,1e-8);
			GenericBundleAdjustmentMetricChecks.checkReprojectionError(found,obs,1e-4);
		}
	}

	@Test
	public void apply_undo_projective() {
		for (int h = 0; h < 2; h++) {
			boolean homogenous = h == 1;
			ScaleSceneStructure alg = new ScaleSceneStructure();

			alg.medianPoint.set(0.5,0.9,1.3);
			alg.medianDistancePoint = 1.2;

			SceneStructureProjective expected = new SceneStructureProjective(homogenous);
			SceneStructureProjective found = new SceneStructureProjective(homogenous);

			BundleAdjustmentObservations obs = createProjectiveScene(found,0xBEEF);
			createProjectiveScene(expected,0xBEEF);

			// Should have perfect observations
			GenericBundleAdjustmentProjectiveChecks.checkReprojectionError(found,obs,1e-4);

			alg.applyScale(found,obs);

			// Make sure it was changed
			if( !homogenous ) {
				// can't normalize camera matrix in this situation
				for (int i = 0; i < expected.views.length; i++) {
					double error = SpecializedOps_DDRM.diffNormF(expected.views[i].worldToView, found.views[i].worldToView);
					assertTrue(error > UtilEjml.TEST_F64);
				}
			}

			// Must still have perfect observations if scaling was correctly applied. Otherwise solution will be changed when optimizing
			GenericBundleAdjustmentProjectiveChecks.checkReprojectionError(found,obs,1e-4);

			// Undo scaling and see if it got the original parameters back
			alg.undoScale(found,obs);

			GenericBundleAdjustmentProjectiveChecks.assertEquals(expected,found,1e-8);
			GenericBundleAdjustmentProjectiveChecks.checkReprojectionError(found,obs,1e-4);
		}
	}

	public static BundleAdjustmentObservations createProjectiveScene(SceneStructureMetric scene ,
																	 long seed ) {
		Random rand = new Random(seed);

		scene.initialize(2,5,20);
		BundleAdjustmentObservations observations = new BundleAdjustmentObservations(scene.views.length);

		CameraPinhole camera0 = new CameraPinhole(500+rand.nextDouble()*10,510+rand.nextDouble()*10,0,450,400,900,800);
//		CameraPinhole camera1 = new CameraPinhole(456+rand.nextDouble()*10,510+rand.nextDouble()*10,0,420,410,900,800);

		scene.setCamera(0,false,camera0);
//		scene.setCamera(1,false,camera1);

		for (int i = 0; i < scene.views.length; i++) {
			Se3_F64 worldToView = new Se3_F64();

			worldToView.T.x = i*0.2 + rand.nextGaussian()*0.1;
			worldToView.T.y = -i*0.1+ rand.nextGaussian()*0.1;
			worldToView.T.z = rand.nextGaussian()*0.05;

			double rotX = rand.nextGaussian()*0.05;
			double rotY = rand.nextGaussian()*0.05;
			double rotZ = rand.nextGaussian()*0.05;

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,worldToView.R);

			scene.setView(i,false,worldToView);

			scene.connectViewToCamera(i,0);
		}

		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		for (int i = 0; i < scene.points.length; i++) {
			// Point in world frame
			Point3D_F64 X = new Point3D_F64(rand.nextGaussian(),rand.nextGaussian(),3+rand.nextGaussian());
			if( scene.homogenous ) {
				scene.points[i].set(X.x, X.y, X.z, 1);
			} else {
				scene.points[i].set(X.x, X.y, X.z);
			}
			// Connect the point to views if it's visible inside of
			for (int j = 0; j < scene.views.length; j++) {
				w2p.configure(camera0,scene.views[j].worldToView); // approximate by using the same camera

				Point2D_F64 pixel = w2p.transform(X);
				if( pixel != null && pixel.x >= 0 && pixel.y >= 0 && pixel.x < camera0.width && pixel.y < camera0.height ) {
					scene.connectPointToView(i,j);

					observations.getView(j).add(i,(float)pixel.x,(float)pixel.y);
				}
			}
		}

		return observations;
	}


	public static BundleAdjustmentObservations createProjectiveScene(SceneStructureProjective scene ,
																	 long seed ) {
		Random rand = new Random(seed);

		scene.initialize(5,20);
		BundleAdjustmentObservations observations = new BundleAdjustmentObservations(scene.views.length);

		CameraPinhole camera0 = new CameraPinhole(500+rand.nextDouble()*10,510+rand.nextDouble()*10,0,450,400,900,800);

		DMatrixRMaj K0 = PerspectiveOps.pinholeToMatrix(camera0,(DMatrixRMaj)null);
		DMatrixRMaj P = new DMatrixRMaj(3,4);

		for (int i = 0; i < scene.views.length; i++) {
			Se3_F64 worldToView = new Se3_F64();

			worldToView.T.x = i*0.2 + rand.nextGaussian()*0.1;
			worldToView.T.y = -i*0.1+ rand.nextGaussian()*0.1;
			worldToView.T.z = rand.nextGaussian()*0.05;

			double rotX = rand.nextGaussian()*0.05;
			double rotY = rand.nextGaussian()*0.05;
			double rotZ = rand.nextGaussian()*0.05;

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,worldToView.R);
			PerspectiveOps.createCameraMatrix(worldToView.R,worldToView.T,K0,P);

			scene.setView(i,false,P);
		}

		Point2D_F64 pixel = new Point2D_F64();
		for (int i = 0; i < scene.points.length; i++) {
			// Point in world frame
			Point3D_F64 X = new Point3D_F64(rand.nextGaussian(),rand.nextGaussian(),3+rand.nextGaussian());
			if( scene.homogenous ) {
				scene.points[i].set(X.x, X.y, X.z, 1);
			} else {
				scene.points[i].set(X.x, X.y, X.z);
			}
			// Connect the point to views if it's visible inside of
			for (int j = 0; j < scene.views.length; j++) {
				PerspectiveOps.renderPixel(scene.views[j].worldToView,X,pixel);

				if( pixel.x >= 0 && pixel.y >= 0 && pixel.x < camera0.width && pixel.y < camera0.height ) {
					scene.connectPointToView(i,j);

					observations.getView(j).add(i,(float)pixel.x,(float)pixel.y);
				}
			}
		}

		return observations;
	}

}
