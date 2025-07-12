package lk.banking.web.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.logging.Logger;

/**
 * Utility for managing "flash" messages (messages that appear once after a redirect).
 * Messages are stored in the session and moved to the request scope for display,
 * then immediately cleared from the session.
 */
public class FlashMessageUtil {

    private static final Logger LOGGER = Logger.getLogger(FlashMessageUtil.class.getName());
    private static final String FLASH_MESSAGE_KEY = "flashMessage";
    private static final String FLASH_MESSAGE_TYPE_KEY = "flashMessageType"; // Optional: "success", "error", "info"

    /**
     * Stores a success message in the session to be displayed on the next request.
     * @param session The current HTTP session.
     * @param message The success message.
     */
    public static void putSuccessMessage(HttpSession session, String message) {
        if (session != null) {
            session.setAttribute(FLASH_MESSAGE_KEY, message);
            session.setAttribute(FLASH_MESSAGE_TYPE_KEY, "success");
            LOGGER.fine("FlashMessageUtil: Stored success message in session: " + message);
        }
    }

    /**
     * Stores an error message in the session to be displayed on the next request.
     * @param session The current HTTP session.
     * @param message The error message.
     */
    public static void putErrorMessage(HttpSession session, String message) {
        if (session != null) {
            session.setAttribute(FLASH_MESSAGE_KEY, message);
            session.setAttribute(FLASH_MESSAGE_TYPE_KEY, "error");
            LOGGER.fine("FlashMessageUtil: Stored error message in session: " + message);
        }
    }

    /**
     * Retrieves a flash message from the session and moves it to the request scope.
     * The message is immediately removed from the session after retrieval.
     * @param request The current HttpServletRequest.
     */
    public static void retrieveAndClearMessages(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // Don't create if none exists
        if (session != null) {
            String message = (String) session.getAttribute(FLASH_MESSAGE_KEY);
            String type = (String) session.getAttribute(FLASH_MESSAGE_TYPE_KEY);

            if (message != null) {
                request.setAttribute("flashMessage", message); // Make available in request scope
                request.setAttribute("flashMessageType", type != null ? type : "info"); // Default to info type
                session.removeAttribute(FLASH_MESSAGE_KEY); // IMPORTANT: Clear from session
                session.removeAttribute(FLASH_MESSAGE_TYPE_KEY); // Clear type
                LOGGER.fine("FlashMessageUtil: Retrieved and cleared flash message: " + message + " (Type: " + type + ")");
            }
        }
    }
}