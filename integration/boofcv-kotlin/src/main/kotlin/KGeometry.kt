package boofcv.kotlin

import georegression.geometry.ConvertRotation3D_F64
import georegression.geometry.GeometryMath_F64
import georegression.struct.EulerType
import georegression.struct.point.Point2D_F64
import georegression.struct.point.Point3D_F32
import georegression.struct.point.Point3D_F64
import georegression.struct.se.Se3_F32
import georegression.struct.se.Se3_F64
import georegression.struct.so.Quaternion_F64
import georegression.struct.so.Rodrigues_F64
import georegression.transform.se.SePointOps_F32
import georegression.transform.se.SePointOps_F64
import org.ejml.data.DMatrixRMaj

fun DMatrixRMaj.times(a : Point2D_F64, out : Point2D_F64) { GeometryMath_F64.mult(this,a, out)}
fun DMatrixRMaj.times( a : Point2D_F64) : Point2D_F64
{ val out = Point2D_F64();GeometryMath_F64.mult(this,a, out);return out}
fun DMatrixRMaj.times(a : Point2D_F64, out : Point3D_F64) { GeometryMath_F64.mult(this,a, out)}

fun Point2D_F64.asHomogenous(): Point3D_F64 = Point3D_F64(this.x,this.y,1.0)
fun Point2D_F64.asHomogenous(out : Point3D_F64): Point3D_F64 { out.set(this.x,this.y,1.0); return out}

fun Point3D_F32.transform( t : Se3_F32 ) { SePointOps_F32.transform(t,this,this)}
fun Point3D_F32.transform( t : Se3_F32 , dst : Point3D_F32) { SePointOps_F32.transform(t,this,dst)}
fun Point3D_F64.transform( t : Se3_F64 ) { SePointOps_F64.transform(t,this,this)}
fun Point3D_F64.transform( t : Se3_F64 , dst : Point3D_F64) { SePointOps_F64.transform(t,this,dst)}

fun DMatrixRMaj.asRodrigues( out : Rodrigues_F64? = null ) : Rodrigues_F64 = ConvertRotation3D_F64.matrixToRodrigues(this,out)
fun DMatrixRMaj.asQuaternion( out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.matrixToQuaternion(this,out)
fun DMatrixRMaj.asEuler( type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.matrixToEuler(this,type,out)

fun Rodrigues_F64.asMatrix( out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.rodriguesToMatrix(this,out)
fun Rodrigues_F64.asQuaternion( out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.rodriguesToQuaternion(this,out)
fun Rodrigues_F64.asEuler(type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.rodriguesToEuler(this,type,out)

fun Quaternion_F64.asMatrix( out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.quaternionToMatrix(this,out)
fun Quaternion_F64.asRodrigues( out : Rodrigues_F64? = null ) : Rodrigues_F64 = ConvertRotation3D_F64.quaternionToRodrigues(this,out)
fun Quaternion_F64.asEuler(type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.quaternionToEuler(this,type,out)

fun DoubleArray.asMatrix(type: EulerType, out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.eulerToMatrix(type,this[0],this[1],this[2],out)
fun DoubleArray.asRodrigues(type: EulerType, out : Rodrigues_F64? = null) : Rodrigues_F64 = this.asMatrix(type).asRodrigues(out)
fun DoubleArray.asQuaternion(type: EulerType, out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.eulerToQuaternion(type,this[0],this[1],this[2],out)
