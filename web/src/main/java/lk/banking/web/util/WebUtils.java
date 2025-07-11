package lk.banking.web.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lk.banking.core.dto.LoggedInUser; // Import the new LoggedInUser DTO

/**
 * Utility class for web-related operations, especially within a JSF context.
 */
public class WebUtils {

    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser"; // Consistent session key

    /**
     * Retrieves the current HttpServletRequest instance.
     * @return The current HttpServletRequest.
     */
    public static HttpServletRequest getRequest() {
        // Ensure FacesContext is available (only during JSF request lifecycle)
        if (FacesContext.getCurrentInstance() == null) {
            // Or throw a more specific exception if this is a hard requirement
            throw new IllegalStateException("FacesContext is not available. This method can only be called during a JSF request.");
        }
        return (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest();
    }

    /**
     * Retrieves the LoggedInUser object from the current HTTP session.
     * @return The LoggedInUser object, or null if no user is logged in or found in session.
     */
    public static LoggedInUser getLoggedInUser() {
        HttpServletRequest req = getRequest();
        HttpSession session = req.getSession(false); // Do not create a new session if one doesn't exist

        if (session != null) {
            return (LoggedInUser) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        }
        return null;
    }

    /**
     * Stores the LoggedInUser object into the HTTP session.
     * This method would be called during the login process.
     * @param user The LoggedInUser object to store.
     */
    public static void setLoggedInUser(LoggedInUser user) {
        HttpServletRequest req = getRequest();
        HttpSession session = req.getSession(true); // Create a session if one doesn't exist
        session.setAttribute(LOGGED_IN_USER_SESSION_KEY, user);
    }

    /**
     * Invalidates the current HTTP session, effectively logging out the user.
     */
    public static void logoutUser() {
        HttpServletRequest req = getRequest();
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
    public static void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
    }

    public static void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public static void addWarnMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, message, null));
    }

    public static void addFatalMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
    }

}