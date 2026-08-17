package com.z227.akatzumatool.common;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MathUtil {
    public static Matrix4f createViewMatrix(Camera camera) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();

        matrix.rotate(Axis.XP.rotationDegrees(camera.getXRot()));
        matrix.rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
//    matrix.rotate((float) Math.toRadians(rz), new Vector3f(0, 0, 1));
        Vec3 cameraPos = camera.getPosition();
        matrix.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        return matrix;
    }


    // 获取客户端时间，shader 和 CPU 随机闪烁都以秒为单位同步。
    public static float getClientTime(float partialTick) {
        if (Minecraft.getInstance().level == null) {
            return partialTick / 20.0f;
        }
        return (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0f;
    }
}
