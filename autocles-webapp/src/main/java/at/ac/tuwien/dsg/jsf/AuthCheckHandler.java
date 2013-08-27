package at.ac.tuwien.dsg.jsf;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.bean.ManagedProperty;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * This class enforces basic application security. Each time
 * a JSF page is requested, the method renderView(..) of this
 * class is called. If it turns out that there is currently 
 * no user logged in, a forward to the login page is triggered.
 */
public class AuthCheckHandler extends com.sun.facelets.FaceletViewHandler implements Serializable {
	private static final long serialVersionUID = 1L;

	@ManagedProperty("#{uiBean}")
	private UIBean bean;
	/** Property used for redirection, e.g., after a successful login. */
	public static String redirect = null;

	public AuthCheckHandler (ViewHandler base) {
		super(base);
	}

	/**
	 * This method is called when a JSF view is to be rendered.
	 * If no user is currently logged in, there will be a 
	 * redirection to the login page...
	 */
	@Override
	public void renderView(FacesContext fc, UIViewRoot root) throws IOException, FacesException {
		String path = fc.getExternalContext().getRequestServletPath();
		if(path.contains("/")) {
			path = path.substring(path.indexOf("/") + 1);
		}
		List<String> unsecuredPages = Arrays.asList(
				"index.jsf");
		List<String> adminPages = Arrays.asList();

		if(path.indexOf("login.jsf") < 0 && 
				path.indexOf("loginAdmin.jsf") < 0 && 
				!unsecuredPages.contains(path)) {
			if(bean == null) {
				bean = (UIBean) fc.getApplication()
					.evaluateExpressionGet(fc, "#{uiBean}", UIBean.class);
			}
			if(adminPages.contains(path)) {
				if(!bean.isAdminLoggedIn()) {
					try {
						redirect = path;
						fc.getExternalContext().redirect("loginAdmin.jsf");
						return;
					} catch (Exception e) {
						System.err.println("renderView: " + e);
					}
				}
			} else {
				if(!bean.isLoggedIn()) {
					try {
						redirect = path;
						fc.getExternalContext().redirect("login.jsf");
						return;
					} catch (Exception e) {
						System.err.println("renderView: " + e);
					}
				}
			}
		}
		try {
			super.renderView(fc, root);
		} catch (Exception e) {
			e.printStackTrace();
			UIBean.addMessage("Error rendering page.", e);
		}
	}

	public static boolean checkLogin(String user, String pass) {
		return true; // TODO implement!
	}
}
