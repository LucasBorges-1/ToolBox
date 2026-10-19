package com.toolbox;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.io.IOUtils;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.zip.*;

@RestController
@RequestMapping("/api")
public class ToolsController {
  private ResponseEntity<byte[]> download(byte[] data,String type,String name) { return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header("Content-Disposition","attachment; filename=\""+name+"\"").body(data); }
  @GetMapping("/health") public Map<String,String> health(){return Map.of("status","ok");}
  @PostMapping("/pdf/merge") public ResponseEntity<byte[]> merge(@RequestParam("files") MultipartFile[] files) throws Exception {
    if(files.length<2 || files.length>20) throw new IllegalArgumentException("Selecione entre 2 e 20 PDFs.");
    try(var out=new ByteArrayOutputStream()) { var merger=new PDFMergerUtility(); for(var file:files)merger.addSource(new org.apache.pdfbox.io.RandomAccessReadBuffer(file.getBytes())); merger.setDestinationStream(out);merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());return download(out.toByteArray(),"application/pdf","toolbox-unido.pdf"); }
  }
  @PostMapping("/pdf/split") public ResponseEntity<byte[]> split(@RequestParam MultipartFile file,@RequestParam(defaultValue="1") int page) throws Exception {
    try(var source=Loader.loadPDF(file.getBytes());var doc=new PDDocument();var out=new ByteArrayOutputStream()) {if(page<1||page>source.getNumberOfPages())throw new IllegalArgumentException("Página fora do intervalo do PDF.");doc.importPage(source.getPage(page-1));doc.save(out);return download(out.toByteArray(),"application/pdf","pagina-"+page+".pdf");}
  }
  @PostMapping("/image/convert") public ResponseEntity<byte[]> image(@RequestParam MultipartFile file,@RequestParam(defaultValue="png") String format,@RequestParam(defaultValue="0") int width) throws Exception {
    if(!Set.of("png","jpg").contains(format))throw new IllegalArgumentException("Escolha PNG ou JPG.");
    var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()));
    try(input) {var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("Imagem não reconhecida. Use PNG ou JPG.");var reader=readers.next();try {reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);if((long)w*h>25000000)throw new IllegalArgumentException("Limite de 25 megapixels.");if(width<0||width>6000)throw new IllegalArgumentException("Largura deve estar entre 1 e 6000.");var src=reader.read(0);int ow=width==0?w:width,oh=Math.max(1,(int)((long)h*ow/w));if((long)ow*oh>25000000)throw new IllegalArgumentException("Imagem resultante muito grande.");var dest=new BufferedImage(ow,oh,format.equals("jpg")?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB);var g=dest.createGraphics();if(format.equals("jpg")){g.setColor(Color.WHITE);g.fillRect(0,0,ow,oh);}g.drawImage(src,0,0,ow,oh,null);g.dispose();var out=new ByteArrayOutputStream();ImageIO.write(dest,format,out);return download(out.toByteArray(),format.equals("jpg")?"image/jpeg":"image/png","toolbox-imagem."+format);}finally{reader.dispose();}}
  }
  @PostMapping("/qr") public ResponseEntity<byte[]> qr(@RequestParam String text) throws Exception {if(text.isBlank()||text.length()>1500)throw new IllegalArgumentException("Informe até 1.500 caracteres.");var matrix=new MultiFormatWriter().encode(text,BarcodeFormat.QR_CODE,512,512,Map.of(EncodeHintType.CHARACTER_SET,"UTF-8"));var out=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",out);return download(out.toByteArray(),"image/png","toolbox-qrcode.png");}
  @PostMapping("/zip") public ResponseEntity<byte[]> zip(@RequestParam("files") MultipartFile[] files) throws Exception {if(files.length==0||files.length>50)throw new IllegalArgumentException("Selecione de 1 a 50 arquivos.");var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){int n=0;for(var f:files){String name=Optional.ofNullable(f.getOriginalFilename()).orElse("arquivo").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[^\\p{L}\\p{N}._ -]","_");zip.putNextEntry(new ZipEntry((++n)+"-"+name));zip.write(f.getBytes());zip.closeEntry();}}return download(out.toByteArray(),"application/zip","toolbox-arquivos.zip");}
  @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,String>> error(Exception ex) {String message=ex instanceof IllegalArgumentException?ex.getMessage():"Não foi possível processar. Verifique o formato e os limites do arquivo.";return ResponseEntity.badRequest().body(Map.of("message",message));}
}
