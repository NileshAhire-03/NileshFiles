package com.nilspath.api;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** In-memory PDF/DOCX parsing and transparent, deterministic job skill matching. */
@RestController
@RequestMapping({"/api/candidate/resume", "/api/hr/resume"})
public class ResumeController {
  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final int MAX_TEXT = 150_000;
  private final Jobs jobs;
  private final ResumeMatcher matcher;
  private final AccountDirectory directory;
  ResumeController(Jobs jobs, ResumeMatcher matcher, AccountDirectory directory) { this.jobs = jobs; this.matcher = matcher; this.directory = directory; }
  private List<Job> visibleJobs(Authentication auth) {
    Account account = directory.current(auth);
    if (account.role.equals("HR")) return jobs.findAll().stream().filter(j -> account.id.equals(j.hrId)).toList();
    return jobs.findByActiveTrue();
  }

  @PostMapping(value="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Authentication auth) {
    if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Choose a non-empty PDF or DOCX file."));
    if (file.getSize() > MAX_BYTES) return ResponseEntity.status(413).body(Map.of("error", "Maximum resume size is 5 MB."));
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
    boolean pdf = name.endsWith(".pdf"), docx = name.endsWith(".docx");
    if (!pdf && !docx) return ResponseEntity.badRequest().body(Map.of("error", "Only PDF and DOCX files are supported."));
    try {
      byte[] bytes = file.getBytes();
      if (bytes.length > MAX_BYTES) return ResponseEntity.status(413).body(Map.of("error", "Maximum resume size is 5 MB."));
      String content;
      if (pdf) {
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') throw new IOException("Not a PDF document");
        try (var document = Loader.loadPDF(bytes)) {
          if (document.isEncrypted()) return ResponseEntity.badRequest().body(Map.of("error", "Password-protected PDFs are not supported."));
          if (document.getNumberOfPages() > 30) return ResponseEntity.badRequest().body(Map.of("error", "Resume must contain at most 30 pages."));
          content = new PDFTextStripper().getText(document);
        }
      } else {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') throw new IOException("Not a DOCX document");
        try (var document = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(document)) {
          content = extractor.getText();
        }
      }
      if (content.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "No selectable text found. Scanned-image resumes need OCR, which is not enabled."));
      if (content.length() > MAX_TEXT) return ResponseEntity.badRequest().body(Map.of("error", "Resume contains too much text."));
      return ResponseEntity.ok(Map.of("fileName", name, "characters", content.length(), "matches", matcher.score(content, visibleJobs(auth))));
    } catch (Exception ex) {
      return ResponseEntity.badRequest().body(Map.of("error", "Could not read resume. Upload a valid, unencrypted PDF or DOCX."));
    }
  }

  @PostMapping("/match-text")
  public ResponseEntity<?> matchText(@jakarta.validation.Valid @RequestBody ApiController.ResumeInput request, Authentication auth) {
    return ResponseEntity.ok(Map.of("matches", matcher.score(request.resume(), visibleJobs(auth))));
  }
}
