// In: lk.banking.web.util.PdfGenerationUtil.java
package lk.banking.web.util;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lk.banking.core.dto.TransactionDto;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class PdfGenerationUtil {

    /**
     * Renders a JSP file into an HTML string.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param jspPath The path to the JSP file (e.g., "/WEB-INF/jspf/_transactionTable.jspf").
     * @param transactions The data to be passed to the JSP.
     * @return The rendered HTML as a String.
     * @throws Exception if rendering fails.
     */
    private static String renderJspToString(HttpServletRequest request, HttpServletResponse response, String jspPath, List<TransactionDto> transactions) throws Exception {
        // Set the transaction data as a request attribute so the JSP can access it
        request.setAttribute("transactionsForPdf", transactions);

        // Create a StringWriter to capture the JSP's output
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // Create a custom response wrapper to capture the output
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
            @Override
            public PrintWriter getWriter() {
                return printWriter;
            }
        };

        // Get the RequestDispatcher and include the JSP, which will "print" its HTML to our StringWriter
        RequestDispatcher dispatcher = request.getRequestDispatcher(jspPath);
        dispatcher.include(request, responseWrapper);

        // Flush and return the captured HTML
        printWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Generates a PDF from a list of transactions by first rendering a JSP to HTML,
     * then converting the HTML to a PDF using Flying Saucer.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param transactions The list of TransactionDto objects.
     * @return A byte array containing the generated PDF.
     * @throws Exception If there is an error during PDF creation.
     */
    public static byte[] generatePdfFromJsp(HttpServletRequest request, HttpServletResponse response, List<TransactionDto> transactions) throws Exception {
        // 1. Define the path to our reusable JSP table fragment
        String jspPath = "/WEB-INF/jspf/_transactionTable.jspf";

        // 2. Render the JSP to an HTML string
        String htmlContent = renderJspToString(request, response, jspPath, transactions);

        // IMPORTANT: Remove the old attribute to avoid it leaking to other requests
        request.removeAttribute("transactionsForPdf");

        // 3. Use Flying Saucer to convert the HTML string to a PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        // The base URL is needed to resolve any relative paths (e.g., for images or CSS)
        // Here, we can create a simple one.
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        renderer.setDocumentFromString(htmlContent, baseUrl);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }
}