//package com.jiandan.terence.realtimevideotcp;
//
//import android.opengl.EGLConfig;
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.util.Log;
//
//import java.nio.ByteBuffer;
//import java.nio.FloatBuffer;
//
//import javax.microedition.khronos.opengles.GL10;
//
///**
// * Created by HP on 2018/4/27.
// */
//
//public class MyRenerer2 implements GLSurfaceView.Renderer {
//    private int programHandleMain;
//    private int aPositionMain;
//    private int aTexCoordMain;
//    private int uYTextureMain,uUVTextureMain;
//    private int[] Ytexture=new int[1];
//    private int[] UVtexture=new int[1];
//    private ByteBuffer uvBuf,yBuf;
//    //private int vTexture;
//    private int viewWidth,viewHeight,frameWidth,frameHeight;
//    public static String VERTEX_SHADER =
//            "attribute vec4 vPosition;    \n" +
//                    "attribute vec2 a_texCoord;   \n" +
//                    "varying vec2 tc;             \n" +
//                    "void main()                  \n" +
//                    "{                            \n" +
//                    "   gl_Position = vPosition;  \n" +
//                    "   tc = a_texCoord;          \n" +
//                    "}                            \n";
//
//
//    public static String FRAG_SHADER =
//            "precision mediump float;\n" +
//                    "varying  vec2 tc;                      \n" +
//                    "uniform sampler2D SamplerY;            \n" +
//                    "uniform sampler2D SamplerUV;            \n" +
//                    "const float PI = 3.14159265;           \n" +
//                    "const mat3 convertMat = mat3( 1.0, 1.0, 1.0, 0.0, -0.39465, 2.03211, 1.13983, -0.58060, 0.0 );\n" +
//                    "void main(void)                            \n" +
//                    "{                                          \n" +
//                    "vec3 yuv;                                  \n" +
//                    "yuv.x = texture2D(SamplerY, tc).r;         \n" +
//                    "yuv.z = texture2D(SamplerUV, tc).r - 0.5;   \n" +
//                    "yuv.y = texture2D(SamplerUV, tc).a - 0.5;   \n" +
//                    "vec3 color = convertMat * yuv;             \n" +
//                    "vec4 mainColor = vec4(color, 1.0);         \n" +
//                    "gl_FragColor =mainColor;                                       \n" +
//                    "}                                                              \n";
//    //设置opengl 渲染的坐标系统，从[-1，1]
//    public static float squareVertices[] = {  //rotate 90
//            1.0f, 1.0f,//rt
//            1.0f, -1.0f,//rb
//            -1.0f, 1.0f,//lt
//            -1.0f, -1.0f,//lb
//    };
//    //设置纹理基本坐标
//    public static float coordVertices[] = {
//            0.0f, 0.0f,//lb
//            1.0f, 0.0f,//rb
//            0.0f, 1.0f,//lt
//            1.0f, 1.0f,//rt
//    };
//    private FloatBuffer mVertices;
//    // 创建纹理
//    private void createTexture(int width, int height, int format, int[] textureId) {
//        //创建纹理
//        GLES20.glGenTextures(1, textureId, 0);
//        //绑定纹理
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
//        //设置纹理属性
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
//    }
//
//    public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
//        int vertexShader;
//        int fragmentShader;
//        int programObject;
//        int[] linked = new int[1];
//
//        // Load the vertex/fragment shaders
//        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
//        if (vertexShader == 0) {
//            return 0;
//        }
//
//        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
//        if (fragmentShader == 0) {
//            GLES20.glDeleteShader(vertexShader);
//            return 0;
//        }
//
//        // Create the program object
//        programObject = GLES20.glCreateProgram();
//
//        if (programObject == 0) {
//            return 0;
//        }
//
//        GLES20.glAttachShader(programObject, vertexShader);
//        GLES20.glAttachShader(programObject, fragmentShader);
//
//        // Link the program
//        GLES20.glLinkProgram(programObject);
//
//        // Check the link status
//        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);
//
//        if (linked[0] == 0) {
//            Log.e("ESShader", "Error linking program:");
//            Log.e("ESShader", GLES20.glGetProgramInfoLog(programObject));
//            GLES20.glDeleteProgram(programObject);
//            return 0;
//        }
//
//        // Free up no longer needed shader resources
//        GLES20.glDeleteShader(vertexShader);
//        GLES20.glDeleteShader(fragmentShader);
//
//        return programObject;
//    }
//
//    public static int loadShader(int type, String shaderSrc) {
//        int shader;
//        int[] compiled = new int[1];
//
//        // Create the shader object
//        shader = GLES20.glCreateShader(type);
//        if (shader == 0) {
//            return 0;
//        }
//        // Load the shader source
//        GLES20.glShaderSource(shader, shaderSrc);
//        // Compile the shader
//        GLES20.glCompileShader(shader);
//        // Check the compile status
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
//
//        if (compiled[0] == 0) {
//            Log.e("ESShader", GLES20.glGetShaderInfoLog(shader));
//            GLES20.glDeleteShader(shader);
//            return 0;
//        }
//        return shader;
//    }
//
//    //GLSurface Render 子类相关部分
//    private void InitShader() {
//        programHandleMain = loadProgram(VERTEX_SHADER,FRAG_SHADER);
//        if (programHandleMain != -1) {
//            //启动纹理
//            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//            // 获取VertexShader变量
//            //aPositionMain = getShaderHandle(programHandleMain, "vPosition");
//            //aTexCoordMain = getShaderHandle(programHandleMain, "a_texCoord");
//            aPositionMain = GLES20.glGetAttribLocation(programHandleMain, "a_position");
//            aTexCoordMain = GLES20.glGetAttribLocation(programHandleMain, "a_texCoord");
//            // 获取FrameShader变量
//            uYTextureMain = GLES20.glGetAttribLocation(programHandleMain, "SamplerY");
//            uUVTextureMain = GLES20.glGetAttribLocation(programHandleMain, "SamplerUV");
//            // 使用滤镜着色器程序
//            GLES20.glUseProgram(programHandleMain);
//            //给变量赋值
//            GLES20.glUniform1i(uYTextureMain, 0);
//            GLES20.glUniform1i(uUVTextureMain, 1);
//            GLES20.glEnableVertexAttribArray(aPositionMain);
//            GLES20.glEnableVertexAttribArray(aTexCoordMain);
//            // 设置Vertex Shader数据
//            squareVertices.position(0);
//            GLES20.glVertexAttribPointer(aPositionMain, 2, GLES20.GL_FLOAT, false, 0, squareVertices);
//            coordVertices.position(0);
//            GLES20.glVertexAttribPointer(aTexCoordMain, 2, GLES20.GL_FLOAT, false, 0, coordVertices);
//            //创建yuv纹理
//            createTexture(frameWidth, frameHeight, GLES20.GL_LUMINANCE, Ytexture);
//            createTexture(frameWidth >> 1, frameHeight >> 1, GLES20.GL_LUMINANCE_ALPHA, UVtexture);
//        }
//    }
//
//    public void onDrawFrame(GL10 unused) {
//        // 重绘背景色
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        if (yBuf != null) {
//            //y
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Ytexture[0]);
//            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
//                    0,
//                    0,
//                    0,
//                    frameWidth,
//                    frameHeight,
//                    GLES20.GL_LUMINANCE,
//                    GLES20.GL_UNSIGNED_BYTE,
//                    yBuf);
//            //uv
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, UVtexture[0]);
//            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
//                    0,
//                    0,
//                    0,
//                    frameWidth >> 1,
//                    frameHeight >> 1,
//                    GLES20.GL_LUMINANCE_ALPHA,
//                    GLES20.GL_UNSIGNED_BYTE,
//                    uvBuf);
//        }
//        //绘制
//        GLES20.glViewport(0, 0, viewWidth, viewHeight);
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//    }
//
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
//        //设置背景的颜色为黑色
//        GLES20.glClearColor(0.f, 0.f, 0.f, 1.0f);
//        InitShader();
//    }
//
//    public void onSurfaceChanged(GL10 unused, int width, int height) {
//        viewWidth = width;
//        viewHeight = height;
//        GLES20.glViewport(0, 0, viewWidth, viewHeight);
//    }
//}