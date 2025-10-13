package plot

import javafx.application.Application
import javafx.scene.*
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import javafx.stage.Stage
import kotlin.math.*

// =======================================================
//  PCA 평균평면 + 컬러 등고선(무지개, devi 기반) 텍스처 오버레이
// =======================================================
class BalanceSurfaceApp : Application() {

    // [x, y, z(평면용), devi(색/등고선용)]
    private val points = arrayOf(
        doubleArrayOf(0.0,   120.0, 1.95,  2.0),
        doubleArrayOf(0.0,     0.0, 1.654, 4.0),
        doubleArrayOf(121.0,   0.0, 1.798, 6.0),
    )

    override fun start(primaryStage: Stage) {
        val sigmaScale = 0.50
        val lam = 1e-9
        val gridRes = 260
        val centerOrigin = true
        val padRatio = 0.1
        val zScale = 20.0
        val nContours = 24
        val contourColor = Color.BLACK
        val contourPx = 2
        val forceVminZero = true

        val x = DoubleArray(points.size) { points[it][0] }
        val y = DoubleArray(points.size) { points[it][1] }
        val z = DoubleArray(points.size) { points[it][2] }
        val v = DoubleArray(points.size) { points[it][3] }

        // === 그리드 (유틸 함수 복구됨) ===
        val (xi, yi) = computeGrid(x, y, gridRes, centerOrigin, padRatio)
        val (Xg, Yg) = meshgrid(xi, yi)

        // === RBF 보간 (devi 기반 색상필드) ===
        val sigma = pickSigmaFromPoints(x, y, sigmaScale)
        val w = rbfGaussianWeights(x, y, v, sigma, lam)
        val Zg = rbfGaussianEval(w, x, y, Xg, Yg, sigma)

        val (vmin, vmax) = if (forceVminZero)
            0.0 to max(v.maxOrNull() ?: 1.0, 1e-9)
        else {
            val zmin = Zg.min2D(); val zmax = Zg.max2D()
            zmin to if (abs(zmax - zmin) < 1e-9) zmin + 1 else zmax
        }

        // === PCA 평균평면 ===
        val (normal, d, _) = fitPlanePCA(x, y, z)
        val (nx, ny, nz) = normal
        val Pg = planeGridFromNormal(normal, d, Xg, Yg)   // z = -(nx x + ny y + d)/nz

        // === 텍스처(무지개 히트맵 + 등고선) ===
        val tex = makeHeatmapWithContours(Zg, vmin, vmax, nContours, contourPx, contourColor)

        // === 평면 메쉬 ===
        val mesh = buildPlaneMeshWithTexture(Xg, Yg, Pg, zScale)
        val mat = PhongMaterial().apply {
            diffuseMap = tex
            diffuseColor = Color.WHITE
            specularColor = Color.BLACK
        }
        val surface = MeshView(mesh).apply {
            material = mat
            cullFace = CullFace.NONE
            drawMode = DrawMode.FILL
        }

        // === 측정점 ===
        val markers = Group().apply {
            for (i in points.indices) {
                val px = points[i][0]; val py = points[i][1]
                val zOnPlane = -(nx * px + ny * py + d) / nz
                val yUp = zOnPlane * zScale + 0.5
                children += makeSphere(px, py, yUp, 2.5, Color.BLACK)
            }
        }

        val axes = buildAxes(max(xi.last() - xi.first(), yi.last() - yi.first()).toFloat())
        val ambient = AmbientLight(Color.color(0.95, 0.95, 0.98))
        val world = Group(surface, markers, axes, ambient)

        // === SubScene / 카메라 ===
        val subScene = SubScene(world, 1200.0, 900.0, true, SceneAntialiasing.BALANCED)
        val camera = PerspectiveCamera(true).apply {
            nearClip = 0.1
            farClip = 20000.0
            fieldOfView = 35.0
            transforms.addAll(
                Rotate(-35.0, Rotate.X_AXIS),
                Rotate(-55.0, Rotate.Y_AXIS),
                Translate(0.0, 0.0, -getCameraDistance(xi, yi, Pg, zScale))
            )
        }
        subScene.camera = camera
        subScene.isFocusTraversable = true
        subScene.requestFocus()
        SimpleOrbitControls(world, subScene, camera)

        // === Scene ===
        val rootPane = StackPane(subScene)
        val scene = Scene(rootPane, 1200.0, 900.0)
        subScene.widthProperty().bind(scene.widthProperty())
        subScene.heightProperty().bind(scene.heightProperty())

        primaryStage.title = "PCA Average Plane + Rainbow Contours (devi)"
        primaryStage.scene = scene
        primaryStage.show()
    }

