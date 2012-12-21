package org.slf4j.impl;

import grails.plugin.logback.LogbackLoggerFactory;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * Provides stack trace cleaning and filtering of Logback log output.
 * Based on org.slf4j.impl.StaticLoggerBinder from Grails.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	/**
	 * Declare the version of the SLF4J API this implementation is compiled
	 * against. The value of this field is usually modified with each release.
	 */
	// to avoid constant folding by the compiler, this field must *not* be final
	public static String REQUESTED_API_VERSION = "1.6"; // !final

	/**
	 * Return the singleton of this class.
	 *
	 * @return the StaticLoggerBinder singleton
	 */
	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	/**
	 * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
	 * method should always be the same object
	 */
	protected ILoggerFactory loggerFactory = new LogbackLoggerFactory();

	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	public String getLoggerFactoryClassStr() {
		return LogbackLoggerFactory.class.getName();
	}
}
