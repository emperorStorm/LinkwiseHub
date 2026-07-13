package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.service.DocumentParseService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文档解析服务，将上传文件统一抽取为可分片文本。
 */
@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    @Override
    public String parse(InputStream inputStream, String fileType) {
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        try {
            String safeType = normalizeFileType(fileType);
            String text = parseByType(inputStream, safeType);
            return requireText(text);
        } catch (BusinessException e) {
            closeQuietly(inputStream);
            throw e;
        } catch (Exception e) {
            closeQuietly(inputStream);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件解析失败或内容为空: " + e.getMessage());
        }
    }

    private String normalizeFileType(String fileType) {
        String safeType = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        if (!DocumentFileType.isLegacySupported(safeType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, DocumentFileType.getSupportedMessage());
        }
        return safeType;
    }

    private String parseByType(InputStream inputStream, String fileType) throws Exception {
        return switch (fileType) {
            case DocumentFileType.TXT, DocumentFileType.MD -> parseText(inputStream);
            case DocumentFileType.DOC -> parseDoc(inputStream);
            case DocumentFileType.DOCX -> parseDocx(inputStream);
            case DocumentFileType.PPT -> parsePpt(inputStream);
            case DocumentFileType.PPTX -> parsePptx(inputStream);
            case DocumentFileType.XLS -> parseXls(inputStream);
            case DocumentFileType.XLSX -> parseXlsx(inputStream);
            case DocumentFileType.PDF -> parsePdf(inputStream);
            default -> throw new BusinessException(ErrorCode.PARAM_INVALID, DocumentFileType.getSupportedMessage());
        };
    }

    private String parseText(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            // 一次性读取适用于当前 10MB 上限，后续大文件再改为流式解析。
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String parseDoc(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             HWPFDocument document = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String parsePpt(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             HSLFSlideShow slideShow = new HSLFSlideShow(in)) {
            SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow);
            return extractor.getText();
        }
    }

    private String parsePptx(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             XMLSlideShow slideShow = new XMLSlideShow(in)) {
            XSLFExtractor extractor = new XSLFExtractor(slideShow);
            return extractor.getText();
        }
    }

    private String parseXls(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             HSSFWorkbook workbook = new HSSFWorkbook(in)) {
            return parseWorkbook(workbook);
        }
    }

    private String parseXlsx(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            return parseWorkbook(workbook);
        }
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream;
             PDDocument document = Loader.loadPDF(in.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseWorkbook(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        StringBuilder builder = new StringBuilder();
        for (Sheet sheet : workbook) {
            builder.append("Sheet: ").append(sheet.getSheetName()).append('\n');
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
                    Cell cell = row.getCell(index);
                    String value = cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
                    if (!value.isEmpty()) {
                        values.add(value);
                    }
                }
                if (!values.isEmpty()) {
                    builder.append(String.join("\t", values)).append('\n');
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String requireText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件解析失败或内容为空");
        }
        return text;
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
            // 解析失败时补偿关闭输入流，避免覆盖原始异常。
        }
    }
}
