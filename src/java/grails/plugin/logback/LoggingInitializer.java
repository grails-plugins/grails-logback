package grails.plugin.logback;

import groovy.util.ConfigObject;

/**
 * Default logging initializer used for Logback. Based on org.codehaus.groovy.grails.plugins.logging.LoggingInitializer.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LoggingInitializer {

	//TODO GrailsProjectPackager.LOGGING_INITIALIZER_CLASS

	public void initialize(ConfigObject config) {
		LogbackConfig.initialize(config);
	}
}
