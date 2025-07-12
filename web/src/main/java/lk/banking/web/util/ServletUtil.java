package lk.banking.web.util;

import jakarta.ejb.EJBException;
import lk.banking.core.exception.BankingException; // Import BankingException

import java.util.logging.Logger;

/**
 * Utility class for common Servlet-related helper methods,
 * including unwrapping exceptions from EJB calls.
 */
public class ServletUtil {

    private static final Logger LOGGER = Logger.getLogger(ServletUtil.class.getName());

    /**
     * Unwraps the root cause of an EJBException, if it's a BankingException.
     * Otherwise, returns the original EJBException or a generic Exception.
     * @param e The exception caught from an EJB invocation.
     * @return The most specific BankingException, or a generic Exception.
     */
    public static Exception unwrapEJBException(Exception e) {
        if (e instanceof EJBException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                // If the cause is a BankingException or any subclass, return it.
                // This relies on the classloader recognizing the common base class.
                if (cause instanceof BankingException) {
                    LOGGER.fine("ServletUtil: Unwrapped EJBException to BankingException: " + cause.getClass().getName());
                    return (BankingException) cause;
                } else {
                    LOGGER.fine("ServletUtil: EJBException cause is not a BankingException: " + cause.getClass().getName());
                    return new Exception("EJB operation failed: " + cause.getMessage(), cause); // Wrap in generic Exception
                }
            } else {
                LOGGER.fine("ServletUtil: EJBException has no cause.");
                return new Exception("EJB operation failed: " + e.getMessage(), e); // EJBException without a cause
            }
        }
        LOGGER.fine("ServletUtil: Not an EJBException. Returning original exception.");
        return e; // Not an EJBException, return as is
    }
}