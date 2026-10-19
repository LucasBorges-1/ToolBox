package com.toolbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.Loader;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.zip.ZipInputStream;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class ToolsTest {
 @Autowired MockMvc mvc;
 byte[] pdf() throws Exception {try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.addPage(new PDPage());doc.save(out);return out.toByteArray();}}
 @Test void homeAndHealth() throws Exception {mvc.perform(get("/")).andExpect(status().isOk());mvc.perform(get("/api/health")).andExpect(jsonPath("$.status").value("ok"));}
 @Test void mergeAndExtract() throws Exception {var a=new MockMultipartFile("files","a.pdf","application/pdf",pdf());var bytes=mvc.perform(multipart("/api/pdf/merge").file(a).file(a)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();try(var doc=Loader.loadPDF(bytes)){assertEquals(4,doc.getNumberOfPages());}bytes=mvc.perform(multipart("/api/pdf/split").file(new MockMultipartFile("file",pdf())).param("page","2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();try(var doc=Loader.loadPDF(bytes)){assertEquals(1,doc.getNumberOfPages());}}
 @Test void invalidPageAndFile() throws Exception {mvc.perform(multipart("/api/pdf/split").file(new MockMultipartFile("file",pdf())).param("page","9")).andExpect(status().isBadRequest());mvc.perform(multipart("/api/image/convert").file(new MockMultipartFile("file",new byte[]{1,2}))).andExpect(status().isBadRequest());}
 @Test void qrRoundTrip() throws Exception {var bytes=mvc.perform(multipart("/api/qr").param("text","Olá ToolBox")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();var image=ImageIO.read(new ByteArrayInputStream(bytes));var decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))));assertEquals("Olá ToolBox",decoded.getText());}
 @Test void resize() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(100,50,BufferedImage.TYPE_INT_RGB),"png",out);var bytes=mvc.perform(multipart("/api/image/convert").file(new MockMultipartFile("file",out.toByteArray())).param("width","40").param("format","jpg")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();var image=ImageIO.read(new ByteArrayInputStream(bytes));assertEquals(40,image.getWidth());assertEquals(20,image.getHeight());}
 @Test void zipSafeNames() throws Exception {var bytes=mvc.perform(multipart("/api/zip").file(new MockMultipartFile("files","../../file.txt","text/plain","hello".getBytes()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){assertEquals("1-file.txt",zip.getNextEntry().getName());assertEquals("hello",new String(zip.readAllBytes()));}}
}
