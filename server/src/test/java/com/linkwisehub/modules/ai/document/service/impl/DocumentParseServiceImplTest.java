package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DocumentParseServiceImplTest {

    private final DocumentParseServiceImpl service = new DocumentParseServiceImpl();

    @Test
    void parseTextAndMarkdown() {
        assertContains(parse(bytes("txt content"), DocumentFileType.TXT), "txt content");
        assertContains(parse(bytes("# md content"), DocumentFileType.MD), "md content");
    }

    @Test
    void parseWordDocument() throws Exception {
        assertContains(parse(buildDocx(), DocumentFileType.DOCX), "docx content");
    }

    @Test
    void parseLegacyWordDocumentWhenTextutilAvailable() throws Exception {
        assumeTrue(commandExists("textutil"), "当前环境没有 textutil，跳过老版 doc 生成测试");
        assertContains(parse(buildLegacyDoc(), DocumentFileType.DOC), "legacy doc content");
    }

    @Test
    void parsePowerPointDocument() throws Exception {
        assertContains(parse(buildPptx(), DocumentFileType.PPTX), "pptx content");
        assertContains(parse(buildPpt(), DocumentFileType.PPT), "legacy ppt content");
    }

    @Test
    void parseExcelDocument() throws Exception {
        assertContains(parse(buildWorkbook(new XSSFWorkbook(), "xlsx content"), DocumentFileType.XLSX), "xlsx content");
        assertContains(parse(buildWorkbook(new HSSFWorkbook(), "xls content"), DocumentFileType.XLS), "xls content");
    }

    @Test
    void parsePdfDocument() throws Exception {
        assertContains(parse(buildPdf(), DocumentFileType.PDF), "pdf content");
    }

    @Test
    void rejectUnsupportedEmptyAndBrokenFile() {
        assertThrows(BusinessException.class, () -> parse(bytes("content"), "exe"));
        assertThrows(BusinessException.class, () -> parse(bytes("   "), DocumentFileType.TXT));
        assertThrows(BusinessException.class, () -> parse(bytes("bad pdf"), DocumentFileType.PDF));
    }

    private String parse(byte[] content, String fileType) {
        return service.parse(new ByteArrayInputStream(content), fileType);
    }

    private byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private void assertContains(String text, String expected) {
        assertTrue(text.contains(expected), () -> "解析结果未包含预期文本: " + expected + "\n实际内容: " + text);
    }

    private byte[] buildDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("docx content");
            document.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildLegacyDoc() throws Exception {
        Path tempDir = Files.createTempDirectory("legacy-doc-test");
        Path txtPath = tempDir.resolve("sample.txt");
        Path docPath = tempDir.resolve("sample.doc");
        Files.writeString(txtPath, "legacy doc content", StandardCharsets.UTF_8);
        Process process = new ProcessBuilder("textutil", "-convert", "doc", txtPath.toString(), "-output", docPath.toString()).start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        assumeTrue(finished && process.exitValue() == 0 && Files.exists(docPath), "textutil 生成 doc 文件失败，跳过老版 doc 测试");
        return Files.readAllBytes(docPath);
    }

    private boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder("sh", "-c", "command -v " + command).start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] buildPptx() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setText("pptx content");
            slideShow.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildPpt() throws Exception {
        try (HSLFSlideShow slideShow = new HSLFSlideShow();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            HSLFSlide slide = slideShow.createSlide();
            HSLFTextBox textBox = new HSLFTextBox();
            textBox.setAnchor(new Rectangle(50, 50, 400, 100));
            textBox.setText("legacy ppt content");
            slide.addShape(textBox);
            slideShow.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildWorkbook(Workbook workbook, String content) throws Exception {
        try (Workbook closeableWorkbook = workbook;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            closeableWorkbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue(content);
            closeableWorkbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("pdf content");
                contentStream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
