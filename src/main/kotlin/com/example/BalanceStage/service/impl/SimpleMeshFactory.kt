package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.MeshFactory
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Rotate
import org.springframework.stereotype.Service
import kotlin.math.acos
import kotlin.math.sqrt

@Service
class SimpleMeshFactory : MeshFactory {

    override fun createPlaneMesh(width: Double, height: Double): TriangleMesh {
        val mesh = TriangleMesh()

        val w = width.toFloat() / 2f
        val h = height.toFloat() / 2f

        // 4 vertices
        mesh.points.addAll(
            -w, -h, 0f,  // 0: bottom-left
             w, -h, 0f,  // 1: bottom-right
             w,  h, 0f,  // 2: top-right
            -w,  h, 0f   // 3: top-left
        )

        // Texture coordinates (required)
        mesh.texCoords.addAll(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )

        // 2 triangles (6 indices)
        mesh.faces.addAll(
            0, 0, 1, 1, 2, 2,  // Triangle 1
            0, 0, 2, 2, 3, 3   // Triangle 2
        )

        return mesh
    }

    override fun createPlaneMeshView(
        width: Double,
        height: Double,
        normalVector: DoubleArray
    ): MeshView {
        val mesh = createPlaneMesh(width, height)
        val (nx, ny, nz) = normalVector

        return MeshView(mesh).apply {
            material = PhongMaterial(Color.color(0.5, 0.5, 0.5, 0.3))  // Gray, 30% opacity
            cullFace = CullFace.NONE  // Show both sides

            // Rotate plane to match normal vector
            val rotAxis = crossProduct(0.0, 0.0, 1.0, nx, ny, nz)
            val rotAngle = Math.toDegrees(acos(nz))

            if (rotAxis.magnitude() > 0.001) {
                transforms.add(Rotate(rotAngle, rotAxis.x, rotAxis.y, rotAxis.z))
            }
        }
    }

    private data class Vec3(val x: Double, val y: Double, val z: Double) {
        fun magnitude() = sqrt(x*x + y*y + z*z)
    }

    private fun crossProduct(ax: Double, ay: Double, az: Double,
                            bx: Double, by: Double, bz: Double): Vec3 {
        return Vec3(
            ay * bz - az * by,
            az * bx - ax * bz,
            ax * by - ay * bx
        )
    }
}
