package grails.plugin.logback;

import grails.util.Environment;
import grails.util.GrailsWebUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * Configures Logback in WAR deployment using the DSL. Based on
 * org.codehaus.groovy.grails.plugins.log4j.web.util.Log4jConfigListener.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogbackConfigListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		try {
			GrailsApplication grailsApplication = GrailsWebUtil.lookupApplication(event.getServletContext());
			if (grailsApplication == null) {
				return;
			}

			if (grailsApplication.getConfig() != null) {
				return;
			}

			// in this case we're running inside a WAR deployed environment
			LogbackConfig.initialize(new DefaultGrailsApplication().getConfig());
		}
		catch (Throwable e) {
			LogLog.error("Error initializing Logback: " + e.getMessage(), e);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		if (Environment.getCurrent() != Environment.DEVELOPMENT) {
			((LoggerContext)LoggerFactory.getILoggerFactory()).stop();
		}
	}
}
