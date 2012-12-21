package grails.plugin.logback

import org.slf4j.LoggerFactory

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class Utils {

	static void clearLoggers() {
		LogbackLoggerFactory loggerFactory = LoggerFactory.ILoggerFactory
		loggerFactory.loggerContext.loggerCache.clear()
	}
}
