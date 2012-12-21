package grails.plugin.logback

import org.slf4j.Logger

import ch.qos.logback.classic.Level
import ch.qos.logback.core.Appender

/**
 * Based on org.codehaus.groovy.grails.plugins.log4j.RootLog4jConfig.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class RootLogbackConfig {

	protected Logger root
	protected LogbackConfig config

	RootLogbackConfig(Logger root, LogbackConfig config) {
		this.root = root
		this.config = config
	}

	def debug(Object[] appenders = null) {
		setLevelAndAppender Level.DEBUG, appenders
	}

	def info(Object[] appenders = null) {
		setLevelAndAppender Level.INFO, appenders
	}

	def warn(Object[] appenders = null) {
		setLevelAndAppender Level.WARN, appenders
	}

	def trace(Object[] appenders = null) {
		setLevelAndAppender Level.TRACE, appenders
	}

	def all(Object[] appenders = null) {
		setLevelAndAppender Level.ALL, appenders
	}

	def error(Object[] appenders = null) {
		setLevelAndAppender Level.ERROR, appenders
	}

	def fatal(Object[] appenders = null) {
		setLevelAndAppender Level.ERROR, appenders
	}

	def off(Object[] appenders = null) {
		setLevelAndAppender Level.OFF, appenders
	}

	void setProperty(String s, o) {
		root."$s" = o
	}

	protected setLevelAndAppender(Level level, Object[] appenders) {
		root.level = level
		for (appenderName in appenders) {
			Appender appender
			if (appenderName instanceof Appender) {
				appender = appenderName
			}
			else {
				appender = config.appenders[appenderName?.toString()]
			}
			if (appender) {
				root.addAppender appender
			}
		}
	}
}