    // -------------------- PCA 평면 --------------------
    private fun fitPlanePCA(x: DoubleArray, y: DoubleArray, z: DoubleArray): Triple<DoubleArray, Double, DoubleArray> {
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
        val normal = eigVecs[idx]
        val d = -(normal[0]*cx + normal[1]*cy + normal[2]*cz)
        return Triple(normal, d, doubleArrayOf(cx, cy, cz))
    }

    // 3x3 대칭행렬 고유분해 (야코비 회전)
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

    private fun planeGridFromNormal(n: DoubleArray, d: Double,
                                    X: Array<DoubleArray>, Y: Array<DoubleArray>): Array<DoubleArray> {
        val (nx, ny, nz) = n
        return Array(X.size) { r ->
            DoubleArray(X[0].size) { c -> -(nx*X[r][c] + ny*Y[r][c] + d)/nz }
        }
    }

    // -------------------- RBF --------------------
    private fun pickSigmaFromPoints(x: DoubleArray, y: DoubleArray, scale: Double): Double {
        var dmin = Double.POSITIVE_INFINITY
        for(i in x.indices) for(j in i+1 until x.size)
            dmin = min(dmin, hypot(x[i]-x[j], y[i]-y[j]))
        return max(dmin*scale, 1e-6)
    }

    private fun rbfGaussianWeights(x: DoubleArray, y: DoubleArray, v: DoubleArray,
                                   sigma: Double, lam: Double): DoubleArray {
        val n=x.size; val K=Array(n){DoubleArray(n)}; val s2=sigma*sigma
        for(i in 0 until n){
            for(j in 0 until n){
                val d2=(x[i]-x[j]).pow(2)+(y[i]-y[j]).pow(2)
                K[i][j]=exp(-d2/s2)
            }
            K[i][i]+=lam
        }
        return solve(K,v)
    }

    private fun rbfGaussianEval(w: DoubleArray,xPts:DoubleArray,yPts:DoubleArray,
                                Xg:Array<DoubleArray>,Yg:Array<DoubleArray>,sigma:Double):Array<DoubleArray>{
        val rows=Xg.size; val cols=Xg[0].size
        val Z=Array(rows){DoubleArray(cols)}; val s2=sigma*sigma
        for(j in w.indices){
            val xj=xPts[j]; val yj=yPts[j]; val wj=w[j]
            for(r in 0 until rows){
                val Xr=Xg[r]; val Yr=Yg[r]; val Zr=Z[r]
                for(c in 0 until cols){
                    val d2=(Xr[c]-xj).pow(2)+(Yr[c]-yj).pow(2)
                    Zr[c]+=wj*exp(-d2/s2)
                }
            }
        }
        return Z
    }

    // -------------------- 유틸 (누락되었던 부분 복구) --------------------
    private fun solve(Ain:Array<DoubleArray>,bin:DoubleArray):DoubleArray{
        val n=bin.size; val A=Array(n){Ain[it].clone()}; val b=bin.clone()
        for(p in 0 until n){
            var maxI=p
            for(i in p+1 until n) if(abs(A[i][p])>abs(A[maxI][p])) maxI=i
            val tmp=A[p];A[p]=A[maxI];A[maxI]=tmp
            val tb=b[p];b[p]=b[maxI];b[maxI]=tb
            val piv=A[p][p]; if(abs(piv)<1e-12) continue
            for(j in p until n) A[p][j]/=piv; b[p]/=piv
            for(i in 0 until n) if(i!=p){
                val a=A[i][p]
                if(a!=0.0){for(j in p until n)A[i][j]-=a*A[p][j]; b[i]-=a*b[p]}
            }
        }
        return b
    }

