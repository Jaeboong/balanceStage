package plot.geom

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** 현재 코드의 PCA 로직을 그대로 옮긴 구현체 */
class PcaPlaneEstimator : PlaneEstimator {

    override fun estimate(x: DoubleArray, y: DoubleArray, z: DoubleArray): PlaneModel {
        val n = x.size
        val cx = x.average(); val cy = y.average(); val cz = z.average()
        var cxx=0.0; var cxy=0.0; var cxz=0.0
        var cyy=0.0; var cyz=0.0; var czz=0.0
        for (i in 0 until n) {
            val dx = x[i]-cx; val dy = y[i]-cy; val dz = z[i]-cz
            cxx += dx*dx; cxy += dx*dy; cxz += dx*dz
            cyy += dy*dy; cyz += dy*dz; czz += dz*dz
        }
        val C = arrayOf(
            doubleArrayOf(cxx, cxy, cxz),
            doubleArrayOf(cxy, cyy, cyz),
            doubleArrayOf(cxz, cyz, czz)
        )

        val (eigVecs, eigVals) = eigenSymmetric3x3(C)
        val idx = eigVals.indices.minByOrNull { eigVals[it] }!!
        val normal = eigVecs[idx] // 이미 정규화됨
        val d = -(normal[0]*cx + normal[1]*cy + normal[2]*cz)
        return PlaneModel(normal, d, doubleArrayOf(cx, cy, cz))
    }

    override fun planeZ(
        normal: DoubleArray, d: Double,
        X: Array<DoubleArray>, Y: Array<DoubleArray>
    ): Array<DoubleArray> {
        val (nx, ny, nz) = normal
        return Array(X.size) { r ->
            DoubleArray(X[0].size) { c -> -(nx*X[r][c] + ny*Y[r][c] + d)/nz }
        }
    }

    /** 3x3 대칭행렬 고유분해 (야코비 회전) — 기존 코드 그대로 */
    private fun eigenSymmetric3x3(Ain: Array<DoubleArray>): Pair<Array<DoubleArray>, DoubleArray> {
        val A = Array(3) { Ain[it].clone() }
        val V = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
        fun off(): Double {
            val a01=A[0][1]; val a02=A[0][2]; val a12=A[1][2]
            return sqrt(a01*a01+a02*a02+a12*a12)
        }
        for (iter in 0 until 50) {
            if (off() < 1e-12) break
            var p=0; var q=1; var maxV=abs(A[0][1])
            val cands = arrayOf(Triple(0,2,abs(A[0][2])), Triple(1,2,abs(A[1][2])))
            for ((i,j,v) in cands) if (v>maxV){p=i;q=j;maxV=v}
            val app=A[p][p]; val aqq=A[q][q]; val apq=A[p][q]
            val tau=(aqq-app)/(2*apq)
            val t=if(tau>=0)1/(tau+sqrt(1+tau*tau))else 1/(tau-sqrt(1+tau*tau))
            val c=1/sqrt(1+t*t); val s=t*c
            val appN=c*c*app-2*c*s*apq+s*s*aqq
            val aqqN=s*s*app+2*c*s*apq+c*c*aqq
            A[p][p]=appN; A[q][q]=aqqN; A[p][q]=0.0; A[q][p]=0.0
            for (r in 0 until 3) {
                if (r!=p && r!=q) {
                    val arp=A[r][p]; val arq=A[r][q]
                    A[r][p]=c*arp-s*arq; A[p][r]=A[r][p]
                    A[r][q]=s*arp+c*arq; A[q][r]=A[r][q]
                }
            }
            for (r in 0 until 3) {
                val vrp=V[r][p]; val vrq=V[r][q]
                V[r][p]=c*vrp-s*vrq
                V[r][q]=s*vrp+c*vrq
            }
        }
        val eigVals = doubleArrayOf(A[0][0], A[1][1], A[2][2])
        val eigVecs = Array(3){i->doubleArrayOf(V[0][i],V[1][i],V[2][i])}
        for(i in 0..2){
            val nrm=sqrt(eigVecs[i][0].pow(2)+eigVecs[i][1].pow(2)+eigVecs[i][2].pow(2))
            if(nrm>0){eigVecs[i][0]/=nrm;eigVecs[i][1]/=nrm;eigVecs[i][2]/=nrm}
        }
        return eigVecs to eigVals
    }
}
