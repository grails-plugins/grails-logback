package grails.plugin.logback

import grails.util.Environment

/**
 * Based on org.codehaus.groovy.grails.plugins.log4j.EnvironmentsLog4jConfig.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class EnvironmentsLogbackConfig {

	protected LogbackConfig config

	EnvironmentsLogbackConfig(LogbackConfig config) {
		this.config = config
	}

	def development(Closure callable) {
		if (Environment.current == Environment.DEVELOPMENT) {
			config.invokeCallable callable
		}
	}

	def production(Closure callable) {
		if (Environment.current == Environment.PRODUCTION) {
			config.invokeCallable callable
		}
	}

	def test(Closure callable) {
		if (Environment.current == Environment.TEST) {
			config.invokeCallable callable
		}
	}

	def methodMissing(String name, args) {
		if (args && args[0] instanceof Closure) {
			// treat all method calls that take a closure as custom environment names
			if (Environment.current == Environment.CUSTOM && Environment.current.name == name) {
				config.invokeCallable args[0]
			}
		}
		else {
			LogLog.error "Method missing when configuring Logback: $name"
		}
	}
}