    private fun computeGrid(
        x: DoubleArray, y: DoubleArray, res: Int, centerOrigin: Boolean, pad: Double
    ): Pair<DoubleArray, DoubleArray> {
        val xmin = x.minOrNull() ?: -1.0; val xmax = x.maxOrNull() ?: 1.0
        val ymin = y.minOrNull() ?: -1.0; val ymax = y.maxOrNull() ?: 1.0
        val xs: DoubleArray
        val ys: DoubleArray
        if (centerOrigin) {
            val Rx = max(abs(xmin), abs(xmax)) * (1 + pad)
            val Ry = max(abs(ymin), abs(ymax)) * (1 + pad)
            xs = linspace(-Rx, Rx, res)
            ys = linspace(-Ry, Ry, res)
        } else {
            val dx = (xmax - xmin) * pad
            val dy = (ymax - ymin) * pad
            xs = linspace(xmin - dx, xmax + dx, res)
            ys = linspace(ymin - dy, ymax + dy, res)
        }
        return xs to ys
    }

    private fun linspace(a: Double, b: Double, n: Int): DoubleArray {
        if (n <= 1) return doubleArrayOf(a)
        val step = (b - a) / (n - 1)
        return DoubleArray(n) { i -> a + i * step }
    }

    private fun meshgrid(x: DoubleArray, y: DoubleArray): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val rows = y.size; val cols = x.size
        val X = Array(rows) { DoubleArray(cols) }
        val Y = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) for (j in 0 until cols) { X[i][j] = x[j]; Y[i][j] = y[i] }
        return X to Y
    }

    private fun Array<DoubleArray>.min2D():Double=this.minOf{it.min()}
    private fun Array<DoubleArray>.max2D():Double=this.maxOf{it.max()}

    // -------------------- 텍스처(히트맵 + 등고선) --------------------
    private fun makeHeatmapWithContours(
        Z: Array<DoubleArray>,
        vmin: Double,
        vmax: Double,
        nContours: Int,
        lineWidthPx: Int,
        lineColor: Color
    ): WritableImage {
        val rows = Z.size
        val cols = Z[0].size
        val img = WritableImage(cols, rows)
        val buf = IntArray(cols * rows)

        // 1) 히트맵(무지개 컬러맵)
        val dv = max(vmax - vmin, 1e-9)
        var k = 0
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val t = ((Z[i][j] - vmin) / dv).coerceIn(0.0, 1.0)
                val (R,G,B) = rainbowRGB(t)
                buf[k++] = (255 shl 24) or (R shl 16) or (G shl 8) or B
            }
        }

        // 2) 등고선 (Marching Squares → 선분을 텍스처에 래스터)
        val levels = DoubleArray(nContours) { idx ->
            val a = (idx + 1).toDouble() / (nContours + 1).toDouble()
            vmin + a * (vmax - vmin)
        }
        val argbLine = ((255) shl 24) or
                (((lineColor.red  *255).toInt()) shl 16) or
                (((lineColor.green*255).toInt()) shl 8 ) or
                (((lineColor.blue *255).toInt()))

        for (L in levels) {
            for (i in 0 until rows - 1) {
                for (j in 0 until cols - 1) {
                    val v00 = Z[i][j];   val v10 = Z[i][j+1]
                    val v11 = Z[i+1][j+1]; val v01 = Z[i+1][j]
                    fun interp(a: Double, b: Double, t0: Double, t1: Double): Double {
                        val d = (t1 - t0)
                        return if (abs(d) < 1e-12) (a + b) * 0.5 else a + (b - a) * ((L - t0) / d)
                    }
                    val xL = j.toDouble(); val xR = (j+1).toDouble()
                    val yT = i.toDouble(); val yB = (i+1).toDouble()
                    val pLeft   = if ((v00 - L)*(v01 - L) <= 0)  Pair(xL, interp(yT, yB, v00, v01)) else null
                    val pRight  = if ((v10 - L)*(v11 - L) <= 0)  Pair(xR, interp(yT, yB, v10, v11)) else null
                    val pTop    = if ((v00 - L)*(v10 - L) <= 0)  Pair(interp(xL, xR, v00, v10), yT) else null
                    val pBottom = if ((v01 - L)*(v11 - L) <= 0)  Pair(interp(xL, xR, v01, v11), yB) else null

                    val pts = arrayOf(pLeft, pTop, pRight, pBottom).filterNotNull()
                    if (pts.size >= 2) {
                        drawLineOnBuffer(buf, cols, rows, pts[0], pts[1], argbLine, lineWidthPx)
                        if (pts.size == 4) {
                            drawLineOnBuffer(buf, cols, rows, pts[2], pts[3], argbLine, lineWidthPx)
                        }
                    }
                }
            }
        }

        img.pixelWriter.setPixels(0, 0, cols, rows, PixelFormat.getIntArgbInstance(), buf, 0, cols)
        return img
    }

    private fun drawLineOnBuffer(
        buf: IntArray, w: Int, h: Int,
        p0: Pair<Double, Double>, p1: Pair<Double, Double>,
        argb: Int, thickness: Int
    ) {
        var x0 = p0.first; var y0 = p0.second
        var x1 = p1.first; var y1 = p1.second
        val dx = x1 - x0; val dy = y1 - y0
        val steps = max(abs(dx), abs(dy))
        if (steps < 1e-9) return
        val sx = dx / steps; val sy = dy / steps
        var x = x0; var y = y0
        val half = max(1, thickness) / 2
        for (i in 0..steps.toInt()) {
            val xi = x.roundToInt()
            val yi = y.roundToInt()
            for (oy in -half..half) for (ox in -half..half) {
                val xx = xi + ox; val yy = yi + oy
                if (xx in 0 until w && yy in 0 until h) buf[yy * w + xx] = argb
            }
            x += sx; y += sy
        }
    }

    // -------------------- 무지개 컬러맵 (HSV) --------------------
    private fun rainbowRGB(tIn: Double): Triple<Int, Int, Int> {
        val t = tIn.coerceIn(0.0, 1.0)
        val hue = 240.0 * (1.0 - t)            // 240°(blue) → 0°(red)
        val (r, g, b) = hsvToRgb(hue, 1.0, 1.0)
        return Triple((r * 255 + 0.5).toInt(), (g * 255 + 0.5).toInt(), (b * 255 + 0.5).toInt())
    }
    private fun hsvToRgb(h: Double, s: Double, v: Double): Triple<Double, Double, Double> {
        if (s <= 1e-12) return Triple(v, v, v)
        val hh = ((h % 360.0) + 360.0) % 360.0
        val c = v * s
        val x = c * (1 - abs((hh / 60.0) % 2 - 1))
        val m = v - c
        val (rp, gp, bp) = when {
            hh < 60  -> Triple(c, x, 0.0)
            hh < 120 -> Triple(x, c, 0.0)
            hh < 180 -> Triple(0.0, c, x)
            hh < 240 -> Triple(0.0, x, c)
            hh < 300 -> Triple(x, 0.0, c)
            else     -> Triple(c, 0.0, x)
        }
        return Triple(rp + m, gp + m, bp + m)
    }

    // -------------------- 메쉬/카메라 --------------------
    private fun buildPlaneMeshWithTexture(
        X: Array<DoubleArray>, Y: Array<DoubleArray>, P: Array<DoubleArray>, zScale: Double
    ): TriangleMesh {
        val rows = X.size
        val cols = X[0].size
        val mesh = TriangleMesh()
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val xf = X[i][j].toFloat()
                val yUp = (P[i][j] * zScale).toFloat()
                val zf = Y[i][j].toFloat()
                mesh.points.addAll(xf, yUp, zf)
            }
        }
        for (i in 0 until rows) {
            val v = i.toFloat() / (rows - 1).toFloat()
            for (j in 0 until cols) {
                val u = j.toFloat() / (cols - 1).toFloat()
                mesh.texCoords.addAll(u, v)
            }
        }
        fun vid(i: Int, j: Int) = i * cols + j
        for (i in 0 until rows - 1) for (j in 0 until cols - 1) {
            val v00 = vid(i, j)
            val v01 = vid(i, j + 1)
            val v10 = vid(i + 1, j)
            val v11 = vid(i + 1, j + 1)
            mesh.faces.addAll(v00, v00,  v10, v10,  v11, v11)
            mesh.faces.addAll(v00, v00,  v11, v11,  v01, v01)
        }
        return mesh
    }

    private fun buildAxes(length: Float): Group {
        val ax = javafx.scene.shape.Box(length.toDouble(), 0.5, 0.5).apply { material = PhongMaterial(Color.RED) }
        val ay = javafx.scene.shape.Box(0.5, length.toDouble(), 0.5).apply { material = PhongMaterial(Color.BLUE) }  // Up(Y)
        val az = javafx.scene.shape.Box(0.5, 0.5, length.toDouble()).apply { material = PhongMaterial(Color.GREEN) }
        return Group(ax, ay, az)
    }

    private fun makeSphere(x: Double, z: Double, yUp: Double, radius: Double, color: Color): Node {
        return javafx.scene.shape.Sphere(radius).apply {
            material = PhongMaterial(color)
            translateX = x
            translateZ = z
            translateY = yUp
            drawMode = DrawMode.FILL
        }
    }

    private fun getCameraDistance(xi: DoubleArray, yi: DoubleArray, Pg: Array<DoubleArray>, zScale: Double): Double {
        val dx = xi.last() - xi.first()
        val dz = yi.last() - yi.first()
        var minP = Pg[0][0]; var maxP = Pg[0][0]
        for (r in Pg.indices) for (c in Pg[r].indices) {
            minP = min(minP, Pg[r][c]); maxP = max(maxP, Pg[r][c])
        }
        val dy = (maxP - minP) * zScale
        return max(dx, max(dz, dy)) * 2.5 + 400.0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(BalanceSurfaceApp::class.java)
        }
    }
}

