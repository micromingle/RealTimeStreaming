//package com.jiandan.terence.realtimevideotcp;
//
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//
//import java.nio.ByteBuffer;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
///**
// * Created by HP on 2018/4/28.
// */
//
//public class CustomRender implements GLSurfaceView.Renderer {
//    private static byte[] image; //The image buffer that will hold the camera image when preview callback arrives
//    private int[] yTextureNames;
//    private int[] uTextureNames;
//    private int[] vTextureNames;
//   // private Camera camera; //The camera object
//
//    //The Y and UV buffers that will pass our image channel data to the textures
//    private ByteBuffer yBuffer;
//    private ByteBuffer uvBuffer;
//    private int width,height;
//    //Our vertex shader code; nothing special
//    String vertexShader =
//            "attribute vec4 a_position;                         \n" +
//                    "attribute vec2 a_texCoord;                         \n" +
//                    "varying vec2 v_texCoord;                           \n" +
//
//                    "void main(){                                       \n" +
//                    "   gl_Position = a_position;                       \n" +
//                    "   v_texCoord = a_texCoord;                        \n" +
//                    "}                                                  \n";
//
//    //Our fragment shader code; takes Y,U,V values for each pixel and calculates R,G,B colors,
//    //Effectively making YUV to RGB conversion
//    String fragmentShader =
//            "#ifdef GL_ES                                       \n" +
//                    "precision highp float;                             \n" +
//                    "#endif                                             \n" +
//
//                    "varying vec2 v_texCoord;                           \n" +
//                    "uniform sampler2D y_texture;                       \n" +
//                    "uniform sampler2D uv_texture;                      \n" +
//
//                    "void main (void){                                  \n" +
//                    "   float r, g, b, y, u, v;                         \n" +
//
//                    //We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE,
//                    //that's why we're pulling it from the R component, we could also use G or B
//                    "   y = texture2D(y_texture, v_texCoord).r;         \n" +
//
//                    //We had put the U and V values of each pixel to the A and R,G,B components of the
//                    //texture respectively using GL_LUMINANCE_ALPHA. Since U,V bytes are interspread
//                    //in the texture, this is probably the fastest way to use them in the shader
//                    "   u = texture2D(uv_texture, v_texCoord).a - 0.5;  \n" +
//                    "   v = texture2D(uv_texture, v_texCoord).r - 0.5;  \n" +
//
//
//                    //The numbers are just YUV to RGB conversion constants
//                    "   r = y + 1.13983*v;                              \n" +
//                    "   g = y - 0.39465*u - 0.58060*v;                  \n" +
//                    "   b = y + 2.03211*u;                              \n" +
//
//                    //We finally set the RGB color of our pixel
//                    "   gl_FragColor = vec4(r, g, b, 1.0);              \n" +
//                    "}                                                  \n";
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//
//    }
//
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//
//    }
//
//    @Override
//    public void onDrawFrame(GL10 gl) {
//  /*
//         * Because of Java's limitations, we can't reference the middle of an array and
//         * we must copy the channels in our byte array into buffers before setting them to textures
//         */
//
//        //Copy the Y channel of the image into its buffer, the first (width*height) bytes are the Y channel
//        yBuffer.put(image, 0, width*height);
//        yBuffer.position(0);
//
//        //Copy the UV channels of the image into their buffer, the following (width*height/2) bytes are the UV channel; the U and V bytes are interspread
//        uvBuffer.put(image, width*height, width*height/2);
//        uvBuffer.position(0);
//
//        /*
//         * Prepare the Y channel texture
//         */
//
//        //Set texture slot 0 as active and bind our texture object to it
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE0);
//        GLES20.glUniform1i(GLES20.GL_TEXTURE0);
//        //yTexture.bind();
//
//        //Y texture is (width*height) in size and each pixel is one byte; by setting GL_LUMINANCE, OpenGL puts this byte into R,G and B components of the texture
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, 1280, 720, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
//
//        //Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//
//        /*
//         * Prepare the UV channel texture
//         */
//
//        //Set texture slot 1 as active and bind our texture object to it
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        uvTexture.bind();
//
//        //UV texture is (width/2*height/2) in size (downsampled by 2 in both dimensions, each pixel corresponds to 4 pixels of the Y channel)
//        //and each pixel is two bytes. By setting GL_LUMINANCE_ALPHA, OpenGL puts first byte (V) into R,G and B components and of the texture
//        //and the second byte (U) into the A component of the texture. That's why we find U and V at A and R respectively in the fragment shader code.
//        //Note that we could have also found V at G or B as well.
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, 1280/2, 720/2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvBuffer);
//
//        //Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//        /*
//         * Draw the textures onto a mesh using our shader
//         */
//
//        shader.begin();
//
//        //Set the uniform y_texture object to the texture at slot 0
//        shader.setUniformi("y_texture", 0);
//
//        //Set the uniform uv_texture object to the texture at slot 1
//        shader.setUniformi("uv_texture", 1);
//
//        //Render our mesh using the shader, which in turn will use our textures to render their content on the mesh
//       // mesh.render(shader, GLES20.GL_TRIANGLES);
//       // shader.end();
//    }
//}
