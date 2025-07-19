package lk.banking.web.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

@WebFilter(urlPatterns = {"/pdf/*"}) // This filter will apply to any URL starting with /pdf/
public class PdfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Create a wrapper to capture the HTML output from the JSP
        HtmlResponseWrapper responseWrapper = new HtmlResponseWrapper(httpResponse);

        // 2. Proceed with the servlet/JSP chain. The JSP will write its HTML to our wrapper.
        chain.doFilter(httpRequest, responseWrapper);

        // 3. Get the captured HTML content
        String htmlContent = responseWrapper.getCaptureAsString();

        try {
            // 4. Use Flying Saucer to convert the captured HTML to a PDF
            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort() + httpRequest.getContextPath();
            renderer.setDocumentFromString(htmlContent, baseUrl);
            renderer.layout();
            renderer.createPDF(pdfOutputStream);

            // 5. Set the real response headers for a PDF download
            httpResponse.setContentType("application/pdf");
            httpResponse.setContentLength(pdfOutputStream.size());
            httpResponse.setHeader("Content-Disposition", "attachment; filename=\"Transaction_Statement.pdf\"");

            // 6. Write the PDF bytes to the real response
            OutputStream realOutputStream = httpResponse.getOutputStream();
            realOutputStream.write(pdfOutputStream.toByteArray());
            realOutputStream.flush();

        } catch (Exception e) {
            throw new ServletException("Error while converting HTML to PDF", e);
        }
    }

    // Helper classes for capturing the response
    private static class HtmlResponseWrapper extends HttpServletResponseWrapper {
        private final StringWriter capture;
        private final PrintWriter writer;

        public HtmlResponseWrapper(HttpServletResponse response) {
            super(response);
            capture = new StringWriter();
            writer = new PrintWriter(capture);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        public String getCaptureAsString() {
            return capture.toString();
        }
    }
}