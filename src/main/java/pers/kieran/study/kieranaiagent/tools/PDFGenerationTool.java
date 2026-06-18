package pers.kieran.study.kieranaiagent.tools;

import cn.hutool.core.io.FileUtil;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.ListNumberingType;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import pers.kieran.study.kieranaiagent.constant.FileConstant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    private static final int MAX_PDF_IMAGES = 8;
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024L;
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    @Tool(description = "Generate a PDF file with plain text content and optional real-scene image URLs. If the user asks for images in the PDF, pass image URLs from searchImages to imageUrls.")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Plain text content to be included in the PDF") String content,
            @ToolParam(required = false, description = "Optional image URLs to embed in the PDF. Use newline-separated URLs, comma-separated URLs, or a JSON array string. Include source URLs for attribution when possible.") String imageUrls) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String safeFileName = sanitizeFileName(fileName);
        String safeContent = sanitizePdfContent(content);
        String filePath = fileDir + "/" + safeFileName;
        try {
            FileUtil.mkdir(fileDir);
            ImageEmbedResult imageEmbedResult;
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = createChineseFont();
                document.setFont(font);
                addContent(document, safeContent);
                imageEmbedResult = addImages(document, imageUrls, safeFileName);
            }
            return String.format(
                    "{\"type\":\"pdf\",\"fileName\":\"%s\",\"filePath\":\"%s\",\"url\":\"/api/ai/files/pdf/%s\",\"imageCount\":%d,\"skippedImageCount\":%d}",
                    escapeJson(safeFileName),
                    escapeJson(filePath),
                    escapeJson(safeFileName),
                    imageEmbedResult.embeddedCount(),
                    imageEmbedResult.skippedCount()
            );
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }


    private ImageEmbedResult addImages(Document document, String imageUrls, String safeFileName) {
        List<String> urls = extractImageUrls(imageUrls);
        if (urls.isEmpty()) {
            return new ImageEmbedResult(0, 0);
        }
        document.add(new Paragraph("\n实景图片素材")

                .setFontSize(15)
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("以下图片来自检索到的素材链接；正式发布前请再次核验授权范围与署名要求。")
                .setFontSize(9));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        int embeddedCount = 0;
        int skippedCount = 0;
        int index = 1;
        Path imageDir = Path.of(FileConstant.FILE_SAVE_DIR, "pdf", "images", safeFileName.replaceAll("[^a-zA-Z0-9._-]", "_"));
        try {
            Files.createDirectories(imageDir);
        } catch (Exception e) {
            return new ImageEmbedResult(0, urls.size());
        }

        for (String url : urls) {
            if (embeddedCount >= MAX_PDF_IMAGES) {
                skippedCount++;
                continue;
            }
            try {
                DownloadedImage downloadedImage = downloadImage(httpClient, url, imageDir, index);
                ImageData imageData = ImageDataFactory.create(downloadedImage.path().toString());
                Image image = new Image(imageData);
                image.setAutoScale(true);
                image.setMaxWidth(460);
                image.setMaxHeight(260);
                document.add(image);
                document.add(new Paragraph("图" + index + "  素材来源：" + url)
                        .setFontSize(8));
                embeddedCount++;
                index++;
            } catch (Exception e) {
                skippedCount++;
                document.add(new Paragraph("图片嵌入失败：" + url + "（" + sanitizePdfContent(e.getMessage()) + "）")
                        .setFontSize(8));
            }
        }
        return new ImageEmbedResult(embeddedCount, skippedCount);
    }

    private List<String> extractImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return List.of();
        }
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = IMAGE_URL_PATTERN.matcher(imageUrls);
        while (matcher.find() && urls.size() < MAX_PDF_IMAGES * 2) {
            String url = trimUrlTail(matcher.group());
            if (url.startsWith("http://") || url.startsWith("https://")) {
                urls.add(url);
            }
        }
        return new ArrayList<>(urls);
    }

    private String trimUrlTail(String url) {
        while (!url.isEmpty()) {
            char last = url.charAt(url.length() - 1);
            if (last == ',' || last == '.' || last == ';' || last == ')' || last == ']'
                    || last == '}' || last == '\"' || last == '\'') {
                url = url.substring(0, url.length() - 1);
            } else {
                break;
            }
        }
        return url;
    }

    private DownloadedImage downloadImage(HttpClient httpClient, String url, Path imageDir, int index) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 KieranAiAgent/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase();
        if (!contentType.startsWith("image/") && !looksLikeImageUrl(url)) {
            throw new IllegalStateException("not an image response");
        }
        byte[] body = response.body();
        if (body.length == 0) {
            throw new IllegalStateException("empty image");
        }
        if (body.length > MAX_IMAGE_BYTES) {
            throw new IllegalStateException("image too large");
        }
        String extension = imageExtension(contentType, url);
        Path imagePath = imageDir.resolve("image_" + index + extension);
        Files.write(imagePath, body);
        return new DownloadedImage(imagePath);
    }

    private boolean looksLikeImageUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".png")
                || lowerUrl.contains(".webp") || lowerUrl.contains("images.unsplash.com");
    }

    private String imageExtension(String contentType, String url) {
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        if (contentType.contains("gif")) {
            return ".gif";
        }
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".png")) {
            return ".png";
        }
        if (lowerUrl.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private record ImageEmbedResult(int embeddedCount, int skippedCount) {
    }

    private record DownloadedImage(Path path) {
    }

    private PdfFont createChineseFont() throws java.io.IOException {
        String[] fontPaths = {
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf"
        };
        for (String fontPath : fontPaths) {
            String actualPath = fontPath.contains(",") ? fontPath.substring(0, fontPath.indexOf(',')) : fontPath;
            if (Files.exists(Path.of(actualPath))) {
                return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
            }
        }
        return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
    }

    private void addContent(Document document, String content) {
        String[] lines = content.split("\\R");
        com.itextpdf.layout.element.List orderedList = null;
        Pattern orderedPattern = Pattern.compile("^\\s*(\\d+)[\\.、)]\\s+(.+)$");
        for (String line : lines) {
            String trimmedLine = line.trim();
            Matcher matcher = orderedPattern.matcher(trimmedLine);
            if (matcher.matches()) {
                if (orderedList == null) {
                    orderedList = new com.itextpdf.layout.element.List(ListNumberingType.DECIMAL);
                    orderedList.setMarginLeft(18);
                }
                orderedList.add(matcher.group(2));
                continue;
            }

            if (orderedList != null) {
                document.add(orderedList);
                orderedList = null;
            }
            document.add(new Paragraph(trimmedLine.isEmpty() ? " " : trimmedLine));
        }

        if (orderedList != null) {
            document.add(orderedList);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "generated.pdf";
        }
        String safeFileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safeFileName.toLowerCase().endsWith(".pdf") ? safeFileName : safeFileName + ".pdf";
    }

    private String sanitizePdfContent(String content) {
        if (content == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(content.length());
        for (int offset = 0; offset < content.length(); ) {
            int codePoint = content.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (isSupportedPdfCodePoint(codePoint)) {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append(' ');
            }
        }
        return builder.toString()
                .replace('?', '-')
                .replace('?', '-')
                .replace('?', '-')
                .replace('?', '-')
                .replace('?', '"')
                .replace('?', '"')
                .replace('?', '\'')
                .replace('?', '\'');
    }

    private boolean isSupportedPdfCodePoint(int codePoint) {
        if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
            return true;
        }
        if (codePoint >= 0x20 && codePoint <= 0xFFFF) {
            return !Character.isISOControl(codePoint)
                    && Character.getType(codePoint) != Character.SURROGATE
                    && codePoint != 0xFE0F
                    && codePoint != 0x200D;
        }
        return false;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
