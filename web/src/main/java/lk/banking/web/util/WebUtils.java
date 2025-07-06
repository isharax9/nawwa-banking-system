package lk.banking.web.util;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import lk.banking.core.entity.User;

public class WebUtils {
    public static HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest();
    }

    public static User getLoggedInUser() {
        HttpServletRequest req = getRequest();
        return (User) req.getSession().getAttribute("user");
    }
}