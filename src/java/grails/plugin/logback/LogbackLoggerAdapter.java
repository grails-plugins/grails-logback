package grails.plugin.logback;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;

import java.util.Iterator;

import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachable;

/**
 * A Logback adapter that produces cleaner, more informative stack traces. Based on org.slf4j.impl.GrailsLog4jLoggerAdapter.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogbackLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger, AppenderAttachable<ILoggingEvent> {

	private static final long serialVersionUID = 1;

	protected static final String FQCN = LogbackLoggerAdapter.class.getName();

	protected final Logger logbackLogger;
	protected StackTraceFilterer stackTraceFilterer = new LogbackStackTraceFilterer();

	public LogbackLoggerAdapter(Logger logger) {
		logbackLogger = logger;
		name = logger.getName();
	}

	// interface methods

	public boolean isTraceEnabled() {
		return logbackLogger.isTraceEnabled();
	}

	public void trace(String msg) {
		logMessage(TRACE, msg, null);
	}

	public void trace(String format, Object arg) {
		logMessageFormat(TRACE, format, arg);
	}

	public void trace(String format, Object arg1, Object arg2) {
		logMessageFormat(TRACE, format, arg1, arg2);
	}

	public void trace(String format, Object[] argArray) {
		logMessageFormat(TRACE, format, argArray);
	}

	public void trace(String msg, Throwable t) {
		logMessage(TRACE, msg, t);
	}

	public boolean isDebugEnabled() {
		return logbackLogger.isDebugEnabled();
	}

	public void debug(String msg) {
		logMessage(DEBUG, msg, null);
	}

	public void debug(String format, Object arg) {
		logMessageFormat(DEBUG, format, arg);
	}

	public void debug(String format, Object arg1, Object arg2) {
		logMessageFormat(DEBUG, format, arg1, arg2);
	}

	public void debug(String format, Object[] argArray) {
		logMessageFormat(DEBUG, format, argArray);
	}

	public void debug(String msg, Throwable t) {
		logMessage(DEBUG, msg, t);
	}

	public boolean isInfoEnabled() {
		return logbackLogger.isInfoEnabled();
	}

	public void info(String msg) {
		logMessage(INFO, msg, null);
	}

	public void info(String format, Object arg) {
		logMessageFormat(INFO, format, arg);
	}

	public void info(String format, Object arg1, Object arg2) {
		logMessageFormat(INFO, format, arg1, arg2);
	}

	public void info(String format, Object[] argArray) {
		logMessageFormat(INFO, format, argArray);
	}

	public void info(String msg, Throwable t) {
		logMessage(INFO, msg, t);
	}

	public boolean isWarnEnabled() {
		return logbackLogger.isEnabledFor(WARN);
	}

	public void warn(String msg) {
		logMessage(WARN, msg, null);
	}

	public void warn(String format, Object arg) {
		logMessageFormat(WARN, format, arg);
	}

	public void warn(String format, Object[] argArray) {
		logMessageFormat(WARN, format, argArray);
	}

	public void warn(String format, Object arg1, Object arg2) {
		logMessageFormat(WARN, format, arg1, arg2);
	}

	public void warn(String msg, Throwable t) {
		logMessage(WARN, msg, t);
	}

	public boolean isErrorEnabled() {
		return logbackLogger.isEnabledFor(ERROR);
	}

	public void error(String msg) {
		logMessage(ERROR, msg, null);
	}

	public void error(String format, Object arg) {
		logMessageFormat(ERROR, format, arg);
	}

	public void error(String format, Object arg1, Object arg2) {
		logMessageFormat(ERROR, format, arg1, arg2);
	}

	public void error(String format, Object[] argArray) {
		logMessageFormat(ERROR, format, argArray);
	}

	public void error(String msg, Throwable t) {
		logMessage(ERROR, msg, t);
	}

	public void addAppender(Appender<ILoggingEvent> newAppender) {
		logbackLogger.addAppender(newAppender);
	}

	public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
		return logbackLogger.iteratorForAppenders();
	}

	public Appender<ILoggingEvent> getAppender(String appenderName) {
		return logbackLogger.getAppender(appenderName);
	}

	public boolean isAttached(Appender<ILoggingEvent> appender) {
		return logbackLogger.isAttached(appender);
	}

	public void detachAndStopAllAppenders() {
		logbackLogger.detachAndStopAllAppenders();
	}

	public boolean detachAppender(Appender<ILoggingEvent> appender) {
		return logbackLogger.detachAppender(appender);
	}

	public boolean detachAppender(String appenderName) {
		return logbackLogger.detachAppender(appenderName);
	}

	public void log(Marker marker, String fqcn, int levelInt, String message, Object[] argArray, Throwable t) {
		logbackLogger.log(marker, fqcn, levelInt, message, argArray, t);
	}

	// non-interface methods mirrored from real logger

	public boolean isEnabledFor(Level level) {
		return logbackLogger.isEnabledFor(level);
	}

	public Level getEffectiveLevel() {
		return logbackLogger.getEffectiveLevel();
	}

	public Level getLevel() {
		return logbackLogger.getLevel();
	}

	public void setLevel(Level level) {
		logbackLogger.setLevel(level);
	}

	public Iterator<Appender<ILoggingEvent>> getAllAppenders() {
		return iteratorForAppenders();
	}

	public boolean isAdditive() {
		return logbackLogger.isAdditive();
	}

	public void setAdditive(boolean additive) {
		logbackLogger.setAdditive(additive);
	}

	public Logger getNativeLogger() {
		return logbackLogger;
	}

	// private methods

	protected FormattingTuple getMessageFormat(final String format, final Object... args) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
		cleanIfException(ft.getThrowable());
		return ft;
	}

	protected Throwable cleanIfException(final Throwable t) {
		if (t != null) {
			stackTraceFilterer.filter(t, true);
		}
		return t;
	}

	protected void logMessageFormat(final Level level, final String format, final Object... args) {
		if (logbackLogger.isEnabledFor(level)) {
			FormattingTuple ft = getMessageFormat(format, args);
			logbackLogger.log(null, FQCN, toLocationAwareInt(level), ft.getMessage(), args, ft.getThrowable());
		}
	}

	protected void logMessage(final Level level, final String msg, final Throwable t) {
		Throwable filteredTrace = t;
		if (t != null && logbackLogger.isEnabledFor(level) && !DefaultStackTraceFilterer.STACK_LOG_NAME.equals(name)) {
			filteredTrace = cleanIfException(t);
		}
		logbackLogger.log(null, FQCN, toLocationAwareInt(level), msg, null, filteredTrace);
	}

	protected int toLocationAwareInt(Level level) {
		switch (level.levelInt) {
			case Level.TRACE_INT:
				return LocationAwareLogger.TRACE_INT;
			case Level.DEBUG_INT:
				return LocationAwareLogger.DEBUG_INT;
			case Level.INFO_INT:
				return LocationAwareLogger.INFO_INT;
			case Level.WARN_INT:
				return LocationAwareLogger.WARN_INT;
			case Level.ERROR_INT:
				return LocationAwareLogger.ERROR_INT;
			default:
				return LocationAwareLogger.ERROR_INT;
		}
	}
}