/** 마우스 드래그로 회전/줌 */
private class SimpleOrbitControls(
    private val target: Group,
    private val scene: SubScene,
    private val cam: Camera
) {
    private var anchorX = 0.0
    private var anchorY = 0.0
    private var anchorYaw = 0.0
    private var anchorPitch = 0.0
    private val rotX = Rotate(-35.0, Rotate.X_AXIS)
    private val rotY = Rotate(-55.0, Rotate.Y_AXIS)
    private var distance = 800.0

    init {
        target.transforms.addAll(rotX, rotY, Translate(0.0, 0.0, 0.0))
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            anchorX = it.sceneX
            anchorY = it.sceneY
            anchorYaw = rotY.angle
            anchorPitch = rotX.angle
        }
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED) {
            val dx = it.sceneX - anchorX
            val dy = it.sceneY - anchorY
            rotY.angle = anchorYaw + dx * 0.3
            rotX.angle = (anchorPitch - dy * 0.3).coerceIn(-89.0, 89.0)
        }
        scene.setOnScroll {
            distance = (distance + it.deltaY * 0.5).coerceIn(150.0, 4000.0)
            cam.transforms.removeIf { t -> t is Translate }
            cam.transforms.add(Translate(0.0, 0.0, -distance))
        }
    }
}
