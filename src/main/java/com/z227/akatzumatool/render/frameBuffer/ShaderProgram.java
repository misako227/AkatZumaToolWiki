package com.z227.akatzumatool.render.frameBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public abstract class ShaderProgram {


    private int programID;
    private int vertexShaderID;
    private int fragmentShaderID;

    private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(ResourceLocation vertexFile, ResourceLocation fragmentFile) {
        vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER);
        fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER);
        programID = glCreateProgram();
        glAttachShader(programID, vertexShaderID);
        glAttachShader(programID, fragmentShaderID);    //绑定着色器
        bindAttributes();  //绑定传给着色器的值
        glLinkProgram(programID);       //链接着色器
        glValidateProgram(programID);   //验证着色器
        getAllUniformLocations();    //获取所有uniform变量的位置
    }

    protected int getUniformLocation(String uniformName) {
        return glGetUniformLocation(programID, uniformName);
    }
    protected abstract void getAllUniformLocations();

    public void bindAttribute(int attribute, String name) {
        glBindAttribLocation(programID, attribute, name);
    }

    protected void bindAttributes() {

    }

    public void start() {
        glUseProgram(programID);
    }

    public void stop() {
        glUseProgram(0);
    }

    public void cleanUp() {
        stop();
        glDetachShader(programID, vertexShaderID);
        glDetachShader(programID, fragmentShaderID);
        glDeleteShader(vertexShaderID);
        glDeleteShader(fragmentShaderID);
        glDeleteProgram(programID);
    }

    private static int loadShader(ResourceLocation resource, int type) {
        String content = "";
        if (resource != null) {
            try {
                InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(resource).open();
                // 读取内容...
                content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }catch (IOException e) {
                System.err.println("AkatZumaTool Could not read file!");
                e.printStackTrace();
                System.exit(-1);
            }
        }
        int shaderID = GL30.glCreateShader(type);
        GL30.glShaderSource(shaderID, content);
        glCompileShader(shaderID);
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(shaderID, 500));
            System.err.println("Could not compile shader!");
            System.exit(-1);
        }
        return shaderID;
    }


    public void loadFloat(int location, float value) {
        GL20.glUniform1f(location, value);
    }

    public void loadVector(int location, Vector4f vector) {
        GL20.glUniform4f(location, vector.x, vector.y, vector.z, vector.w);
    }

    public void loadVector(int location, Vector3f vector) {
        GL20.glUniform3f(location, vector.x, vector.y, vector.z);
    }

    public void loadVector(int location, Vector2f vector) {
        GL20.glUniform2f(location, vector.x, vector.y);
    }

    public void loadInt(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    public void loadBoolean(int location, boolean value) {
        float toLoad = 0;
        if (value) {
            toLoad = 1;
        }
        GL20.glUniform1f(location, toLoad);
    }


    public void loadMatrix(int location, Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(location, false, matrixBuffer);
    }

}
