package mchorse.bbs_mod.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.graphics.InverseView;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

public class MatrixStackUtils
{
    private static Matrix3f normal = new Matrix3f();

    private static Matrix4f oldProjection = new Matrix4f();
    private static Matrix4f oldMV = new Matrix4f();
    private static Matrix3f oldInverse = new Matrix3f();

    public static void scaleStack(MatrixStack stack, float x, float y, float z)
    {
        stack.peek().getPositionMatrix().scale(x, y, z);
        stack.peek().getNormalMatrix().scale(x < 0F ? -1F : 1F, y < 0F ? -1F : 1F, z < 0F ? -1F : 1F);
    }

    public static void cacheMatrices()
    {
        /* Cache the global stuff */
        oldProjection.set(RenderSystem.getProjectionMatrix());
        oldMV.set(RenderSystem.getModelViewMatrix());
        /* Keep the view rotation holder across the ortho section, as the original 1.21.1 port does */
        oldInverse.set(InverseView.get());

        /* The modelview stack is a plain JOML Matrix4fStack since 1.21, and it carries no normal
         * matrix - the shader gets that one separately. */
        Matrix4fStack renderStack = RenderSystem.getModelViewStack();

        /* Pushed and left on the stack, exactly as the original 1.21.1 port does: the identity
         * modelview has to stay installed for the whole UI 3D pass and is popped by
         * restoreMatrices(). Popping it here made the next applyModelViewMatrix() read the
         * pre-UI matrix back. */
        renderStack.pushMatrix();
        renderStack.identity();
        RenderSystem.applyModelViewMatrix();
    }

    public static void restoreMatrices()
    {
        /* Return back to orthographic projection */
        RenderSystem.setProjectionMatrix(oldProjection, VertexSorter.BY_Z);
        InverseView.set(oldInverse);

        Matrix4fStack renderStack = RenderSystem.getModelViewStack();

        renderStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * 1.21 removed {@code RenderSystem}'s inverse view rotation matrix outright - there is no
     * global for shaders to read any more. It was the inverse of the camera rotation, which the
     * camera still hands out, so it is rebuilt from that.
     */
    public static Matrix3f getInverseViewRotationMatrix()
    {
        Quaternionf rotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();

        return new Matrix3f().rotation(rotation.conjugate(new Quaternionf()));
    }

    public static void applyTransform(MatrixStack stack, Transform transform)
    {
        stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);
        stack.multiply(transform.createRotation());
        scaleStack(stack, transform.scale.x, transform.scale.y, transform.scale.z);
    }

    public static void multiply(MatrixStack stack, Matrix4f matrix)
    {
        normal.set(matrix);
        normal.getScale(Vectors.TEMP_3F);

        Vectors.TEMP_3F.x = Vectors.TEMP_3F.x == 0F ? 0F : 1F / Vectors.TEMP_3F.x;
        Vectors.TEMP_3F.y = Vectors.TEMP_3F.y == 0F ? 0F : 1F / Vectors.TEMP_3F.y;
        Vectors.TEMP_3F.z = Vectors.TEMP_3F.z == 0F ? 0F : 1F / Vectors.TEMP_3F.z;

        normal.scale(Vectors.TEMP_3F);

        stack.peek().getPositionMatrix().mul(matrix);
        stack.peek().getNormalMatrix().mul(normal);
    }

    public static void scaleBack(MatrixStack matrices)
    {
        Matrix4f position = matrices.peek().getPositionMatrix();

        float scaleX = (float) Math.sqrt(position.m00() * position.m00() + position.m10() * position.m10() + position.m20() * position.m20());
        float scaleY = (float) Math.sqrt(position.m01() * position.m01() + position.m11() * position.m11() + position.m21() * position.m21());
        float scaleZ = (float) Math.sqrt(position.m02() * position.m02() + position.m12() * position.m12() + position.m22() * position.m22());

        float max = Math.max(scaleX, Math.max(scaleY, scaleZ));

        position.m00(position.m00() / max);
        position.m10(position.m10() / max);
        position.m20(position.m20() / max);

        position.m01(position.m01() / max);
        position.m11(position.m11() / max);
        position.m21(position.m21() / max);

        position.m02(position.m02() / max);
        position.m12(position.m12() / max);
        position.m22(position.m22() / max);
    }

    public static Matrix4f stripScale(Matrix4f matrix)
    {
        Matrix4f m = new Matrix4f(matrix);

        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        float sy = (float) Math.sqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        float sz = (float) Math.sqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());

        if (sx != 0F)
        {
            m.m00(m.m00() / sx);
            m.m01(m.m01() / sx);
            m.m02(m.m02() / sx);
        }

        if (sy != 0F)
        {
            m.m10(m.m10() / sy);
            m.m11(m.m11() / sy);
            m.m12(m.m12() / sy);
        }

        if (sz != 0F)
        {
            m.m20(m.m20() / sz);
            m.m21(m.m21() / sz);
            m.m22(m.m22() / sz);
        }

        return m;
    }
}
