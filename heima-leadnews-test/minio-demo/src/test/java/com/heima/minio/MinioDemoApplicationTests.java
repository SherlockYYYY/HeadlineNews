package com.heima.minio;

import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = MinioDemoApplication.class)
@RunWith(SpringRunner.class)
class MinioDemoApplicationTests {

    @Autowired
    private FileStorageService fileStorageService;

//    @Test
//    public void testUpdateImgFile() {
//        try {
//            FileInputStream fileInputStream = new FileInputStream("E:\\tmp\\ak47.jpg");
//            String filePath = fileStorageService.uploadImgFile("", "ak47.jpg", fileInputStream);
//            System.out.println(filePath);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("D:\\AA2025Summer\\heima-leadnews\\heima-leadnews-test\\freemarker-demo\\src\\main\\resources\\static\\02-list.html");
        String filePath = fileStorageService.uploadHtmlFile("", "02-list.html", fileInputStream);
        System.out.println(filePath);

    }

}
