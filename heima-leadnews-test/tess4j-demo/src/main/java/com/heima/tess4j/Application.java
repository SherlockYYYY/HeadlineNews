package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;

public class Application {
    /**  识别图片中的文字
     * @param args
     */
    public static void main(String[] args) {
        //创建实例
        ITesseract tesseract = new Tesseract();

        //设置字体库路径
        tesseract.setDatapath("D:\\AA2025Summer\\heima-leadnews\\heima-leadnews-test\\tess4j-demo\\src\\main\\resources");
        //设置语言
        tesseract.setLanguage("chi_sim");

        //识别图片
        File file = new File("D:\\AA2025Summer\\heima-leadnews\\heima-leadnews-test\\tess4j-demo\\src\\main\\resources\\img.png");
        try {
            String result = tesseract.doOCR(file);
            System.out.println("识别的结果为"+result.replaceAll("\\r|\\n","-"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
